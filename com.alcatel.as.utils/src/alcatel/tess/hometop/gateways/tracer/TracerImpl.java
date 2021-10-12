// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.tracer;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;
import alcatel.tess.hometop.gateways.utils.ConfigListener;
import alcatel.tess.hometop.gateways.utils.Hashtable;

/**
 * The class <code>TracerImpl</code> is a default tracer implementation.
 *
 * @version 1.0
 */
@SuppressWarnings("deprecation")
public class TracerImpl implements Tracer, ConfigListener {
  
  public TracerImpl() {
  }
  
  public TracerImpl(String name) {
    this.cnf = TracerManager.getConfig();
    ;
    this.name = name;
    this.box = TracerManager.getTracerBox();
    this.facility = -1;
    
    try {
      propertyChanged(cnf, new String[] { "tracer." + name + ".insertHeader",
          "tracer." + name + ".filtering", "tracer." + name + ".level", "tracer." + name + ".stackTrace",
          "tracer." + name + ".applyAllHandlers", "tracer.autoFlush", });
    }
    
    catch (ConfigException e) {
    }
  }
  
  public TracerImpl(Config cnf, String name, TracerBox box) throws ConfigException {
    this.cnf = cnf;
    this.name = name;
    this.box = box;
    this.facility = -1;
    
    propertyChanged(cnf, new String[] { "tracer." + name + ".insertHeader", "tracer." + name + ".filtering",
        "tracer." + name + ".level", "tracer." + name + ".handlers", "tracer." + name + ".stackTrace",
        "tracer." + name + ".applyAllHandlers", "tracer.autoFlush", });
  }
  
  boolean doStackTrace() {
    return (this.stackTrace);
  }
  
  boolean doInsertHeader() {
    return (this.insertHeader);
  }
  
  String getApplInstance() {
    return (box.getApplInstance());
  }
  
  int getFacility() {
    return (facility);
  }
  
  // ------------------------- ConfigListener interface ------------------------
  
  /**
   * Reloads changed properties.
   *
   * @param cnf a <code>Config</code> value
   * @param props a <code>String[]</code> value
   */
  public void propertyChanged(Config cnf, String[] props) throws ConfigException {
    for (int i = 0; i < props.length; i++) {
      if (Debug.enabled)
        Debug.p(this, "propertyChanged", props[i]);
      
      if (props[i].equalsIgnoreCase("tracer.autoFlush")) {
        setAutoFlush(cnf.getBoolean("tracer.autoFlush", false));
      } else if (props[i].equalsIgnoreCase("tracer." + name + ".level")) {
        setLevel(TracerManager.getLevel(cnf.getString("tracer." + name + ".level", "DEBUG")));
      } else if (props[i].equalsIgnoreCase("tracer." + name + ".insertHeader")) {
        insertHeader(cnf.getBoolean("tracer." + name + ".insertHeader", true));
      } else if (props[i].equalsIgnoreCase("tracer." + name + ".stackTrace")) {
        setStackTrace(cnf.getBoolean("tracer." + name + ".stackTrace", false));
      } else if (props[i].equalsIgnoreCase("tracer." + name + ".applyAllHandlers")) {
        setApplyAllHandlers(cnf.getBoolean("tracer." + name + ".applyAllHandlers", true));
      } else if (props[i].equalsIgnoreCase("tracer." + name + ".filtering")) {
        setFiltering(cnf.getBoolean("tracer." + name + ".filtering", true));
      } else if (props[i].equalsIgnoreCase("tracer." + name + ".handlers")) {
        // remove previous handlers.
        myHandlers = null;
        
        String s[] = cnf.getStringArray("tracer." + name + ".handlers", " ,");
        for (int j = 0; j < s.length; j++) {
          if (Debug.enabled)
            Debug.p(this, "propertyChanged", "adding handler " + s[j]);
          
          if (myHandlers == null) {
            myHandlers = new Handler[1];
            myHandlers[0] = box.getHandler(s[j]);
            
            if (myHandlers[0] == null) {
              throw new RuntimeException("failed to load handler " + s[j]);
            }
          } else {
            Handler tmp[] = new Handler[myHandlers.length + 1];
            System.arraycopy(myHandlers, 0, tmp, 0, myHandlers.length);
            tmp[tmp.length - 1] = box.getHandler(s[j]);
            
            if (tmp[tmp.length - 1] == null) {
              throw new RuntimeException("failed to load handler " + s[j]);
            }
            
            myHandlers = tmp;
          }
        }
      }
    }
  }
  
