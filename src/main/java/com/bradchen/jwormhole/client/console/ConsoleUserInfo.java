package com.bradchen.jwormhole.client.console;

import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleUserInfo implements UIKeyboardInteractive, UserInfo {

	private final Console console;
	private String passphrase;
	private String password;

	public ConsoleUserInfo() {
		this.console = System.console();
	}

	@Override
	public String[] promptKeyboardInteractive(String destination, String name, String instruction,
											  String[] prompt, boolean[] echo) {
		String[] results = new String[prompt.length];
		for (int i = 0; i < prompt.length; i++) {
			results[i] = promptForPassword(prompt[i]);
		}
		return results;
	}

	@Override
	public String getPassphrase() {
		return passphrase;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public boolean promptPassphrase(String prompt) {
		passphrase = promptForPassword(prompt);
		return passphrase != null;
	}

	@Override
	public boolean promptPassword(String prompt) {
		password = promptForPassword(prompt);
		return password != null;
	}

	private String promptForPassword(String prompt) {
		if (console == null) {
			System.out.print(prompt + " ");
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(System.in));
				return reader.readLine();
			} catch (IOException ex) {
				return null;
			} finally {
				IOUtils.closeQuietly(reader);
			}
		} else {
			return new String(console.readPassword("%s ", prompt));
		}
	}

	@Override
	public boolean promptYesNo(String message) {
		showMessage(message + " ");
		String response;
		if (console == null) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(System.in));
				response = reader.readLine();
			} catch (IOException ex) {
				response = null;
			} finally {
				IOUtils.closeQuietly(reader);
			}
		} else {
			response = console.readLine();
		}
		return "yes".equalsIgnoreCase(response) || "y".equals(response);
	}

	@Override
	public void showMessage(String message) {
		if (console == null) {
			System.out.print(message);
		} else {
			console.printf("%s", message);
		}
	}

}
