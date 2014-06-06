package com.bradchen.jwormhole.client.console;

import com.bradchen.jwormhole.client.Client;

import java.util.List;

public interface CommandHandler {

	List<String> getCommandHints();

	boolean handle(Client client, String[] args, String command);

}
