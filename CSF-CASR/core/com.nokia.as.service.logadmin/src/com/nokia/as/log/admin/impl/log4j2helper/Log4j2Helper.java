package com.nokia.as.log.admin.impl.log4j2helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.nokia.as.log.service.admin.LogHandler;

public class Log4j2Helper {

	private final static String APPENDER_NAME = Log4j2Helper.class.getName();

	public void setLevel(String loggerName, String lev, LoggerContext ctx) {
		Level level = Level.valueOf(lev);
		boolean isRoot = "rootLogger".equals(loggerName) || loggerName.isEmpty();
		Configuration conf = ctx.getConfiguration();

		if (isRoot) {
			conf.getRootLogger().setLevel(level);
			for (LoggerConfig logConf : conf.getLoggers().values()) {
				logConf.setLevel(level);
			}
		} else {
			LoggerConfig logConf = conf.getLoggerConfig(loggerName);

			if (!logConf.equals(conf.getRootLogger())) {
				if (!logConf.getLevel().equals(level)) {
					logConf.setLevel(level);
				}
			}

			for (Logger l : ctx.getLoggers()) {
				if (l.getName().startsWith(loggerName)) {
					LoggerConfig loggerConfig = conf.getLoggerConfig(l.getName());
					if (!loggerConfig.equals(conf.getRootLogger())) {
						loggerConfig.setLevel(level);
					} else {
						loggerConfig = new LoggerConfig(l.getName(), level, true);
						loggerConfig.setParent(conf.getRootLogger());
						conf.addLogger(l.getName(), loggerConfig);
					}
				}
			}
		}
		ctx.updateLoggers();
	}

	public String getLevel(String loggerName) {
		Logger logger;

		if ("rootLogger".equals(loggerName)) {
			logger = LogManager.getRootLogger();
		} else {
			logger = LogManager.getLogger(loggerName);
		}
		if (logger != null) {
			Level level = logger.getLevel();
			level = level == null ? Level.WARN : level;
			return level.name();
		}

		return null;
	}

	public void addLogHandler(LogHandler logHandler, LoggerContext ctx, boolean format) {
		String appenderName = getAppenderName(logHandler);
		Configuration conf = ctx.getConfiguration();
		for (final LoggerConfig loggerConfig : conf.getLoggers().values()) {
			if (loggerConfig.getAppenders().get(appenderName) != null) {
				throw new IllegalArgumentException("appender already registered");
			}
		}

		PatternLayout layout = PatternLayout.newBuilder().withPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN).build();
		LogConsumerAppender appender = new LogConsumerAppender(logHandler, appenderName, conf.getFilter(), layout, false, format);
		appender.start();
		conf.addAppender(appender);
		for (final LoggerConfig loggerConfig : conf.getLoggers().values()) {
			loggerConfig.addAppender(appender, null, null);
		}
		conf.getRootLogger().addAppender(appender, null, null);
		ctx.updateLoggers();
	}

	public void removeLogHandler(LogHandler logHandler, LoggerContext ctx) {
		String appenderName = getAppenderName(logHandler);
		Configuration conf = ctx.getConfiguration();
		for (final LoggerConfig loggerConfig : conf.getLoggers().values()) {
			loggerConfig.removeAppender(appenderName);
		}
		conf.getRootLogger().removeAppender(appenderName);
		ctx.updateLoggers();
	}

	private String getAppenderName(LogHandler logHandler) {
		StringBuilder sb = new StringBuilder();
		sb.append(APPENDER_NAME);
		sb.append("-");
		sb.append(System.identityHashCode(logHandler));
		return sb.toString();
	}

}