  // ------------------------- Tracer interface --------------------------------
  
  public String getName() {
    return (this.name);
  }
  
  public Logger getLogger() {
    return Logger.getLogger(name != null ? name : "");
  }
  
  public void log(int level, String msg) {
    if (isEnabled(level)) {
      box.handleEvent(BasicLogEvent.acquire(this, level, msg, null, box));
      if (level <= Tracer.LOG_WARN || autoFlush) {
        flush();
      }
    }
  }
  
  public void log(int level, String msg, Throwable t) {
    if (isEnabled(level)) {
      box.handleEvent(BasicLogEvent.acquire(this, level, msg, t, box));
      if (level <= Tracer.LOG_WARN || autoFlush) {
        flush();
      }
    }
  }
  
  public void log(int level, String msg, Throwable t, long d, Thread thread, String ctx) {
    if (isEnabled(level)) {
      box.handleEvent(BasicLogEvent.acquire(this, level, msg, t, d, thread, ctx));
      if (level <= Tracer.LOG_WARN || autoFlush) {
        flush();
      }
    }
  }
  
  public void logRaw(int level, String msg, Throwable t) {
    if (isEnabled(level)) {
      box.handleEvent(BasicLogEvent.acquire(this, level, msg, t));
      if (level <= Tracer.LOG_WARN || autoFlush) {
        flush();
      }
    }
  }
  
  public void logErr(String msg) {
    if (isEnabled(LOG_ERR)) {
      box.handleEvent(BasicLogEvent.acquire(this, LOG_ERR, msg, null, box));
      flush();
    }
  }
  
  public void logErr(String msg, Throwable t) {
    if (isEnabled(LOG_ERR)) {
      box.handleEvent(BasicLogEvent.acquire(this, LOG_ERR, msg, t, box));
      flush();
    }
  }
  
  public void logWarn(String msg) {
    if (isEnabled(LOG_WARN)) {
      box.handleEvent(BasicLogEvent.acquire(this, LOG_WARN, msg, null, box));
      flush();
    }
  }
  
  public void logWarn(String msg, Throwable t) {
    if (isEnabled(LOG_WARN)) {
      box.handleEvent(BasicLogEvent.acquire(this, LOG_WARN, msg, t, box));
      flush();
    }
  }
  
  public void logNotice(String msg) {
    if (isEnabled(LOG_NOTICE)) {
      box.handleEvent(BasicLogEvent.acquire(this, LOG_NOTICE, msg, null, box));
      if (autoFlush) {
        flush();
      }
    }
  }
  
  public void logNotice(String msg, Throwable t) {
    if (isEnabled(LOG_NOTICE)) {
      box.handleEvent(BasicLogEvent.acquire(this, LOG_NOTICE, msg, t, box));
      if (autoFlush) {
        flush();
      }
    }
  }
  
  public void logInfo(String msg) {
    if (isEnabled(LOG_INFO)) {
      box.handleEvent(BasicLogEvent.acquire(this, LOG_INFO, msg, null, box));
      if (autoFlush) {
        flush();
      }
    }
  }
  
  public void logInfo(String msg, Throwable t) {
    if (isEnabled(LOG_INFO)) {
      box.handleEvent(BasicLogEvent.acquire(this, LOG_INFO, msg, t, box));
      if (autoFlush) {
        flush();
      }
    }
  }
  
  public void logDebug(String msg) {
    if (isEnabled(LOG_DEBUG)) {
      box.handleEvent(BasicLogEvent.acquire(this, LOG_DEBUG, msg, null, box));
      if (autoFlush) {
        flush();
      }
    }
  }
  
  public void logDebug(String msg, Throwable t) {
    if (isEnabled(LOG_DEBUG)) {
      box.handleEvent(BasicLogEvent.acquire(this, LOG_DEBUG, msg, t, box));
      if (autoFlush) {
        flush();
      }
    }
  }
  
  @SuppressWarnings("deprecation")
  public void logAll(String msg) {
    if (isEnabled(LOG_DEBUG)) {
      box.handleEvent(BasicLogEvent.acquire(this, LOG_DEBUG, msg, null, box));
      if (autoFlush) {
        flush();
      }
    }
  }
  
