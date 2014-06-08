package com.bradchen.jwormhole.client.console;

import com.bradchen.jwormhole.client.Client;

import java.util.List;
import java.util.Properties;

/**
 * Handles a user command entered via console. This provides the possibility of creating custom
 * commands that take advantage of the tunnel.
 */
public interface CommandHandler {

	default void configure(Properties overrideSettings, String server) {}

	List<String> getCommandHints();

	boolean handle(Client client, String command);

}
