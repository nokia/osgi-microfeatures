/*
 * Copyright (c) 2016 Nokia Solutions and Networks. All rights reserved.
 */
package com.nokia.licensing.interfaces;

/**
 * @author wro50095
 *
 */
public interface LoggingPlugin {
	/**
	 * Logs a fine debug message.
	 *
	 * @param sourceClass
	 *            Class where the message was written to log
	 * @param sourceMethod
	 *            Method where the message was written to log
	 * @param msg
	 *            Message
	 */
	public void fine(final String sourceClass, final String sourceMethod, final String msg);

	/**
	 * Logs a finer debug message.
	 *
	 * @param sourceClass
	 *            Class where the message was written to log
	 * @param sourceMethod
	 *            Method where the message was written to log
	 * @param msg
	 *            Message
	 */
	public void finer(final String sourceClass, final String sourceMethod, final String msg);

	/**
	 * Logs a finest debug message.
	 *
	 * @param sourceClass
	 *            Class where the message was written to log
	 * @param sourceMethod
	 *            Method where the message was written to log
	 * @param msg
	 *            Message
	 */
	public void finest(final String sourceClass, final String sourceMethod, final String msg);

	/**
	 * Logs a finest debug message.
	 *
	 * @param sourceClass
	 *            Class where the message was written to log
	 * @param sourceMethod
	 *            Method where the message was written to log
	 * @param msg
	 *            Message
	 * @param params
	 *            Additional parameters
	 */
	public void finest(final String sourceClass, final String sourceMethod, final String msg, final Object[] params);

	/**
	 * Logs an info message.
	 *
	 * @param sourceClass
	 *            Class where the message was written to log
	 * @param sourceMethod
	 *            Method where the message was written to log
	 * @param msg
	 *            Message
	 */
	public void info(final String sourceClass, final String sourceMethod, final String msg);

	/**
	 * Logs an error message
	 *
	 * @param sourceClass
	 *            Class where the error was written to log
	 * @param sourceMethod
	 *            Method where the error was written to log
	 * @param msg
	 *            Message
	 */
	public void error(final String sourceClass, final String sourceMethod, final String msg);

	/**
	 * Logs an error message with throwable
	 *
	 * @param sourceClass
	 *            Class where the error was written to log
	 * @param sourceMethod
	 *            Method where the error was written to log
	 * @param msg
	 *            Message
	 * @param thrown
	 *            Throwable which was gotten
	 */
	public void error(final String sourceClass, final String sourceMethod, final String msg, final Throwable thrown);

}
