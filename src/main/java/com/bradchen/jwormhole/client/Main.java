package com.bradchen.jwormhole.client;

import com.bradchen.jwormhole.client.console.ConsoleUI;

import java.io.IOException;

public final class Main {

	public static void main(String[] args) throws IOException {
		ConsoleUI console = new ConsoleUI(args);
		console.run();
	}

}
