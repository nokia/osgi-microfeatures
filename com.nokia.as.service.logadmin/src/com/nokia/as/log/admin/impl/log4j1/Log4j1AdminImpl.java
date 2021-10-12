// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.log.admin.impl.log4j1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.nokia.as.log.service.admin.LogAdmin;
import com.nokia.as.log.service.admin.LogHandler;

public class Log4j1AdminImpl implements LogAdmin {
	
	private final Map<LogHandler, Appender> _appenders = Collections.synchronizedMap(new HashMap<>());
	private final static Logger _log = Logger.getLogger(Log4j1AdminImpl.class);

	@Override
	public void setLevel(String loggerName, String level) {
		Logger logger = loggerName.equals("rootLogger") ? Logger.getRootLogger() : Logger.getLogger(loggerName);
		logger.setLevel(Level.toLevel(level));
	}

	@Override
	public String getLevel(String loggerName) {
		Logger logger = loggerName.equals("rootLogger") ? Logger.getRootLogger() : Logger.getLogger(loggerName);
		Level level = logger.getLevel();
		level = level == null ? Level.WARN : level;
		return level.toString();
	}

	void addLogHandler(LogHandler logHandler, boolean format) {
		Layout rootLayout;
		Logger rootLogger = Logger.getRootLogger();
		Enumeration<Appender> appenders = rootLogger.getAllAppenders();
		Appender latestAppender = null;
		while (appenders.hasMoreElements()) {
			latestAppender = appenders.nextElement();
		}
		if (latestAppender != null) {
			rootLayout = latestAppender.getLayout();
			LogConsumerAppender appender = new LogConsumerAppender(rootLayout, logHandler, format);
			rootLogger.addAppender(appender);
			_appenders.put(logHandler, appender);
		} else {
			_log.warn("could not register log handler, no layout found in log4j configuration.");
		}
	}

	void removeLogHandler(LogHandler logHandler) {
		Appender appender = _appenders.remove(logHandler);
		if (appender != null) {
			Logger rootLogger = Logger.getRootLogger();
			rootLogger.removeAppender(appender);
		}
	}

	public void removeLogHandlers() {
		List<LogHandler> logHandlers = new ArrayList<>();
		_appenders.keySet().stream().forEach(logHandlers::add);
		for (LogHandler logHandler : logHandlers) {
			removeLogHandler(logHandler);
		}
	}

}
