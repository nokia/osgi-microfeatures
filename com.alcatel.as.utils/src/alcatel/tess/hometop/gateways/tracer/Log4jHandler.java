// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.tracer;

import java.io.IOException;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;

/**
 * File Handler for log events.
 *
 */
public class Log4jHandler implements Handler {
  
  /**
   * Creates a new <code>Log4jHandler</code> instance.
   *
   * @param params an <code>Object[]</code> value
   * @exception Exception if an error occurs
   */
  public Log4jHandler() {
  }
  
  public Log4jHandler(String name, Logger logger) {
    this.name = name;
    this.logger = logger;
    this.applInstance = TracerManager.getApplInstance();
  }
  
  public void init(Config cnf, String applInstance, String name) throws ConfigException {
    this.applInstance = applInstance.replace(' ', '_');
    this.name = name;
    
    propertyChanged(cnf, new String[] { "tracer.log4jLogger", });
  }
  
  /**
   * Reloads changed properties.
   *
   * @param cnf a <code>Config</code> value
   * @param props a <code>String[]</code> value
   */
  public void propertyChanged(Config cnf, String[] props) throws ConfigException {
    for (int i = 0; i < props.length; i++) {
      if (Debug.enabled)
        Debug.p(this, "propertyChanged", "property=" + props[i]);
      
      if (props[i].equalsIgnoreCase("tracer.log4jLogger")) {
        this.logger = Logger.getLogger(cnf.getString("tracer.log4jLogger"));
        continue;
      }
    }
  }
  
  /**
   * Handles a log and redirect it to a file.
   *
   * @param le a <code>LogEvent</code> value
   * @return a <code>boolean</code> value
   * @exception IOException if an error occurs
   */
  public boolean handleLog(LogEvent le) {
    try {
      switch (le.getLevel()) {
      case Level.LOG_ERR:
        this.logger.fatal(le.toString());
        break;
      
      case Level.LOG_WARN:
        this.logger.warn(le.toString());
        break;
      
      case Level.LOG_NOTICE:
        this.logger.warn(le.toString());
        break;
      
      case Level.LOG_INFO:
        this.logger.info(le.toString());
        break;
      
      case Level.LOG_DEBUG:
        this.logger.debug(le.toString());
        break;
      }
    }
    
    catch (Throwable t) {
      t.printStackTrace(TracerBox.err);
      return (false);
    }
    
    return (true);
  }
  
  /**
   * Describe <code>flush</code> method here.
   *
   * @param info an <code>int</code> value
   * @exception IOException if an error occurs
   */
  public void flush() {
  }
  
  /**
   * Describe <code>flush</code> method here.
   *
   * @param info an <code>int</code> value
   * @exception IOException if an error occurs
   */
  public void clear() {
  }
  
  /**
   * Describe <code>close</code> method here.
   *
   */
  public void close() {
  }
  
  /**
   * Describe <code>toString</code> method here.
   *
   * @return a <code>String</code> value
   */
  public String toString() {
    return ("[Log4jHandler: logger=" + logger + "]");
  }
  
  public String getName() {
    return (this.name);
  }
  
  private String applInstance;
  private String name;
  private Logger logger;
}
