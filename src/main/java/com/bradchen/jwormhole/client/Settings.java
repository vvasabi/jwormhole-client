package com.bradchen.jwormhole.client;

import java.util.Properties;

public final class Settings {

	private static final String SETTING_PREFIX = "jwormhole.client.";

	private final String serverSshHost;
	private final int serverSshPort;
	private final int serverControllerPort;
	private final String serverUsername;

	public Settings(Properties defaults, Properties overrides, String overrideKey) {
		serverSshHost = getSetting(defaults, overrides, overrideKey, "serverSshHost");
		serverSshPort = getSettingInteger(defaults, overrides, overrideKey, "serverSshPort");
		serverControllerPort = getSettingInteger(defaults, overrides, overrideKey,
			"serverControllerPort");
		serverUsername = getSetting(defaults, overrides, overrideKey, "serverUsername");
	}

	private static int getSettingInteger(Properties defaults, Properties overrides,
										 String overrideKey, String key) {
		return Integer.parseInt(getSetting(defaults, overrides, overrideKey, key));
	}

	private static String getSetting(Properties defaults, Properties overrides,
									 String overrideKey, String key) {
		String fullKey = SETTING_PREFIX + overrideKey + "." + key;
		if ((overrides != null) && overrides.containsKey(fullKey)) {
			return (String)overrides.get(fullKey);
		}
		return (String)defaults.get(SETTING_PREFIX + key);
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

}
