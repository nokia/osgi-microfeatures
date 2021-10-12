// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.log.admin.impl.pax;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.ops4j.pax.logging.PaxLoggingService;
import org.osgi.framework.BundleContext;

import com.nokia.as.log.admin.impl.log4j2helper.Log4j2Helper;
import com.nokia.as.log.service.admin.LogAdmin;
import com.nokia.as.log.service.admin.LogHandler;

/**
 * Catch all logs and invoke log listeners.
 */
public class PaxLogAdminImpl implements LogAdmin {

	private final BundleContext _bc;
	private final Log4j2Helper _log4j2Helper = new Log4j2Helper();
	private final Set<LogHandler> _logHandlers = Collections.synchronizedSet(new HashSet<>());

	public PaxLogAdminImpl(BundleContext bc) {
		_bc = bc;
	}

	@Override
	public void setLevel(String loggerName, String lev) {
		PaxLoggingService pls = _bc.getService(_bc.getServiceReference(PaxLoggingService.class));
		org.apache.logging.log4j.core.LoggerContext ctx = getLoggerContext(pls);
		_log4j2Helper.setLevel(loggerName, lev, ctx);
	}

	@Override
	public String getLevel(String loggerName) {
		return _log4j2Helper.getLevel(loggerName);
	}

	void addLogHandler(LogHandler logHandler, boolean format) {
		PaxLoggingService pls = _bc.getService(_bc.getServiceReference(PaxLoggingService.class));
		org.apache.logging.log4j.core.LoggerContext ctx = getLoggerContext(pls);
		_log4j2Helper.addLogHandler(logHandler, ctx, format);
		_logHandlers.add(logHandler);
	}

	void removeLogHandler(LogHandler logHandler) {
		Objects.nonNull(logHandler);
		if (_logHandlers.remove(logHandler)) {
			PaxLoggingService pls = _bc.getService(_bc.getServiceReference(PaxLoggingService.class));
			org.apache.logging.log4j.core.LoggerContext ctx = getLoggerContext(pls);
			_log4j2Helper.removeLogHandler(logHandler, ctx);
		}
	}

	void removeLogHandlers() {
		try {
			PaxLoggingService pls = _bc.getService(_bc.getServiceReference(PaxLoggingService.class));
			org.apache.logging.log4j.core.LoggerContext ctx = getLoggerContext(pls);
			for (LogHandler logHandler : _logHandlers) {
				_log4j2Helper.removeLogHandler(logHandler, ctx);
			}
		} finally {
			_logHandlers.clear();
		}
	}

	/**
	 * Hack the m_log4jContext from the PaxLoggingServiceImpl service (take care,
	 * the pax logging service is an inner class ...)
	 */
	private org.apache.logging.log4j.core.LoggerContext getLoggerContext(PaxLoggingService pls) {
		try {
			Field outerClassField = pls.getClass().getDeclaredField("this$0");
			outerClassField.setAccessible(true);
			PaxLoggingService outer = (PaxLoggingService) outerClassField.get(pls);

			Field log4jContextField = outer.getClass().getDeclaredField("m_log4jContext");
			log4jContextField.setAccessible(true);
			org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) log4jContextField
					.get(outer);
			return ctx;
		} catch (Exception e) {
			throw new RuntimeException("Can't obtain log4j2 logger context from pax logging service", e);
		}
	}


}
