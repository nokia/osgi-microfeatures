package alcatel.tess.hometop.gateways.tracer;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;
import alcatel.tess.hometop.gateways.utils.ConfigListener;

/**
 * Handler for log events. This interface must be implemented by
 * log handlers that write logs to concrete outputs 
 * (file, syslog, database, etc ...).
 */
public interface Handler extends ConfigListener {
  
  public void init(Config cnf, String appInstance, String name) throws ConfigException;
  
  /**
   * Handle a log and consume it to a concetete output.
   * Concrete handler typically write the log to files, syslog, etc...
   *
   * @return true if the log has been consumed, false if the log
   *	has not been consumed because the handler is in error and is waiting
   *	a little before retrying to log.
   */
  public boolean handleLog(LogEvent le);
  
  /**
   * The <code>flush</code> method flushes log messages to concrete log 
   * handler.
   */
  public void flush();
  
  /**
   * The <code>clear</code> method clears log file.
   */
  public void clear();
  
  /**
   * Close the log handler.
   */
  public void close();
  
  public String getName();
}
