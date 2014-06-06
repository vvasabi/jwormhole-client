package com.bradchen.jwormhole.client.console;

import com.bradchen.jwormhole.client.ConnectionClosedHandler;

public class ConsoleConnectionClosedHandler implements ConnectionClosedHandler {

	@Override
	public void connectionClosed(int localPort, String domainName) {
		System.exit(1);
	}

}
