package com.bradchen.jwormhole.client.console.commands;

public class StatusCommandFactory implements CommandFactory {

	@Override
	public Command createCommand() {
		return new StatusCommand();
	}

}
