package com.alcatel_lucent.as.service.jetty.common.utils;

import java.util.function.Supplier;

import org.apache.log4j.Logger;

public class Log {

	private final Logger _log;
	
	public Log(String logger) {
		_log = Logger.getLogger(logger);
	}
	
	public Log(Class<?> logger) {
		_log = Logger.getLogger(logger);
	}
	
	public void warn(Supplier<String> msg) {
		_log.warn(msg.get());
	}
	
	public void warn(Supplier<String> msg, Throwable t) {
		_log.warn(msg.get(), t);
	}

	public boolean info() {
		return _log.isInfoEnabled();
	}
	
	public void info(Supplier<String> msg) {
		if (_log.isInfoEnabled())
			_log.info(msg.get());
	}
	
	public void info(Supplier<String> msg, Throwable t) {
		if (_log.isInfoEnabled())
			_log.info(msg.get(), t);
	}

	public boolean debug() {
		return _log.isDebugEnabled();
	}

	public void debug(Supplier<String> msg) {
		if (_log.isDebugEnabled())
			_log.debug(msg.get());
	}
	
	public void debug(Supplier<String> msg, Throwable t) {
		if (_log.isDebugEnabled())
			_log.debug(msg.get(), t);
	}
	
	public void debug(String msg) {
		if (_log.isDebugEnabled())
			_log.debug(msg);
	}
	
}
