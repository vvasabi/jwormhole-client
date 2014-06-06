package com.bradchen.jwormhole.client;

@FunctionalInterface
public interface ConnectionClosedHandler {

	void connectionClosed(int localPort, String domainName);

}
