package com.bradchen.jwormhole.client.console;

import com.bradchen.jwormhole.client.Client;
import com.bradchen.jwormhole.client.Host;

public interface CommandHandler {

	String getCommandHint();

	boolean handle(Client client, Host host, String[] args, String command);

}
