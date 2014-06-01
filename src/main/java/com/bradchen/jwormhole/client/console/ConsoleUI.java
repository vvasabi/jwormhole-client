package com.bradchen.jwormhole.client.console;

import com.bradchen.jwormhole.client.Client;
import com.bradchen.jwormhole.client.Host;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public final class ConsoleUI {

	private static final String DEFAULT_SETTING_KEY = "default";

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

	public void run() throws IOException {
		if (!validateArgs()) {
			System.exit(1);
			return;
		}

		final int localPort = Integer.parseInt(args[0]);
		final Client client = new Client(DEFAULT_SETTING_KEY, new ConsoleUserInfo());
		client.connect();
		Host host = client.createHost();
		if (host == null) {
			client.shutdown();
			System.err.println("jWormhole server unavailable.");
			System.exit(1);
			return;
		}

		client.proxyLocalPort(host, localPort);
		System.out.println("Proxying " + host.getDomainName() + " to localhost:" + localPort
			+ "...");
		BufferedReader reader = null;
		String line;
		try {
			reader = new BufferedReader(new InputStreamReader(System.in));
			showCommandHint();
			while ((line = reader.readLine()) != null) {
				for (CommandHandler handler : commandHandlers) {
					if (handler.handle(client, host, args, line)) {
						break;
					}
				}
				showCommandHint();
			}
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	private void showCommandHint() {
		List<String> hints = new ArrayList<>(commandHandlers.size());
		for (CommandHandler handler : commandHandlers) {
			String hint = handler.getCommandHint();
			if (hint != null) {
				hints.add(hint);
			}
		}
		System.out.print("Please enter command");
		if (hints.size() > 0) {
			System.out.print(" (" + StringUtils.join(hints, ", ") + ")");
		}
		System.out.println(":");
	}

	private boolean validateArgs() {
		if (args.length < 1) {
			System.err.println("Please specify local port.");
			return false;
		}
		return true;
	}

}
