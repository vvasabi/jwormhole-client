package com.bradchen.jwormhole.client.console.commands;

import java.util.Properties;

/**
 * Handles a user command entered via console. This provides the possibility of creating custom
 * commands that take advantage of the tunnel.
 */
public interface CommandFactory {

	Command createCommand();

}
