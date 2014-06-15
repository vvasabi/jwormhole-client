package com.bradchen.jwormhole.client.console.commands;

import com.bradchen.jwormhole.client.Client;
import com.bradchen.jwormhole.client.console.ConsoleUI;
import jline.TerminalFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class HelpCommand extends Command {

	private final ConsoleUI consoleUI;

	public HelpCommand(ConsoleUI consoleUI) {
		this.consoleUI = consoleUI;
	}

	@Override
	public String getName() {
		return "help";
	}

	@Override
	public String getDescription() {
		return "Print help information.";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("h");
	}

	@Override
	public List<Argument> getArguments() {
		return Arrays.asList(new Command.Argument("command", "command to look up", true));
	}

	@Override
	public void handle(Client client, ArgumentsList argumentsList) {
		Map<String, Command> commands = consoleUI.getCommands();
		Map<String, Command> commandAliases = consoleUI.getCommandAliases();
		String commandToLookUp = argumentsList.getValue("command");
		if (commandToLookUp == null) {
			listAvailableCommands(commands);
			return;
		}

		Command command = commands.containsKey(commandToLookUp) ? commands.get(commandToLookUp)
			: commandAliases.get(commandToLookUp);
		if (command == null) {
			System.err.println("Invalid command: " + commandToLookUp);
			return;
		}

		command.printUsage();
		System.out.println(command.getDescription());
		List<Command.Argument> arguments = command.getArguments();
		if ((arguments != null) && !arguments.isEmpty()) {
			System.out.println("\n" + command.getName() + " command arguments: ");
			for (Command.Argument argument : arguments) {
				if (argument.isOptional()) {
					System.out.print("  [" + argument.getName() + "]");
				} else {
					System.out.print("  <" + argument.getName() + ">");
				}
				System.out.println(" " + argument.getDescription());
			}
		}

		List<String> aliases = command.getAliases();
		if ((aliases != null) && !aliases.isEmpty()) {
			System.out.println("\nAliases:");
			System.out.println(StringUtils.join(aliases, ", "));
		}
	}

	private void listAvailableCommands(Map<String, Command> commands) {
		System.out.println("Available commands:");
		int terminalWidth = TerminalFactory.get().getWidth();
		int currWidth = 0;
		SortedSet<String> keys = new TreeSet<>(commands.keySet());
		for (String name : keys) {
			currWidth += name.length();
			if (currWidth > terminalWidth) {
				System.out.println();
				terminalWidth = name.length() + 1;
			} else {
				terminalWidth++; // to account for a white space
			}
			System.out.print(name + " ");
		}
		System.out.println();
	}

}
