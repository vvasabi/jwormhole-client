package com.bradchen.jwormhole.client.console.commands;

import com.bradchen.jwormhole.client.console.ConsoleUI;

public class HelpCommandFactory implements CommandFactory {

	private final ConsoleUI consoleUI;

	public HelpCommandFactory(ConsoleUI consoleUI) {
		this.consoleUI = consoleUI;
	}

	@Override
	public Command createCommand() {
		return new HelpCommand(consoleUI);
	}

}
