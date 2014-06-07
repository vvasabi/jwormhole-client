package com.bradchen.jwormhole.client;

import java.util.Properties;

/**
 * Stores all settings read from properties files.
 */
public final class Settings {

	private static final String SETTING_PREFIX = "jwormhole.client";

	private final String serverSshHost;
	private final int serverSshPort;
	private final int serverControllerPort;
	private final String serverUsername;
	private final int keepaliveInterval;

	public Settings(Properties defaults, Properties overrides, String server) {
		serverSshHost = getSetting(defaults, overrides, server, "serverSshHost");
		serverSshPort = getSettingInteger(defaults, overrides, server, "serverSshPort");
		serverControllerPort = getSettingInteger(defaults, overrides, server,
			"serverControllerPort");
		serverUsername = getSetting(defaults, overrides, server, "serverUsername");
		keepaliveInterval = getSettingInteger(defaults, overrides, server,
			"keepaliveInterval");
	}

	private static int getSettingInteger(Properties defaults, Properties overrides, String server,
										 String key) {
		return SettingsUtils.getSettingInteger(defaults, overrides, SETTING_PREFIX, server, key);
	}

	private static String getSetting(Properties defaults, Properties overrides, String server,
									 String key) {
		return SettingsUtils.getSetting(defaults, overrides, SETTING_PREFIX, server, key);
	}

	public String getServerSshHost() {
		return serverSshHost;
	}

	public int getServerSshPort() {
		return serverSshPort;
	}

	public int getServerControllerPort() {
		return serverControllerPort;
	}

	public String getServerUsername() {
		return serverUsername;
	}

	public int getKeepaliveInterval() {
		return keepaliveInterval;
	}

}
