package com.bradchen.jwormhole.client.console.commands;

import com.bradchen.jwormhole.client.SettingsUtils;

import java.io.IOException;
import java.util.Properties;

public class StatusCommandFactory implements CommandFactory {

	private static final String GIT_PROPERTIES = "git.properties"; // relative to class path

	private final Properties gitProperties;

	public StatusCommandFactory() throws IOException {
		gitProperties = readGitProperties();
	}

	@Override
	public Command createCommand() {
		return new StatusCommand(gitProperties);
	}

	private static Properties readGitProperties() throws IOException {
		return SettingsUtils.readSettingsFromClassPathResource(StatusCommand.class.getClassLoader(),
			GIT_PROPERTIES);
	}

}
