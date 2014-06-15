package com.bradchen.jwormhole.client.console.commands;

import com.bradchen.jwormhole.client.Client;

import java.util.Arrays;
import java.util.List;

public class QuitCommand extends Command {

	@Override
	public String getName() {
		return "quit";
	}

	@Override
	public String getDescription() {
		return "Exit jWormhole.";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("q");
	}

	@Override
	public List<Argument> getArguments() {
		return null;
	}

	@Override
	public void handle(Client client, ArgumentsList argumentsList) {
		client.shutdown();
		System.exit(0);
	}

}
