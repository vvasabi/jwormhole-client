package com.bradchen.jwormhole.client.console;

import com.bradchen.jwormhole.client.Client;
import com.bradchen.jwormhole.client.Host;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class QuitCommandHandler implements CommandHandler {

	@Override
	public List<String> getCommandHints() {
		return Arrays.asList("q to quit");
	}

	@Override
	public boolean handle(Client client, Host host, String[] args, String command) {
		try {
			if ("q".equalsIgnoreCase(command) || "quit".equalsIgnoreCase(command)) {
				client.removeHost(host);
				client.shutdown();
				System.exit(0);
				return true;
			}
			return false;
		} catch (IOException exception) {
			throw new RuntimeException(exception);
		}
	}

}
