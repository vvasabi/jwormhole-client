package com.bradchen.jwormhole.client.console;

import com.bradchen.jwormhole.client.Client;
import com.bradchen.jwormhole.client.Settings;
import com.bradchen.jwormhole.client.SettingsUtils;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
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

	private static final String COMMAND_HANDLERS_SETTING = "console.commandHandlers";
	private static final String DEFAULT_SETTING_KEY = "default";
	private static final Pattern HOST_KEY_PATTERN = Pattern.compile("^[-_.a-z0-9]+$",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern JAR_FILE_PATTERN = Pattern.compile("\\.jar$",
		Pattern.CASE_INSENSITIVE);

	private final String[] args;
	private final List<CommandHandler> commandHandlers;

	public ConsoleUI(String[] args) {
		this.args = args;
		this.commandHandlers = new ArrayList<>();
	}

	public void addCommandHandler(CommandHandler commandHandler) {
		commandHandlers.add(commandHandler);
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
		loadCommandHandlers(defaultSettings, overrideSettings, server);

		// start client
		final Client client = new Client(settings, new ConsoleUserInfo());
		client.addConnectionClosedHandler(new ConsoleConnectionClosedHandler());
		client.connect();
		String domainName = client.proxyLocalPort(localPort, hostName);
		if (domainName == null) {
			client.shutdown();
			if (hostName == null) {
				System.err.println("jWormhole server unavailable on this server.");
			} else {
				System.err.println("jWormhole server unavailable on this server, or specified " +
					"host name unavailable.");
			}
			System.exit(1);
			return;
		}

		// load history and register shutdown hook
		System.out.println("Proxying " + domainName + " to localhost:" + localPort + "...");
		String historyFilePath = System.getenv("HOME") + File.separator + HISTORY_PATH;
		final FileHistory history = new FileHistory(new File(historyFilePath));
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			client.shutdown();
			try {
				history.flush();
			} catch (IOException ignored) {
			}
			try {
				TerminalFactory.get().restore();
			} catch (Exception ignored) {
			}
		}));

		// start to read line from console
		showCommandHint();
		ConsoleReader console = new ConsoleReader();
		console.setHistory(history);
		console.setPrompt("> ");
		String line;
		while ((line = console.readLine()) != null) {
			if (StringUtils.isBlank(line)) {
				continue;
			}

			boolean handled = false;
			for (CommandHandler handler : commandHandlers) {
				try {
					if (handler.handle(client, line)) {
						handled = true;
						break;
					}
				} catch (RuntimeException exception) {
					LOGGER.error("Error occurred when trying to handle command", exception);
				}
			}
			if (!handled) {
				System.out.println("Unrecognized command: " + line);
			}
			showCommandHint();
		}
	}

	private void loadCommandHandlers(Properties defaultSettings, Properties overrideSettings,
									 String server) {
		// default one bundled
		addCommandHandler(new QuitCommandHandler());

		// see if we need to load any..
		String handlersSetting = SettingsUtils.getSetting(defaultSettings, overrideSettings,
			Settings.SETTING_PREFIX, null, COMMAND_HANDLERS_SETTING);
		if (StringUtils.isBlank(handlersSetting)) {
			return;
		}

		// load jars
		String pluginsPath = System.getenv("HOME") + File.separator + PLUGINS_PATH;
		URL[] urls = getPluginJars(pluginsPath);
		URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());

		// initialize handlers
		try {
			for (String handlerClassName : handlersSetting.split(",")) {
				if (StringUtils.isBlank(handlerClassName)) {
					continue;
				}
				Class<?> handlerClass = Class.forName(handlerClassName.trim(), true, classLoader);
				if (!CommandHandler.class.isAssignableFrom(handlerClass)) {
					throw new RuntimeException("Invalid CommandHandler class: " +
						handlerClass.getName());
				}

				CommandHandler handler = (CommandHandler)handlerClass.getConstructor()
					.newInstance();
				handler.configure(overrideSettings, server);
				addCommandHandler(handler);
			}
		} catch (ClassNotFoundException exception) {
			throw new RuntimeException("Unable to load CommandHandler class.", exception);
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
				InvocationTargetException exception) {
			throw new RuntimeException("Unable to instantiate CommandHandler.", exception);
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

	private void showCommandHint() {
		List<String> allHints = new ArrayList<>(commandHandlers.size());
		for (CommandHandler handler : commandHandlers) {
			List<String> hints = handler.getCommandHints();
			if (hints != null) {
				allHints.addAll(hints);
			}
		}
		System.out.print("\nPlease enter command");
		if (allHints.size() > 0) {
			System.out.print(" (" + StringUtils.join(allHints, ", ") + ")");
		}
		System.out.println(":");
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
