// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.logging;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.prefs.Preferences;

import com.nokia.licensing.interfaces.CLJLPreferences;
import com.nokia.licensing.plugins.PluginRegistry;
import com.nokia.licensing.utils.Constants;


public class LoggingConfig {
	/**  */
	private static final int DEFAULT_LOG_SIZE = 20 * 1000 * 1000;
	private static final int DEFAULT_LOG_COUNT = 5;
	public static Logger nokiaActivityRoot = null;
	public static Logger nokiaErrorRoot = null;
	public static Logger nokiaTraceRoot = null;

	/**
	 * @param args
	 */
	public static void initialize() {
		OESAuditLevel.SUCCESS.intValue();

		try {

			final CLJLPreferences cljlPreferences = PluginRegistry.getRegistry().getPlugin(CLJLPreferences.class);
			final Preferences systemRoot = cljlPreferences.getPreferencesSystemRoot();
			final Preferences pref = systemRoot.node("loggingConfig");
			final String logsDir = "/var/opt/nokia/oss/global/licensing/CLJL/log/";

			final String error_log = pref.get("errorlog", logsDir + "as_error.log");
			final int error_log_size = pref.getInt("errorlog_size", DEFAULT_LOG_SIZE);
			final int error_log_count = pref.getInt("errorlog_count", DEFAULT_LOG_COUNT);

			final String trace_log = pref.get("tracelog", logsDir + "as_trace.log");
			final int trace_log_size = pref.getInt("tracelog_size", DEFAULT_LOG_SIZE);
			final int trace_log_count = pref.getInt("tracelog_count", DEFAULT_LOG_COUNT);

			final String activity_log = pref.get("activitylog", logsDir + "as_activity.log");
			final int activity_log_size = pref.getInt("activitylog_size", DEFAULT_LOG_SIZE);
			final int activity_log_count = pref.getInt("activitylog_count", DEFAULT_LOG_COUNT);

			LoggingConfig.initializeTraceLogger(trace_log, Constants.TRACE_LOGGER_NAME, trace_log_size,
					trace_log_count);
			LoggingConfig.initializeErrorLogger(error_log, Constants.ERROR_LOGGER_NAME, error_log_size,
					error_log_count);
			LoggingConfig.initializeActivityLogger(activity_log, Constants.ACTIVITY_LOGGER_NAME, activity_log_size,
					activity_log_count);

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Closes all open log handlers. It's mandatory to invoke this method at application shutdown if
	 * {@link LoggingConfig#initialize(String)} is called to configure CLJL logs.
	 */
	public static void shutdown() {
		removeAllHandlers(nokiaErrorRoot);
		removeAllHandlers(nokiaTraceRoot);
		removeAllHandlers(nokiaActivityRoot);
	}

	private static void removeAllHandlers(final Logger logger) {
		for (final Handler handler : logger.getHandlers()) {
			handler.close();
			logger.removeHandler(handler);
		}
	}

	protected static void initializeErrorLogger(final String logFileName, final String loggerName,
			final int logFileSize, final int logFilesCount) throws IOException {
		final FileHandler errorHandler = new FileHandler(logFileName, logFileSize, logFilesCount, true);

		errorHandler.setLevel(Level.WARNING);

		final Formatter formatter = new SimpleFormatter();

		errorHandler.setFormatter(formatter);
		nokiaErrorRoot = Logger.getLogger(loggerName);
		removeAllHandlers(nokiaErrorRoot);
		nokiaErrorRoot.addHandler(errorHandler);
		nokiaErrorRoot.setUseParentHandlers(false);
	}

	protected static void initializeTraceLogger(final String logFileName, final String loggerName,
			final int logFileSize, final int logFilesCount) throws IOException {
		final FileHandler traceHandler = new FileHandler(logFileName, logFileSize, logFilesCount, true);

		traceHandler.setLevel(Level.ALL);

		final Formatter formatter = new SimpleFormatter();

		traceHandler.setFormatter(formatter);
		nokiaTraceRoot = Logger.getLogger(loggerName);
		removeAllHandlers(nokiaTraceRoot);
		nokiaTraceRoot.addHandler(traceHandler);
		nokiaTraceRoot.setUseParentHandlers(false);
	}

	protected static void initializeActivityLogger(final String logFileName, final String loggerName,
			final int logFileSize, final int logFilesCount) throws IOException {
		final FileHandler activityHandler = new FileHandler(logFileName, logFileSize, logFilesCount, true);

		activityHandler.setLevel(Level.INFO);

		final Formatter formatter = new SimpleFormatter();

		activityHandler.setFormatter(formatter);
		nokiaActivityRoot = Logger.getLogger(loggerName);
		removeAllHandlers(nokiaActivityRoot);
		nokiaActivityRoot.addHandler(activityHandler);
		nokiaActivityRoot.setUseParentHandlers(false);
	}

	/**
	 * Checks if the Logger has been initialized. If not then invokes initialize(String loggerName) method to initialize
	 * the logger and returns instance of Logger
	 *
	 * @param loggerName
	 * @return Instance of Logger (after initialization)
	 */
	public static Logger getLogger(final String loggerName) {
		if (loggerName.equals(Constants.TRACE_LOGGER_NAME)) {
			if (nokiaTraceRoot == null) {
				return Logger.getLogger(loggerName);
			} else {
				return nokiaTraceRoot;
			}
		} else if (loggerName.equals(Constants.ERROR_LOGGER_NAME)) {
			if (nokiaErrorRoot == null) {
				return Logger.getLogger(loggerName);
			} else {
				return nokiaErrorRoot;
			}
		} else if (loggerName.equals(Constants.ACTIVITY_LOGGER_NAME)) {
			if (nokiaActivityRoot == null) {
				return Logger.getLogger(loggerName);
			} else {
				return nokiaActivityRoot;
			}
		}
		return Logger.getLogger(loggerName);
	}
}
