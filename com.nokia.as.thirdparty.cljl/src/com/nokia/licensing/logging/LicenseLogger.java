/*
 * Copyright (c) 2006 Nokia. All rights reserved.
 */
package com.nokia.licensing.logging;

import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.interfaces.LoggingPlugin;
import com.nokia.licensing.plugins.PluginRegistry;


/**
 * Static class which logs messages and errors. The loggers should be registered before use.
 */
public final class LicenseLogger {
	private static volatile LicenseLogger INSTANCE = new LicenseLogger();

	private final LoggingPlugin loggingPlugin;

	/**
	 * Private constructor
	 */
	private LicenseLogger() {
		try {
			this.loggingPlugin = PluginRegistry.getRegistry().getPlugin(LoggingPlugin.class);
		} catch (final LicenseException e) {
			throw new RuntimeException("Error during logging plugin retrieval", e);
		}
	}

	public static LicenseLogger getInstance() {
		LicenseLogger logger = INSTANCE;
		if (logger == null) {
			synchronized (LicenseLogger.class) {
				logger = INSTANCE;
				if (logger == null) {
					logger = new LicenseLogger();
					INSTANCE = logger;
				}
			}
		}
		return logger;

	}

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
	public void fine(final String sourceClass, final String sourceMethod, final String msg) {
		this.loggingPlugin.fine(sourceClass, sourceMethod, msg);
	}

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
	public void finer(final String sourceClass, final String sourceMethod, final String msg) {
		this.loggingPlugin.finer(sourceClass, sourceMethod, msg);
	}

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
	public void finest(final String sourceClass, final String sourceMethod, final String msg) {
		this.loggingPlugin.finest(sourceClass, sourceMethod, msg);
	}

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
	public void finest(final String sourceClass, final String sourceMethod, final String msg, final Object[] params) {
		this.loggingPlugin.finest(sourceClass, sourceMethod, msg, params);
	}

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
	public void info(final String sourceClass, final String sourceMethod, final String msg) {
		this.loggingPlugin.info(sourceClass, sourceMethod, msg);
	}

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
	public void error(final String sourceClass, final String sourceMethod, final String msg) {
		this.loggingPlugin.error(sourceClass, sourceMethod, msg);
	}

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
	public void error(final String sourceClass, final String sourceMethod, final String msg, final Throwable thrown) {
		this.loggingPlugin.error(sourceClass, sourceMethod, msg, thrown);

	}

}
