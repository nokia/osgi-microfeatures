package com.nextenso.proxylet.mgmt;

/**
 * This Class encapsulates a command.
 * <p/>
 * A command is defined in the deployment and can be launched in the monitoring
 * agent.
 */
public class CommandEvent {

	private int _id;
	private int[] _intArgs;
	private String[] _stringArgs;

	/**
	 * Constructs a new CommandEvent.
	 * 
	 * @param id The command identifier.
	 * @param intArgs The numeric arguments.
	 * @param stringArgs The String arguments
	 */
	public CommandEvent(int id, int[] intArgs, String[] stringArgs) {
		_id = id;
		_intArgs = intArgs;
		_stringArgs = stringArgs;
	}

	/**
	 * Gets the command identifier.
	 * 
	 * @return the identifier.
	 */
	public int getCommandId() {
		return _id;
	}

	/**
	 * Gets the command numeric arguments.
	 * 
	 * @return the numeric arguments.
	 */
	public int[] getCommandIntArgs() {
		return _intArgs;
	}

	/**
	 * Gets the command String arguments.
	 * 
	 * @return the String arguments.
	 */
	public String[] getCommandStringArgs() {
		return _stringArgs;
	}
}
