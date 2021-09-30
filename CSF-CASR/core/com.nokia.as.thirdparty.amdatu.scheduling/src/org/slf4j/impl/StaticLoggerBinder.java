/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.slf4j.impl;

import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

/**
 * Provides a bridge between SLF4J and the OSGi LogService. 
 * 
 * Copy of the StaticLoggerBinder from the Amdatu Web project
 */
public class StaticLoggerBinder extends MarkerIgnoringBase implements ILoggerFactory, Logger {
	private static final long serialVersionUID = 1L;
	
	public static final String REQUESTED_API_VERSION = "1.6.1";
	private static final StaticLoggerBinder m_instance = new StaticLoggerBinder();

	// Injected by Felix DM...
	private volatile LogService m_log;
	private volatile BundleContext m_context;
	private volatile boolean m_debugEnabled;

	public static final StaticLoggerBinder getSingleton() {
        return m_instance;
    }

    public static void log(int level, String message, Throwable exception) {
        LogService log = getSingleton().getLog();
        try {
            log.log(level, message, exception);
        }
        catch (NullPointerException e) {
            // we can safely ignore these
        }
    }
	
	@Override
	public void debug(String msg) {
		log(LogService.LOG_DEBUG, msg, null);
	}
	
	@Override
	public void debug(String msg, Object arg1) {
		debug(MessageFormatter.format(msg, arg1).toString());
	}
	
	@Override
	public void debug(String msg, Object arg1, Object arg2) {
		debug(MessageFormatter.format(msg, arg1, arg2).toString());
	}
	
	@Override
	public void debug(String msg, Object[] arg1) {
		debug(MessageFormatter.arrayFormat(msg, arg1).toString());
	}

	@Override
	public void debug(String msg, Throwable ex) {
		log(LogService.LOG_DEBUG, msg, ex);
	}

	@Override
	public void error(String msg) {
		log(LogService.LOG_ERROR, msg, null);
	}

	@Override
	public void error(String msg, Object arg1) {
		error(MessageFormatter.format(msg, arg1).toString());
	}

	@Override
	public void error(String msg, Object arg1, Object arg2) {
		error(MessageFormatter.format(msg, arg1, arg2).toString());
	}

	@Override
	public void error(String msg, Object[] arg1) {
		error(MessageFormatter.arrayFormat(msg, arg1).toString());
	}
	
	@Override
	public void error(String msg, Throwable ex) {
		log(LogService.LOG_ERROR, msg, ex);
	}

	@Override
	public Logger getLogger(String name) {
		return this;
	}

	public ILoggerFactory getLoggerFactory() {
		return this;
	}

	public String getLoggerFactoryClassStr() {
		return getClass().getName();
	}

	@Override
	public String getName() {
		return "OSGi";
	}

	@Override
	public void info(String msg) {
		log(LogService.LOG_INFO, msg, null);
	}

	@Override
	public void info(String msg, Object arg1) {
		info(MessageFormatter.format(msg, arg1).toString());
	}

	@Override
	public void info(String msg, Object arg1, Object arg2) {
		info(MessageFormatter.format(msg, arg1, arg2).toString());
	}

	@Override
	public void info(String msg, Object[] arg1) {
		info(MessageFormatter.arrayFormat(msg, arg1).toString());
	}

	@Override
	public void info(String msg, Throwable ex) {
		log(LogService.LOG_INFO, msg, ex);
	}

	@Override
	public boolean isDebugEnabled() {
		return m_debugEnabled;
	}

	@Override
	public boolean isDebugEnabled(Marker m) {
		return isDebugEnabled();
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public boolean isErrorEnabled(Marker m) {
		return true;
	}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public boolean isInfoEnabled(Marker m) {
		return true;
	}

	@Override
	public boolean isTraceEnabled() {
		return false;
	}

	@Override
	public boolean isTraceEnabled(Marker m) {
		return isTraceEnabled();
	}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}

	@Override
	public boolean isWarnEnabled(Marker m) {
		return true;
	}

	@Override
	public void trace(String msg) {
//		debug("[TRACE] " + msg);
	}

	@Override
	public void trace(String msg, Object arg1) {
//		debug(MessageFormatter.format("[TRACE] " + msg, arg1).toString());
	}

	@Override
	public void trace(String msg, Object arg1, Object arg2) {
//		debug(MessageFormatter.format("[TRACE] " + msg, arg1, arg2).toString());
	}

	@Override
	public void trace(String msg, Object[] arg1) {
//		debug(MessageFormatter.arrayFormat("[TRACE] " + msg, arg1).toString());
	}

	@Override
	public void trace(String msg, Throwable ex) {
//		debug("[TRACE] " + msg, ex);
	}

	@Override
	public void warn(String msg) {
		log(LogService.LOG_WARNING, msg, null);
	}

	@Override
	public void warn(String msg, Object arg1) {
		warn(MessageFormatter.format(msg, arg1).toString());
	}

	@Override
	public void warn(String msg, Object arg1, Object arg2) {
		warn(MessageFormatter.format(msg, arg1, arg2).toString());
	}

	@Override
	public void warn(String msg, Object[] arg1) {
		warn(MessageFormatter.arrayFormat(msg, arg1).toString());
	}

	@Override
	public void warn(String msg, Throwable ex) {
		log(LogService.LOG_WARNING, msg, ex);
	}

	protected void start(Component comp) {
	    m_debugEnabled = Boolean.parseBoolean(m_context.getProperty("org.amdatu.web.rest.debug"));
	}

	private LogService getLog() {
		return m_log;
	}
}
