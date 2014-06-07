package com.bradchen.jwormhole.client.console;

import com.bradchen.jwormhole.client.Client;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A simple Terminal-based UI.
 */
public final class ConsoleUI {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleUI.class);
	private static final String DEFAULT_SETTING_KEY = "default";
	private static final Pattern HOST_KEY_PATTERN = Pattern.compile("^[-_.a-z0-9]+$",
		Pattern.CASE_INSENSITIVE);

	private final String[] args;
	private final List<CommandHandler> commandHandlers;

	public ConsoleUI(String[] args) {
		this.args = args;
		this.commandHandlers = new ArrayList<>();
		addCommandHandler(new QuitCommandHandler());
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

		final int localPort = Integer.parseInt(arguments.get(0));
		final String hostName = commandLine.getOptionValue("n");
		final String server = commandLine.getOptionValue("s", DEFAULT_SETTING_KEY);
		final Client client = new Client(server, new ConsoleUserInfo());
		client.addConnectionClosedHandler(new ConsoleConnectionClosedHandler());
		client.connect();
		String domainName = client.proxyLocalPort(localPort, hostName);
		if (domainName == null) {
			client.shutdown();
			System.err.println("jWormhole server unavailable on this server.");
			System.exit(1);
			return;
		}

		Runtime.getRuntime().addShutdownHook(new Thread(client::shutdown));
		System.out.println("Proxying " + domainName + " to localhost: " + localPort + "...");
		BufferedReader reader = null;
		String line;
		try {
			reader = new BufferedReader(new InputStreamReader(System.in));
			showCommandHint();
			while ((line = reader.readLine()) != null) {
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
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	private void showCommandHint() {
		List<String> allHints = new ArrayList<>(commandHandlers.size());
		for (CommandHandler handler : commandHandlers) {
			List<String> hints = handler.getCommandHints();
			if (hints != null) {
				allHints.addAll(hints);
			}
		}
		System.out.print("Please enter command");
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
