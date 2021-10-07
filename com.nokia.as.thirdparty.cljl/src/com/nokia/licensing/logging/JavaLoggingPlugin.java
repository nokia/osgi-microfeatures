/*
 * Copyright (c) 2016 Nokia Solutions and Networks. All rights reserved.
 */
package com.nokia.licensing.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.nokia.licensing.interfaces.LoggingPlugin;
import com.nokia.licensing.utils.Constants;


/**
 * @author wro50095
 *
 */
public class JavaLoggingPlugin implements LoggingPlugin {

	/** Logger for error messages */
	private Logger errorLogger = null;

	/** Logger for trace messages */
	private Logger traceLogger = null;

	/** Logger for info messages */
	private Logger activityLogger = null;

	public JavaLoggingPlugin() {
		this.errorLogger = LoggingConfig.getLogger(Constants.ERROR_LOGGER_NAME);
		this.activityLogger = LoggingConfig.getLogger(Constants.ACTIVITY_LOGGER_NAME);
		this.traceLogger = LoggingConfig.getLogger(Constants.TRACE_LOGGER_NAME);
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
	@Override
	public void fine(final String sourceClass, final String sourceMethod, final String msg) {
		this.traceLogger.logp(Level.FINE, sourceClass, sourceMethod, msg);
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
	@Override
	public void finer(final String sourceClass, final String sourceMethod, final String msg) {
		this.traceLogger.logp(Level.FINER, sourceClass, sourceMethod, msg);
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
	@Override
	public void finest(final String sourceClass, final String sourceMethod, final String msg) {
		this.traceLogger.logp(Level.FINEST, sourceClass, sourceMethod, msg);
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
	@Override
	public void finest(final String sourceClass, final String sourceMethod, final String msg, final Object[] params) {
		this.traceLogger.logp(Level.FINEST, sourceClass, sourceMethod, msg, params);
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
	@Override
	public void info(final String sourceClass, final String sourceMethod, final String msg) {
		this.activityLogger.logp(Level.INFO, sourceClass, sourceMethod, msg);
		this.traceLogger.logp(Level.INFO, sourceClass, sourceMethod, msg);

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
	@Override
	public void error(final String sourceClass, final String sourceMethod, final String msg) {
		this.errorLogger.logp(Level.SEVERE, sourceClass, sourceMethod, msg);
		this.traceLogger.logp(Level.SEVERE, sourceClass, sourceMethod, msg);
		this.activityLogger.logp(Level.SEVERE, sourceClass, sourceMethod, msg);
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
	@Override
	public void error(final String sourceClass, final String sourceMethod, final String msg, final Throwable thrown) {
		this.errorLogger.logp(Level.SEVERE, sourceClass, sourceMethod, msg, thrown);
		this.traceLogger.logp(Level.SEVERE, sourceClass, sourceMethod, msg, thrown);
		this.activityLogger.logp(Level.SEVERE, sourceClass, sourceMethod, msg, thrown);

	}

}
