package com.bradchen.jwormhole.client.console;

import org.apache.commons.cli.ParseException;

import java.io.IOException;

public final class Main {

	public static void main(String[] args) throws IOException, ParseException {
		new ConsoleUI(args).run();
	}

}
