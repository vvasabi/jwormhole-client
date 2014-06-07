package com.bradchen.jwormhole.client.console;

import com.bradchen.jwormhole.client.ConnectionLostHandler;

/**
 * Exit the program if connection is lost.
 */
public class ConsoleConnectionClosedHandler implements ConnectionLostHandler {

	@Override
	public void connectionClosed(int localPort, String domainName) {
		System.exit(1);
	}

}
