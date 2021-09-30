package alcatel.tess.hometop.gateways.tracer;

import org.apache.log4j.Logger;

/**
 * The interface <code>Tracer</code> regroups methods used to
 * generate log message. It also includes methods used to
 * override default configuration taken from properties.
 *
 * @author <a href="mailto:pierre@nx0047.nextenso.alcatel.fr">Pierre de Rop</a>
 * @version 1.1
 */
public interface Tracer extends Level {
  
  /**
   * Returns this tracer's name.
   *
   * @return the tracer name
   */
  public String getName();
  
  /**
   * Return a Log4j logger linked to this tracer.
   */
  public Logger getLogger();
  
  /**
   * Logs a message with a specified level.
   *
   * @param msg a <code>String</code> value describing the err log
   */
  public void log(int level, String msg);
  
  /**
   * Logs a message with a specified level.
   *
   * @param msg a <code>String</code> value describing the err log
   */
  public void log(int level, String msg, Throwable t);
  
  /**
   * Logs a message with a specified level. 
   *
   * @param msg a <code>String</code> value describing the err log
   */
  public void log(int level, String msg, Throwable t, long d, Thread thread, String ctx);
  
  /**
   * Logs a message with a specified level. No formatting will be
   * performed.
   *
   * @param msg a <code>String</code> value describing the err log
   */
  public void logRaw(int level, String msg, Throwable t);
  
  /**
   * Logs an emergency err error log message.
   *
   * @param msg a <code>String</code> value describing the err log
   */
  public void logErr(String msg);
  
  /**
   * Logs an emergency err error log message, with an exception.
   * @param msg a <code>String</code> value describing the err log
   * @param t a <code>Throwable</code> value describing the err log 
   *		exception
   */
  public void logErr(String msg, Throwable t);
  
  /**
   * Logs an warning log message. warning message should match
   * recoverable errors (errors that are not fatal to the running
   * application).
   *
   * @param msg a <code>String</code> value describing the warning log
   */
  public void logWarn(String msg);
  
  /**
   * Logs an warning log message with an exception. 
   *
   * @param msg a <code>String</code> value describing the warning log
   * @param t a <code>Throwable</code> value
   *
   * @see #logWarn(String)
   */
  public void logWarn(String msg, Throwable t);
  
  /**
   * Logs an informational log message that contains noticeable informations.
   *
   * @param msg a <code>String</code> value describing the notice log
   */
  public void logNotice(String msg);
  
  public void logNotice(String msg, Throwable t);
  
  /**
   * Logs an informational message that contains a finer notice message.
   *
   * @param msg a <code>String</code> value describing the info log
   */
  public void logInfo(String msg);
  
  public void logInfo(String msg, Throwable t);
  
  /**
   * Logs a debug message.
   *
   * @param msg a <code>String</code> value describing the debug log
   */
  public void logDebug(String msg);
  
  public void logDebug(String msg, Throwable t);
  
  /**
   * Logs a low level debug message.
   *
   * @param msg a <code>String</code> value describing the fine debug log
   *
   * @deprecated This method was wrongly assuming that there was an existing
   * LOG_ALL (8) syslog priority, but actually syslog priorities goes up to 7 (LOG_DEBUG).
   * Please use LOG_DEBUG instead of LOG_ALL.
   */
  public void logAll(String msg);
  
  /**
   * Logs a low level debug message.
   *
   * @param msg a <code>String</code> value describing the fine debug log
   * @param t a <code>Throwable</code> value describing a java exception
   *
   * @deprecated This method was wrongly assuming that there was an existing
   * LOG_ALL (8) syslog priority, but actually syslog priorities goes up to 7 (LOG_DEBUG).
   * Please use LOG_DEBUG instead of LOG_ALL.
   */
  public void logAll(String msg, Throwable t);
  
  /**
   * Logs an apache like access log message.
   *
   * @param level an <code>int</code> value corresponding to a log level.
   * @param userAddr a <code>String</code> value representing the http 
   *		       request user address (like its ip address, or a phone
   *		       number if the logs is emitted by an sms gateway)
   * @param userAuth a <code>String</code> value giving the user password 
   *		       http authentication information (null if not kwown)
   * @param method a <code>String</code> value giving the http request method
   * @param url a <code>String</code> value giving the http request url
   * @param returnCode an <code>int</code> value giving the http response 
   *			 code
   * @param sizeSent an <code>int</code> value giving the size of the http 
   *		       response returned to the http requester
   * @param referer a <code>String</code> value the http referer (null if not
   *		      known)
   * @param browser a <code>String</code> value the value of the user-agent 
   *		      http request header
   */
  public void logAccess(int l, String userAddr, String userAuth, String method, String url, int returnCode,
                        int sizeSent, String referer, String browser);
  
