package com.bradchen.jwormhole.client;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class Main {

	private static final String DEFAULT_SETTING_KEY = "default";

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.println("Please specify local port.");
			System.exit(1);
			return;
		}

		final int localPort = Integer.parseInt(args[0]);
		Client client = new Client(DEFAULT_SETTING_KEY, new CommandLineUserInfo());
		Runtime.getRuntime().addShutdownHook(new Thread(client::shutdown));

		client.connect();
		Host host = client.createHost();
		if (host == null) {
			System.err.println("jWormhole server unavailable.");
			System.exit(1);
			return;
		}

		client.proxyLocalPort(host, localPort);
		System.out.println("Proxying " + host.getDomainName() + " to localhost at " + localPort
			+ "...");
		BufferedReader reader = null;
		String line;
		try {
			reader = new BufferedReader(new InputStreamReader(System.in));
			while ((line = reader.readLine()) != null) {
				if ("q".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
					System.exit(0);
					break;
				}
			}
		} finally {
			IOUtils.closeQuietly(reader);
		}

	}

}
