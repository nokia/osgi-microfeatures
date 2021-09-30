package com.alcatel.as.service.discovery.impl.jmdns;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;

/**
 * handler for JmDNS internal logs.
 * logger is "javax.jmdns.impl.JmDNSImpl"
 */
public class Log4jHandler extends Handler
{
  LogServiceFactory logFactory;

  // TODO improve this logging handler
  
  public Log4jHandler(LogServiceFactory logFactory)
  {
    super();
    this.logFactory = logFactory;
    Logger logger = Logger.getLogger("");
    logger.setLevel(Level.SEVERE);
    logger.addHandler(this);
  }

  @Override
  public void close() throws SecurityException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void flush()
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void publish(LogRecord record)
  {
      /*
    //org.apache.log4j.Logger log4jLogger = org.apache.log4j.Logger.getLogger(record.getLoggerName());
    LogService log4jLogger = logFactory.getLogger(record.getLoggerName());
    
    int level = record.getLevel().intValue();
    
    switch (level) {
    case 1000:
      log4jLogger.error(record.getMessage(), record.getThrown());
      break;

    case 900:
      log4jLogger.warn(record.getMessage(), record.getThrown());
      break;

    case 800:
    case 500:
      if (log4jLogger.isInfoEnabled())
      {
        log4jLogger.info(record.getMessage());
      }
      break;

    default:
      if (log4jLogger.isDebugEnabled())
      {
        log4jLogger.debug(record.getMessage());
      }
      break;
    }
      */

      return;
  }

}
