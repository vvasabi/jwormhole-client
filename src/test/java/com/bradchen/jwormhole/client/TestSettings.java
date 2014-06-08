package com.bradchen.jwormhole.client;

import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Properties;

import static com.bradchen.jwormhole.client.SettingsUtils.readSettingsFromClassPathResource;
import static org.testng.Assert.assertEquals;

public class TestSettings {

	@Test
	public void testDefaultSettings() throws IOException {
		Properties defaultSettings = getDefaultSettings();
		Settings settings = new Settings(defaultSettings, null, "default");
		assertEquals(settings.getServerSshHost(), "");
		assertEquals(settings.getServerSshPort(), 22);
		assertEquals(settings.getServerUsername(), "");
		assertEquals(settings.getServerControllerPort(), 12700);
		assertEquals(settings.getKeepaliveInterval(), 20);
	}

	@Test
	public void testOverrideSettings() throws IOException {
		Properties defaultSettings = getDefaultSettings();
		Settings settings = new Settings(defaultSettings, createOverrideSettings("default"),
			"default");
		assertEquals(settings.getServerSshHost(), "test-server");
		assertEquals(settings.getServerSshPort(), 123);
		assertEquals(settings.getServerUsername(), "user");
		assertEquals(settings.getServerControllerPort(), 2345);
		assertEquals(settings.getKeepaliveInterval(), 4321);
	}

	@Test
	public void testOverrideSettingsWithCustomServer() throws IOException {
		Properties defaultSettings = getDefaultSettings();
		Settings settings = new Settings(defaultSettings, createOverrideSettings("custom"),
			"custom");
		assertEquals(settings.getServerSshHost(), "test-server");
		assertEquals(settings.getServerSshPort(), 123);
		assertEquals(settings.getServerUsername(), "user");
		assertEquals(settings.getServerControllerPort(), 2345);
		assertEquals(settings.getKeepaliveInterval(), 4321);
	}

	private Properties createOverrideSettings(String name) {
		Properties overrideSettings = new Properties();
		String prefix = "jwormhole.client." + name + ".";
		overrideSettings.put(prefix + "serverSshHost", "test-server");
		overrideSettings.put(prefix + "serverSshPort", "123");
		overrideSettings.put(prefix + "serverUsername", "user");
		overrideSettings.put(prefix + "serverControllerPort", "2345");
		overrideSettings.put(prefix + "keepaliveInterval", "4321");
		return overrideSettings;
	}

	private Properties getDefaultSettings() throws IOException {
		return readSettingsFromClassPathResource(Thread.currentThread().getContextClassLoader(),
			"client.default.properties");
	}

}
