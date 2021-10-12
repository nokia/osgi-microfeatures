// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.alcatel.as.service.log.LogService;

/**
 * This class logs some formattable strings into log4j, without using isXXEnabled methods ...
 */
public class Log implements LogService {
  private final Logger _logger;
  
  public Log(Logger logger) {
    _logger = logger;
  }
  
  public Log(String loggerName) {
    _logger = Logger.getLogger(loggerName);
  }
  
  public void log(Level level, String format, Object ... args) {
    if (_logger.isEnabledFor(level)) {
      _logger.log(level, format(format, args));
    }
  }

  public void log(Level level, String format, Throwable t, Object ... args) {
    if (_logger.isEnabledFor(level)) {
      _logger.log(level, format(format, args), t);
    }
  }
  
  public void error(String format, Object ... args) {
    if (_logger.isEnabledFor(Level.ERROR)) {
      _logger.error(format(format, args));
    }
  }
  
  public void error(String format, Throwable t, Object ... args) {
    if (_logger.isEnabledFor(Level.ERROR)) {
      _logger.error(format(format, args), t);
    }
  }
  
  public void warn(String format, Object ... args) {
    if (_logger.isEnabledFor(Level.WARN)) {
      _logger.warn(format(format, args));
    }
  }
  
  public void warn(String format, Throwable t, Object ... args) {
    if (_logger.isEnabledFor(Level.WARN)) {
      _logger.warn(format(format, args), t);
    }
  }
  
  public void info(String format, Object ... args) {
    if (_logger.isEnabledFor(Level.INFO)) {
      _logger.info(format(format, args));
    }
  }
  
  public void info(String format, Throwable t, Object ... args) {
    if (_logger.isEnabledFor(Level.INFO)) {
      _logger.info(format(format, args), t);
    }
  }
  
  public void debug(String format, Object ... args) {
    if (_logger.isEnabledFor(Level.DEBUG)) {
      _logger.debug(format(format, args));
    }
  }
  
  public void debug(String format, Throwable t, Object ... args) {
    if (_logger.isEnabledFor(Level.DEBUG)) {
      _logger.debug(format(format, args), t);
    }
  }
  
  public void trace(String format, Object ... args) {
	if (_logger.isEnabledFor(Level.TRACE)) {
	  _logger.trace(format(format, args));
	}
  }
	  
  public void trace(String format, Throwable t, Object ... args) {
	if (_logger.isEnabledFor(Level.TRACE)) {
	  _logger.trace(format(format, args), t);
	}
  }
  
  public boolean isDebugEnabled() {
    return _logger.isDebugEnabled();
  }
  
  public boolean isEnabledFor(Priority level) {
    return _logger.isEnabledFor(level);
  }
  
  public boolean isInfoEnabled() {
    return _logger.isInfoEnabled();
  }
  
  public boolean isTraceEnabled() {
    return _logger.isTraceEnabled();
  }
  
  public Logger getLogger() {
    return _logger;
  }
  
  public static Log getLogger(String name) {
    return new Log(name);
  }
  
  public static Log getLogger(Logger logger) {
    return new Log(logger);
  }
  
  public static Log getLogger(Class<?> clazz) {
    return new Log(Logger.getLogger(clazz));
  }
  
  @Override
  public String toString() {
      return _logger.getName();
  }
  
  private String format(String format, Object ... args) {
	  if (args == null || args.length == 0) {
		  return format;
	  } else {
		  try {
			  return String.format(format,  args);
		  } catch (Exception e) {
			  // probably the args don't contain expected parameters
			  return format + " - " + e.toString();
		  }
	  }
  }
}
