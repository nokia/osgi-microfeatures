package alcatel.tess.hometop.gateways.tracer;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;

/**
 * The class <code>Log4jTracer</code> is a default tracer implementation.
 *
 * @author <a href="mailto:pierre@nx0047.nextenso.alcatel.fr">Pierre de Rop</a>
 * @version 1.0
 */
public class Log4jTracer extends TracerImpl {
  
  public Log4jTracer(Logger logger) {
    this.logger = logger;
    this.box = TracerManager.getTracerBox();
  }
  
  // ------------------------- Tracer interface --------------------------------
  
  public String getName() {
    return (this.logger.getName());
  }
  
  public Logger getLogger() {
    return logger;
  }
  
  public void log(int level, String msg) {
    logger.log(LOG4J_LEVEL[level], msg);
  }
  
  public void log(int level, String msg, Throwable t) {
    logger.log(LOG4J_LEVEL[level], msg, t);
  }
  
  public void log(int level, String msg, Throwable t, long d, Thread thread, String ctx) {
    logger.log(LOG4J_LEVEL[level], msg, t);
  }
  
  public void logRaw(int level, String msg, Throwable t) {
    logger.log(LOG4J_LEVEL[level], msg, t);
  }
  
  public void logErr(String msg) {
    logger.log(LOG4J_LEVEL[LOG_ERR], msg);
  }
  
  public void logErr(String msg, Throwable t) {
    logger.log(LOG4J_LEVEL[LOG_ERR], msg, t);
  }
  
  public void logWarn(String msg) {
    logger.log(LOG4J_LEVEL[LOG_WARN], msg);
  }
  
  public void logWarn(String msg, Throwable t) {
    logger.log(LOG4J_LEVEL[LOG_WARN], msg, t);
  }
  
  public void logNotice(String msg) {
    logger.log(LOG4J_LEVEL[LOG_NOTICE], msg);
  }
  
  public void logNotice(String msg, Throwable t) {
    logger.log(LOG4J_LEVEL[LOG_NOTICE], msg, t);
  }
  
  public void logInfo(String msg) {
    logger.log(LOG4J_LEVEL[LOG_INFO], msg);
  }
  
  public void logInfo(String msg, Throwable t) {
    logger.log(LOG4J_LEVEL[LOG_INFO], msg, t);
  }
  
  public void logDebug(String msg) {
    logger.log(LOG4J_LEVEL[LOG_DEBUG], msg);
  }
  
  public void logDebug(String msg, Throwable t) {
    logger.log(LOG4J_LEVEL[LOG_DEBUG], msg, t);
  }
  
  @SuppressWarnings("deprecation")
  public void logAll(String msg) {
    logger.log(LOG4J_LEVEL[LOG_ALL], msg);
  }
  
  @SuppressWarnings("deprecation")
  public void logAll(String msg, Throwable t) {
    logger.log(LOG4J_LEVEL[LOG_ALL], msg, t);
  }
  
  public void logAccess(int level, String userAddr, String userAuth, String method, String url,
                        int returnCode, int sizeSent, String referer, String browser) {
    logAccess(level, userAddr, null, userAuth, method, url, returnCode, sizeSent, referer, browser);
  }
  
  public void logAccess(int level, String userAddr, String logName, String userAuth, String method,
                        String url, int returnCode, int sizeSent, String referer, String browser) {
    if (isEnabled(level)) {
      AccessLogEvent event = AccessLogEvent.acquire(this, level, userAddr, logName, userAuth, method, url,
                                                    returnCode, sizeSent, referer, browser);
      try {
        logger.log(LOG4J_LEVEL[level], event.toString());
      }
      
      finally {
        event.release();
      }
    }
  }
  
  public void flush() {
  }
  
  public void clear() {
  }
  
  public boolean isEnabled(int level) {
    boolean enabled = logger.isEnabledFor(LOG4J_LEVEL[level]);
    return (enabled);
  }
  
  public void setFiltering(boolean filtering) {
  }
  
  public void insertHeader(boolean insertHeader) {
  }
  
  public void setLevel(int level) {
    logger.setLevel(LOG4J_LEVEL[level]);
  }
  
  public void setContext(String ctx) {
    NDC.push(ctx);
  }
  
  public void removeContext() {
    NDC.remove();
  }
  
  public void setStackTrace(boolean stackTrace) {
  }
  
  public void setApplyAllHandlers(boolean applyAllHandlers) {
  }
  
  public void setHandlers(Handler[] handlers, boolean applyAllHandlers) {
  }
  
  public Handler[] getHandlers() {
    return null;
  }
  
  public void setFacility(int facility) {
  }
  
  public void setAutoFlush(boolean autoFlush) {
  }
  
  // --- TracerImpl overriden methods ---
  
  public void propertyChanged(Config cnf, String[] props) throws ConfigException {
  }
  
  void handleEvent(LogEvent evt) {
  }
  
  private Logger logger;
  private TracerBox box;
  
  private final static org.apache.log4j.Level[] LOG4J_LEVEL = { org.apache.log4j.Level.FATAL,
      org.apache.log4j.Level.FATAL, org.apache.log4j.Level.FATAL, org.apache.log4j.Level.FATAL, // old LOG_ERR=3
      org.apache.log4j.Level.WARN, // old LOG_WARN=4
      org.apache.log4j.Level.INFO, // old LOG_NOTICE=5
      org.apache.log4j.Level.INFO, // old LOG_INFO=6
      org.apache.log4j.Level.DEBUG, // old LOG_DEBUG=7
      org.apache.log4j.Level.ALL, // old LOG_ALL=8
  };
}
