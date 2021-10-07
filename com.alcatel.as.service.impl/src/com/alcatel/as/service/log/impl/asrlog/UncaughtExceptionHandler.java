package com.alcatel.as.service.log.impl.asrlog;

import org.apache.log4j.Logger;

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
	final Logger _log;

	public UncaughtExceptionHandler(Logger log) {
		_log = log;
	}

	public void uncaughtException(Thread t, Throwable e) {
		_log.error("Detected uncaught exception in thread: " + t.getName(), e);
	}
}
