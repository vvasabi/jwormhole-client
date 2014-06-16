package com.bradchen.jwormhole.client.console;

import au.com.bytecode.opencsv.CSVReader;
import com.bradchen.jwormhole.client.Client;
import com.bradchen.jwormhole.client.Settings;
import com.bradchen.jwormhole.client.SettingsUtils;
import com.bradchen.jwormhole.client.console.commands.Command;
import com.bradchen.jwormhole.client.console.commands.CommandFactory;
import com.bradchen.jwormhole.client.console.commands.HelpCommandFactory;
import com.bradchen.jwormhole.client.console.commands.QuitCommandFactory;
import com.bradchen.jwormhole.client.console.commands.StatusCommandFactory;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import static com.bradchen.jwormhole.client.SettingsUtils.readSettingsFromClassPathResource;
import static com.bradchen.jwormhole.client.SettingsUtils.readSettingsFromFileRelativeToHome;

/**
 * A simple Terminal-based UI.
 */
public final class ConsoleUI {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleUI.class);

	// in class path
	private static final String DEFAULT_SETTINGS_FILE = "client.default.properties";

	// relative to $HOME
	private static final String OVERRIDE_SETTINGS_FILE = ".jwormhole/client.properties";

	// plugins path, relative to $HOME
	private static final String PLUGINS_PATH = ".jwormhole/plugins";

	// command history path, relative to $HOME
	private static final String HISTORY_PATH = ".jwormhole/history";

	private static final String CONSOLE_PLUGINS_SETTING = "console.plugins";
	private static final String DEFAULT_SETTING_KEY = "default";
	private static final Pattern HOST_KEY_PATTERN = Pattern.compile("^[-_.a-z0-9]+$",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern JAR_FILE_PATTERN = Pattern.compile("\\.jar$",
		Pattern.CASE_INSENSITIVE);
	private static final Charset UTF8_CHARSET = Charset.forName("utf-8");
	private static final String MOTD;

	static {
		InputStream in = null;
		String motd;
		try {
			ClassLoader tcl = Thread.currentThread().getContextClassLoader();
			in = tcl.getResourceAsStream("motd.txt");
			motd = IOUtils.toString(in, UTF8_CHARSET);
		} catch (IOException ignored) {
			motd = "";
		} finally {
			IOUtils.closeQuietly(in);
		}
		MOTD = motd;
	}

	private final String[] args;
	private final List<ConsolePlugin> plugins;
	private final Map<String, Command> commands;
	private final Map<String, Command> commandAliases;

	public ConsoleUI(String[] args) {
		this.args = args;
		this.plugins = new ArrayList<>();
		this.commands = new HashMap<>();
		this.commandAliases = new HashMap<>();
	}

	public Map<String, Command> getCommands() {
		return Collections.unmodifiableMap(commands);
	}

	public Map<String, Command> getCommandAliases() {
		return Collections.unmodifiableMap(commandAliases);
	}

	public void run() throws IOException, ParseException {
		if (!validateArgs()) {
			System.exit(1);
			return;
		}

		// parse command line arguments
		Options options = new Options();
		options.addOption("n", "name", true, "custom host name");
		options.addOption("s", "server", true, "server");
		CommandLineParser parser = new PosixParser();
		CommandLine commandLine = parser.parse(options, args);
		List<String> arguments = (List<String>)commandLine.getArgList();
		if (arguments.size() != 1) {
			throw new RuntimeException("Invalid arguments: " + StringUtils.join(arguments, " "));
		}

		// configure system
		final int localPort = Integer.parseInt(arguments.get(0));
		final String hostName = commandLine.getOptionValue("n");
		final String server = commandLine.getOptionValue("s", DEFAULT_SETTING_KEY);
		final Properties defaultSettings = getDefaultSettings();
		final Properties overrideSettings = getOverrideSettings();
		final Settings settings = new Settings(defaultSettings, overrideSettings, server);
		loadPlugins(defaultSettings, overrideSettings, server);
		loadCommands();

		// start client
		final Client client = new Client(settings, new ConsoleUserInfo());
		client.addConnectionClosedHandler(new ConsoleConnectionClosedHandler());
		client.connect();

		// proxy local port
		String domainName = proxyLocalPort(client, localPort, hostName);
		if (domainName == null) {
			client.shutdown();
			System.exit(1);
		}

		// listen for user commands
		startConsole(client, domainName, localPort);
	}

	private String proxyLocalPort(Client client, int localPort, String hostName) {
		String domainName = null;
		try {
			domainName = client.proxyLocalPort(localPort, hostName);
			if (domainName == null) {
				if (hostName == null) {
					System.err.println("jWormhole server unavailable on this server.");
				} else {
					System.err.println("Host name specified is in use.");
				}
			}
		} catch (IOException exception) {
			LOGGER.error("Unable to proxy local port.", exception);
		}
		return domainName;
	}

	private void startConsole(Client client, String domainName, int localPort) throws IOException {
		// load history and register shutdown hook
		String historyFilePath = System.getenv("HOME") + File.separator + HISTORY_PATH;
		FileHistory history = new FileHistory(new File(historyFilePath));
		registerShutdownHook(client, history);

		// show MOTD
		System.out.println(MOTD);
		System.out.println("   proxying " + domainName + " to localhost:" + localPort + "...\n");

		// start to read commands from console
		System.out.println("Enter command (help to see a list of commands):");
		ConsoleReader console = new ConsoleReader();
		console.setHistory(history);
		console.setPrompt("[" + domainName + "~>" + localPort + "]$ ");
		String line;
		while ((line = console.readLine()) != null) {
			if (StringUtils.isBlank(line)) {
				continue;
			}

			String[] tokens = parseCommand(line);
			if ((tokens == null) || (tokens.length == 0)) {
				LOGGER.warn("Error occurred when parsing command.");
				continue;
			}

			Command command = commands.containsKey(tokens[0]) ? commands.get(tokens[0])
					: commandAliases.get(tokens[0]);
			if (command == null) {
				System.err.println("Unrecognized command: " + line + "\n");
				continue;
			}

			try {
				Command.ArgumentsList argumentsList = new Command.ArgumentsList(command);
				if (!argumentsList.parseValues(tokens)) {
					command.printUsage();
					System.out.println();
					continue;
				}
				command.handle(client, argumentsList);
				System.out.println();
			} catch (RuntimeException exception) {
				LOGGER.error("Error occurred when trying to handle command", exception);
			}
		}
	}

	private void registerShutdownHook(Client client, FileHistory history) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			client.shutdown();
			for (ConsolePlugin plugin : plugins) {
				try {
					plugin.shutdown();
				} catch (RuntimeException ignored) {
				}
			}
			try {
				history.flush();
			} catch (IOException ignored) {
			}
			try {
				TerminalFactory.get().restore();
			} catch (Exception ignored) {
			}
		}));
	}

	private String[] parseCommand(String command) {
		InputStream in = new ByteArrayInputStream(command.getBytes(UTF8_CHARSET));
		CSVReader reader = new CSVReader(new InputStreamReader(in), ' ');
		try {
			return reader.readNext();
		} catch (IOException exception) {
			LOGGER.warn("Unable to parse command.", exception);
			return null;
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	private void loadPlugins(Properties defaultSettings, Properties overrideSettings,
							 String server) {
		// see if we need to load any..
		String pluginsSettings = SettingsUtils.getSetting(defaultSettings, overrideSettings,
				Settings.SETTING_PREFIX, null, CONSOLE_PLUGINS_SETTING);
		if (StringUtils.isBlank(pluginsSettings)) {
			return;
		}

		// load jars
		String pluginsPath = System.getenv("HOME") + File.separator + PLUGINS_PATH;
		URL[] urls = getPluginJars(pluginsPath);
		URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());

		// initialize handlers
		try {
			for (String pluginClassName : pluginsSettings.split(",")) {
				if (StringUtils.isBlank(pluginClassName)) {
					continue;
				}
				Class<?> pluginClass = Class.forName(pluginClassName.trim(), true, classLoader);
				if (!ConsolePlugin.class.isAssignableFrom(pluginClass)) {
					throw new RuntimeException("Invalid CommandHandler class: " +
						pluginClass.getName());
				}

				ConsolePlugin plugin = (ConsolePlugin)pluginClass.getConstructor().newInstance();
				plugin.configure(overrideSettings, server);
				plugins.add(plugin);
			}
		} catch (ClassNotFoundException exception) {
			throw new RuntimeException("Unable to load CommandHandler class.", exception);
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
				InvocationTargetException exception) {
			throw new RuntimeException("Unable to instantiate CommandHandler.", exception);
		}
	}

	private void loadCommands() throws IOException {
		List<CommandFactory> commandFactories = new ArrayList<>();
		commandFactories.add(new QuitCommandFactory());
		commandFactories.add(new HelpCommandFactory(this));
		commandFactories.add(new StatusCommandFactory());
		for (ConsolePlugin plugin : plugins) {
			List<CommandFactory> factories = plugin.getCommandFactories();
			if ((factories != null) && !factories.isEmpty()) {
				commandFactories.addAll(factories);
			}
		}
		for (CommandFactory factory : commandFactories) {
			Command command = factory.createCommand();
			commands.put(command.getName(), command);

			List<String> aliases = command.getAliases();
			if (aliases == null) {
				continue;
			}

			for (String alias : aliases) {
				commandAliases.put(alias, command);
			}
		}
	}

	private static URL[] getPluginJars(String path) {
		File directory = new File(path);
		if (!directory.exists() || !directory.isDirectory()) {
			return new URL[0];
		}

		try {
			File[] files = directory.listFiles();
			if (files == null) {
				return new URL[0];
			}

			List<URL> urls = new ArrayList<>(files.length);
			for (File file : files) {
				if (!file.isFile() || !JAR_FILE_PATTERN.matcher(file.getName()).find()) {
					continue;
				}
				urls.add(file.toURI().toURL());
			}

			URL[] result = new URL[urls.size()];
			return urls.toArray(result);
		} catch (MalformedURLException exception) {
			throw new RuntimeException(exception);
		}
	}

	private static Properties getDefaultSettings() throws IOException {
		return readSettingsFromClassPathResource(Thread.currentThread().getContextClassLoader(),
				DEFAULT_SETTINGS_FILE);
	}

	private static Properties getOverrideSettings() throws IOException {
		return readSettingsFromFileRelativeToHome(OVERRIDE_SETTINGS_FILE);
	}

	private boolean validateArgs() {
		if (args.length < 1) {
			System.err.println("Please specify local port.");
			return false;
		}
		if ((args.length >= 2) && !HOST_KEY_PATTERN.matcher(args[1]).matches()) {
			System.err.println("Invalid host name.");
			return false;
		}
		return true;
	}

}
