package com.bradchen.jwormhole.client.console;

import com.bradchen.jwormhole.client.console.commands.CommandFactory;

import java.util.List;
import java.util.Properties;

public interface ConsolePlugin {

	default void configure(Properties overrideSettings, String server) {}

	default void shutdown() {}

	List<CommandFactory> getCommandFactories();

}
