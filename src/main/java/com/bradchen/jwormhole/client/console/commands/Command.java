package com.bradchen.jwormhole.client.console.commands;

import com.bradchen.jwormhole.client.Client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Command {

	public abstract String getName();
	public abstract String getDescription();
	public abstract List<String> getAliases();
	public abstract List<Argument> getArguments();
	public abstract void handle(Client client, ArgumentsList argumentsList);

	public void printUsage() {
		System.out.print("Usage: " + getName());
		List<Argument> arguments = getArguments();
		if (arguments == null) {
			System.out.println();
			return;
		}

		for (Argument argument : getArguments()) {
			System.out.print(" ");
			if (argument.isOptional()) {
				System.out.print("[" + argument.getName() + "]");
			} else {
				System.out.print("<" + argument.getName() + ">");
			}
		}
		System.out.println();
	}

	public static class Argument {

		private final String name;
		private final String description;
		private final boolean optional;

		public Argument(String name, String description) {
			this(name, description, false);
		}

		public Argument(String name, String description, boolean optional) {
			this.name = name;
			this.description = description;
			this.optional = optional;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public boolean isOptional() {
			return optional;
		}

	}

	public static class ArgumentsList {

		private final List<Argument> arguments;
		private final Map<String, String> values;

		public ArgumentsList(Command command) {
			List<Argument> args = command.getArguments();
			this.arguments = (args == null) ? Collections.emptyList() :
				Collections.unmodifiableList(args);
			this.values = new HashMap<>();
		}

		public String getValue(String name) {
			return values.get(name);
		}

		public boolean parseValues(String[] rawValues) {
			if ((rawValues.length - 1) > arguments.size()) {
				return false;
			}

			int index = 0;
			for (Argument argument : arguments) {
				if ((index + 1) >= rawValues.length) {
					if (argument.isOptional()) {
						break;
					}
					return false;
				}
				values.put(argument.getName(), trimQuotes(rawValues[index + 1]));
				index++;
			}
			return true;
		}

		private static String trimQuotes(String str) {
			return str.trim().replaceAll("^\"|\"$", "");
		}

	}

}
