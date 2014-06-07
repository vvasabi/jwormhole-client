package com.bradchen.jwormhole.client.console;

import com.bradchen.jwormhole.client.Client;

import java.util.Arrays;
import java.util.List;

public class QuitCommandHandler implements CommandHandler {

	@Override
	public List<String> getCommandHints() {
		return Arrays.asList("q to quit");
	}

	@Override
	public boolean handle(Client client, String command) {
		if ("q".equalsIgnoreCase(command) || "quit".equalsIgnoreCase(command)) {
			client.shutdown();
			System.exit(0);
			return true;
		}
		return false;
	}

}
