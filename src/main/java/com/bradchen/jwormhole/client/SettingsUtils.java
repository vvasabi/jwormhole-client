package com.bradchen.jwormhole.client;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class SettingsUtils {

	public static Properties readSettingsFromClassPathResource(String path) throws IOException {
		ClassLoader tcl = Thread.currentThread().getContextClassLoader();
		InputStream inputStream = null;
		try {
			inputStream = tcl.getResourceAsStream(path);
			return readPropertiesFile(inputStream);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	public static Properties readSettingsFromFileRelativeToHome(String path) throws IOException {
		String fullPath = getFilePathRelativeToHome(path);
		if (fullPath == null) {
			return null;
		}

		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(new File(fullPath));
			return readPropertiesFile(inputStream);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	private static Properties readPropertiesFile(InputStream inputStream) throws IOException {
		Properties properties = new Properties();
		properties.load(inputStream);
		return properties;
	}

	public static int getSettingInteger(Properties defaults, Properties overrides,
										String defaultPrefix, String overridePrefix, String key) {
		return Integer.parseInt(getSetting(defaults, overrides, defaultPrefix, overridePrefix,
			key));
	}

	public static String getSetting(Properties defaults, Properties overrides, String defaultPrefix,
									String overridePrefix, String key) {
		String overrideKey = defaultPrefix + "." + overridePrefix + "." + key;
		if ((overrides != null) && overrides.containsKey(overrideKey)) {
			return (String)overrides.get(overrideKey);
		}
		return (String)defaults.get(defaultPrefix + "." + key);
	}

	public static String getFilePathRelativeToHome(String relativePath) {
		String path = System.getenv("HOME") + "/" + relativePath;
		File file = new File(path);
		return file.exists() ? path : null;
	}

	private SettingsUtils() {
	}

}
