package com.bradchen.jwormhole.client.console.commands;

public class QuitCommandFactory implements CommandFactory {

	@Override
	public Command createCommand() {
		return new QuitCommand();
	}

}
