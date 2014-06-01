package com.bradchen.jwormhole.client;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class Main {

	private static final String DEFAULT_SETTING_KEY = "default";
	private static final long RENEW_PERIOD = 20;

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.println("Please specify local port.");
			System.exit(1);
			return;
		}

		final int localPort = Integer.parseInt(args[0]);
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		final Client client = new Client(DEFAULT_SETTING_KEY, new CommandLineUserInfo());
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			client.disconnect();
			scheduler.shutdown();
		}));

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
		scheduler.scheduleAtFixedRate(() -> {
			try {
				client.renewHost(host);
			} catch (IOException exception) {
				exception.printStackTrace();
				System.exit(1);
			}
		}, RENEW_PERIOD, RENEW_PERIOD, TimeUnit.SECONDS);

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
