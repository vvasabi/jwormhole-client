package com.bradchen.jwormhole.client.console;

import com.bradchen.jwormhole.client.Client;
import com.bradchen.jwormhole.client.Host;

import java.util.List;

public interface CommandHandler {

	List<String> getCommandHints();

	boolean handle(Client client, Host host, String[] args, String command);

}
