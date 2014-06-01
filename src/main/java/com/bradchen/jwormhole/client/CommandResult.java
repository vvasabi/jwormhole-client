package com.bradchen.jwormhole.client;

public final class CommandResult {

	private final String output;
	private final int returnCode;

	public CommandResult(String output, int returnCode) {
		this.output = output;
		this.returnCode = returnCode;
	}

	public String getOutput() {
		return output;
	}

	public int getReturnCode() {
		return returnCode;
	}

}