  /**
   * Logs an apache like access log message with logname.
   *
   * @param level an <code>int</code> value corresponding to a log level.
   * @param userAddr a <code>String</code> value representing the http 
   *		       request user address (like its ip address, or a phone
   *		       number if the logs is emitted by an sms gateway)
   * @param logName a <code>String</code> value representing the client making the HTTP request. 
   *		    A "-" value is used to indicate no logname present
   * @param userAuth a <code>String</code> value giving the user password 
   *		       http authentication information (null if not kwown)
   * @param method a <code>String</code> value giving the http request method
   * @param url a <code>String</code> value giving the http request url
   * @param returnCode an <code>int</code> value giving the http response 
   *			 code
   * @param sizeSent an <code>int</code> value giving the size of the http 
   *		       response returned to the http requester
   * @param referer a <code>String</code> value the http referer (null if not
   *		      known)
   * @param browser a <code>String</code> value the value of the user-agent 
   *		      http request header
   */
  public void logAccess(int l, String userAddr, String logName, String userAuth, String method, String url,
                        int returnCode, int sizeSent, String referer, String browser);
  
  /**
   * Flushes all pending log messages.
   */
  public void flush();
  
  /**
   * Clears all log handlers.
   */
  public void clear();
  
  /**
   * Sets a per-thread log context. This method should be used when your 
   * application is serving multiple clients in separate threads. It will
   * store a client context description for each client thread and that 
   * information will be added in all log message concerned by that client.
   *
   * @param ctx a <code>String</code> value representing the user client 
   *		  description (typically  the client ip address, a mobile
   *		  phone number, etc ...)
   */
  public void setContext(String ctx);
  
  /**
   * Removes the per-thread log context.
   *
   */
  public void removeContext();
  
  /**
   * Tells if a specific log level is enabled or not.
   *
   * @param level an <code>int</code> value matching one of the log levels
   * @return a <code>boolean</code> true value if the level is enabled, or
   *	       false if not.
   *
   * @see #LOG_EMERG
   * @see #LOG_WARN
   * @see #LOG_NOTICE
   * @see #LOG_INFO
   * @see #LOG_DEBUG
   * @see #LOG_ALL
   */
  public boolean isEnabled(int level);
  
  /**
   * Activates a specific log level. Enabling a log level implicitly enables
   * upper log level. For example, if you enable the LOG_INFO level, then
   * the LOG_EMERG, LOG_WARN, LOG_NOTICE levels will also be activated.
   *
   * @param level an <code>int</code> value
   *
   * @see #LOG_EMERG
   * @see #LOG_WARN
   * @see #LOG_NOTICE
   * @see #LOG_INFO
   * @see #LOG_DEBUG
   * @see #LOG_ALL
   */
  public void setLevel(int level);
  
  /**
   * Describe <code>setFilteredLevel</code> method here.
   *
   * @param level an <code>int</code> value
   */
  public void setFiltering(boolean filtering);
  
  /**
   * Describe <code>insertHeader</code> method here.
   *
   * @param insertHeader a <code>boolean</code> value
   */
  public void insertHeader(boolean insertHeader);
  
  /**
   * The <code>setStackTrace</code> method turns on autostack trace mode.
   *
   * @param stackTrace a <code>boolean</code> value
   */
  public void setStackTrace(boolean stackTrace);
  
  /**
   * The <code>setApplyAllHandlers</code> method is used to configure the
   * tracer so that it will call all log handlers or not
   *
   * @param stackTrace a <code>boolean</code> value set to true if all log
   * handlers must be called, false, if only the first log handler must be
   * called. In the last case, only next handler is called if the previous 
   * handler has failed.
   */
  public void setApplyAllHandlers(boolean stackTrace);
  
  public void setHandlers(Handler[] handlers, boolean applyAllHandler);
  
  public Handler[] getHandlers();
  
  /**
   * The <code>setFacility</code> method set the syslog facility
   * used for that logger. 
   *
   * @param facility an <code>int</code> value which must have the
   * following values:<p>
   * <ul>
   * <li> 0 (Syslog.KERN)
   * <li> 1 (Syslog.USER)
   * <li> 2 (Syslog.MAIL)
   * <li> 3 (Syslog.DAEMON)
   * <li> 4 (Syslog.AUTH)
   * <li> 5 (Syslog.SYSLOG)
   * <li> 6 (Syslog.LPR)
   * <li> 7 (Syslog.NEWS)
   * <li> 8 (Syslog.UUCP)
   * <li> 9 (Syslog.CRON)
   * <li> 16 (Syslog.LOCAL0)
   * <li> 17 (Syslog.LOCAL1)
   * <li> 18 (Syslog.LOCAL2)
   * <li> 19 (Syslog.LOCAL3)
   * <li> 20 (Syslog.LOCAL4)
   * <li> 21 (Syslog.LOCAL5)
   * <li> 22 (Syslog.LOCAL6)
   * <li> 23 (Syslog.LOCAL7)
   * </ul>
   * @see #alcatel.tess.hometop.gateways.tracer.Syslog
   */
  public void setFacility(int facility);
  
  /**
   * The <code>setAutoFlush</code> method turns on/off
   * autoflushing for that tracer.
   *
   * @param autoFlush a <code>boolean</code> value
   */
  public void setAutoFlush(boolean autoFlush);
}
