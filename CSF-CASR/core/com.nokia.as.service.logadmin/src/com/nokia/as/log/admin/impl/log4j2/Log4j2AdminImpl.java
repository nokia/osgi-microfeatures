package com.nokia.as.log.admin.impl.log4j2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.spi.LoggerContextFactory;

import com.nokia.as.log.admin.impl.log4j2helper.Log4j2Helper;
import com.nokia.as.log.service.admin.LogAdmin;
import com.nokia.as.log.service.admin.LogHandler;

public class Log4j2AdminImpl implements LogAdmin {
	
	private final Log4j2Helper _log4j2Helper = new Log4j2Helper();
	private final Set<LogHandler> _logHandlers = Collections.synchronizedSet(new HashSet<>());
	
	@Override
	public void setLevel(String loggerName, String lev) {
		LoggerContextFactory factory = LogManager.getFactory();
		ContextSelector selector = ((Log4jContextFactory) factory).getSelector();
		for (LoggerContext ctx : selector.getLoggerContexts()) {
			_log4j2Helper.setLevel(loggerName, lev, ctx);
		}
	}
	
	@Override
	public String getLevel(String loggerName) {
		return _log4j2Helper.getLevel(loggerName);
	}
	
	void addLogHandler(LogHandler logHandler, boolean doFormat) {
		LoggerContextFactory factory = LogManager.getFactory();
		ContextSelector selector = ((Log4jContextFactory) factory).getSelector();
		for (LoggerContext ctx : selector.getLoggerContexts()) {
			_log4j2Helper.addLogHandler(logHandler, ctx, doFormat);
		}
		_logHandlers.add(logHandler);
	}
			
	void removeLogHandler(LogHandler logHandler) {
		if (_logHandlers.remove(logHandler)) {
			LoggerContextFactory factory = LogManager.getFactory();
			ContextSelector selector = ((Log4jContextFactory) factory).getSelector();
			for (LoggerContext ctx : selector.getLoggerContexts()) {
				_log4j2Helper.removeLogHandler(logHandler, ctx);
			}
		}	
	}
	
	void removeLogHandlers() {
		try {
			for (LogHandler logHandler : _logHandlers) {
				LoggerContextFactory factory = LogManager.getFactory();
				ContextSelector selector = ((Log4jContextFactory) factory).getSelector();
				for (LoggerContext ctx : selector.getLoggerContexts()) {
					_log4j2Helper.removeLogHandler(logHandler, ctx);
				}
			}
		} finally {
			_logHandlers.clear();
		}
	}

}