  @SuppressWarnings("deprecation")
  public void logAll(String msg, Throwable t) {
    if (isEnabled(LOG_DEBUG)) {
      box.handleEvent(BasicLogEvent.acquire(this, LOG_DEBUG, msg, t, box));
      if (autoFlush) {
        flush();
      }
    }
  }
  
  public void logAccess(int level, String userAddr, String userAuth, String method, String url,
                        int returnCode, int sizeSent, String referer, String browser) {
    logAccess(level, userAddr, null, userAuth, method, url, returnCode, sizeSent, referer, browser);
  }
  
  public void logAccess(int level, String userAddr, String logName, String userAuth, String method,
                        String url, int returnCode, int sizeSent, String referer, String browser) {
    if (isEnabled(level)) {
      box.handleEvent(AccessLogEvent.acquire(this, level, userAddr, logName, userAuth, method, url,
                                             returnCode, sizeSent, referer, browser));
      if (autoFlush) {
        flush();
      }
    }
  }
  
  public void flush() {
    FlushEvent evt = FlushEvent.acquire(this, FlushEvent.FLUSH);
    box.handleEvent(evt);
  }
  
  public void clear() {
    FlushEvent evt = FlushEvent.acquire(this, FlushEvent.CLEAR);
    box.handleEvent(evt);
  }
  
  public boolean isEnabled(int level) {
    //
    // Are we configured to log such a level ?
    //
    boolean enabled = (this.level >= level);
    
    //
    // If so, check if the context stored in the tracing thread
    // is filtered or not.
    // 
    if (enabled == true && this.filtering == true) {
      enabled = box.isContextEnabled();
    }
    
    return (enabled);
  }
  
  public void setFiltering(boolean filtering) {
    this.filtering = filtering;
  }
  
  public void insertHeader(boolean insertHeader) {
    this.insertHeader = insertHeader;
  }
  
  public void setLevel(int level) {
    this.level = level;
  }
  
  public void setContext(String ctx) {
    box.setContext(ctx);
  }
  
  public void removeContext() {
    box.removeContext();
  }
  
  public void setStackTrace(boolean stackTrace) {
    this.stackTrace = stackTrace;
  }
  
  public void setApplyAllHandlers(boolean applyAllHandlers) {
    this.applyAllHandlers = applyAllHandlers;
  }
  
  public void setHandlers(Handler[] handlers, boolean applyAllHandlers) {
    this.myHandlers = handlers;
    this.applyAllHandlers = applyAllHandlers;
  }
  
  public Handler[] getHandlers() {
    return (myHandlers);
  }
  
  public void setFacility(int facility) {
    this.facility = facility;
  }
  
  public void setAutoFlush(boolean autoFlush) {
    this.autoFlush = autoFlush;
  }
  
  void handleEvent(LogEvent evt) {
    boolean oneHandlerSucceded = false, ok;
    Handler[] myHandlers = this.myHandlers;
    
    if (evt instanceof FlushEvent) {
      if ((((FlushEvent) evt).getFlag() & FlushEvent.FLUSH) != 0) {
        for (int i = 0; i < myHandlers.length; i++) {
          myHandlers[i].flush();
        }
      } else if ((((FlushEvent) evt).getFlag() & FlushEvent.CLEAR) != 0) {
        // Before calling all clear's method handlers manually erase all log files.
        FileHandler.clearAllFiles(box.getLogDir(), box.getApplInstance());
        
        // Now, call all current active clear's method handlers.
        for (int i = 0; i < myHandlers.length; i++) {
          myHandlers[i].clear();
        }
      }
    } else {
      for (int i = 0; i < myHandlers.length; i++) {
        if (ok = myHandlers[i].handleLog(evt)) {
          oneHandlerSucceded = true;
        }
        
        if (ok == true && !applyAllHandlers) {
          break;
        }
      }
      
      if (!oneHandlerSucceded) {
        TracerBox.err.println("Tracer failed to handle the following log: " + evt.toString());
      }
    }
  }
  
  protected String name;
  protected Hashtable handlers;
  protected Handler myHandlers[];
  protected boolean applyAllHandlers;
  protected Config cnf;
  protected boolean stackTrace;
  protected int level;
  protected boolean filtering;
  protected boolean insertHeader;
  protected int facility;
  protected boolean autoFlush;
  protected TracerBox box;
}
