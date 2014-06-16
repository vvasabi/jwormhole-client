package com.bradchen.jwormhole.client.console.commands;

import com.bradchen.jwormhole.client.Client;
import com.bradchen.jwormhole.client.Settings;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class StatusCommand extends Command {

	private static final DateFormat DATE_TIME_FORMAT
		= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

	@Override
	public String getName() {
		return "status";
	}

	@Override
	public String getDescription() {
		return "Show current status.";
	}

	@Override
	public List<String> getAliases() {
		return null;
	}

	@Override
	public List<Argument> getArguments() {
		return null;
	}

	@Override
	public void handle(Client client, ArgumentsList argumentsList) {
		// time
		long uptime = (System.currentTimeMillis() - client.getProxyStartTime()) / 1000;
		System.out.println("Proxy started at:\t"
			+ DATE_TIME_FORMAT.format(new Date(client.getProxyStartTime())));
		System.out.println("Uptime:\t\t\t"
			+ String.format("%d:%02d:%02d", uptime / 3600, (uptime % 3600) / 60, uptime % 60));

		// server
		Settings settings = client.getSettings();
		System.out.println("Server:\t\t\t" + settings.getServerSshHost() + ":"
			+ settings.getServerSshPort());
		System.out.println("User:\t\t\t" + settings.getServerUsername());

		// proxy
		System.out.println("Proxied domain:\t\t" + client.getProxiedDomainName());
		System.out.println("Local port:\t\t" + client.getLocalPort());
	}

}
