package com.bradchen.jwormhole.client.console;

import com.bradchen.jwormhole.client.Client;

import java.util.List;

/**
 * Handles a user command entered via console. This provides the possibility of creating custom
 * commands that take advantage of the tunnel.
 */
public interface CommandHandler {

	List<String> getCommandHints();

	boolean handle(Client client, String command);

}
