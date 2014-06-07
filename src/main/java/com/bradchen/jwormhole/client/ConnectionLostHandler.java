package com.bradchen.jwormhole.client;

/**
 * An event listener that is called if connection to jWormhole server is lost.
 */
@FunctionalInterface
public interface ConnectionLostHandler {

	void connectionClosed(int localPort, String domainName);

}
