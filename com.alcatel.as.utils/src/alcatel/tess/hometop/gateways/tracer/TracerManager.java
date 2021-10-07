package alcatel.tess.hometop.gateways.tracer;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;

/**
 * The class <code>TracerManager</code> manages all set of tracers
 * configured from the Config.
 *
 */
public class TracerManager {
  
  /**
   * Log4j wrapper initialization.
   */
  public static void wrapLog4j(Logger defLogger, String applInstance) throws ConfigException {
    TracerManager.turnOffStdoutRedirection();
    
    Config cnf = new Config();
    cnf.setProperty("tracer.defaultTracer", defLogger.getName());
    cnf.setProperty("tracer.redirectStdout", "false");
    
    synchronized (TracerManager.class) {
      if (box == null) {
        box = new TracerBox(cnf, applInstance);
      } else {
        box.init(cnf, applInstance);
      }
    }
    
    TracerManager.addTracer(new Log4jTracer(defLogger));
  }
  
  /**
   * Initialized all tracers from configuration.
   *
   * @param cnf a <code>Config</code> value
   * @param applInstance a <code>String</code> value
   * @exception ConfigException if an error occurs
   */
  public static void init(Config cnf, String applInstance) throws ConfigException {
    synchronized (TracerManager.class) {
      if (box == null) {
        box = new TracerBox(cnf, applInstance);
      } else {
        box.init(cnf, applInstance);
      }
    }
  }
  
  public static void setThreadTracer(Tracer t) {
    _currentThreadTracer.set(t);
  }
  
  public static void turnOffStdoutRedirection() {
    TracerBox.turnOffStdoutRedirection();
  }
  
  public static Tracer getThreadTracer() {
    Tracer tracer = _currentThreadTracer.get();
    if (tracer == null)
      return getTracer();
    return tracer;
  }
  
  public static String getApplInstance() {
    initTracerBox();
    return (box.getApplInstance());
  }
  
  public static String getLogDir() {
    initTracerBox();
    return (box.getLogDir());
  }
  
  public static int getMaxFileSize() {
    initTracerBox();
    return (box.getMaxFileSize());
  }
  
  public static int getMaxZipFiles() {
    initTracerBox();
    return (box.getMaxZipFiles());
  }
  
  /**
   * Gets the default tracer as specified in the property
   * "tracer.tracers.default". Notice that all standard and
   * error outputs are redirected the default tracer (all
   * System.out, System.err messages).
   *
   * @param name a <code>String</code> value corresponding to one
   *	    one tracer from the property "tracer.tracers"
   * @return a <code>Tracer</code> value
   */
  public static Tracer getTracer() {
    initTracerBox();
    return (box.getTracer());
  }
  
  /**
   * Gets a particular named tracer. The name must be one
   * of the names specified in the property tracer.tracers
   *
   * @param name a <code>String</code> value corresponding to one
   *	    one tracer from the property "tracer.tracers"
   * @return a <code>Tracer</code> value
   */
  public static Tracer getTracer(String name) {
    initTracerBox();
    return (box.getTracer(name));
  }
  
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
  public static void setContext(String ctx) {
    initTracerBox();
    box.setContext(ctx);
  }
  
  /**
   * Removes the per-thread logger context.
   */
  public static void removeContext() {
    initTracerBox();
    box.removeContext();
  }
  
  public static String getContext() {
    initTracerBox();
    return (box.getContext());
  }
  
  public static Config getConfig() {
    initTracerBox();
    return (box.getConfig());
  }
  
  public static int getLevel(String name) {
    if (name.equals("ERR")) {
      return (3); // SYSLOG_ERR
    } else if (name.equals("WARN")) {
      return (4); // SYSLOG_WARN
    } else if (name.equals("NOTICE")) {
      return (5); // SYSLOG_NOTICE
    } else if (name.equals("INFO")) {
      return (6); // SYSLOG_INFO
    } else if (name.equals("DEBUG")) {
      return (7); // SYSLOG_DEBUG
    } else if (name.equals("ALL")) {
      return (8); // SYSLOG_ALL
    } else {
      throw new RuntimeException();
    }
  }
  
  public static String getLevel(int level) {
    if (level < Level.LOG_ERR || level > Level.LOG_DEBUG) {
      throw new RuntimeException("Invalid level parameter: " + level);
    }
    
    return (Level.levelStr[level]);
  }
  
  /**
   * The <code>isContextEnabled</code> method checks if the context
   * of the tracing thread is enabled or not to trace.
   *
   * @return a <code>boolean</code> value equals to true if the tracing
   *	thread is able to trace a log, or false if not.
   */
  static boolean isContextEnabled() {
    return (box.isContextEnabled());
  }
  
  public static void reloadEnabledContexts(Config cnf) {
    box.reloadEnabledContexts(cnf);
  }
  
  /**
   * Clear (delete files) all tracers.
   *
   */
  public static void clear() {
    synchronized (TracerManager.class) {
      if (box == null) {
        return;
      }
    }
    
    box.clear();
  }
  
  /**
   * Flush all tracers.
   *
   */
  public static void flush() {
    synchronized (TracerManager.class) {
      if (box == null) {
        return;
      }
    }
    
    box.flush();
  }
  
  /**
   * Stops the tracer.
   * @deprecated This method is no more relevant and should not be used any more,
   * because a tracer shutdown hook will automatically flush all pending logs at
   * exit time.
   */
  public static void shutdown() {
  }
  
  public static void addHandler(Handler h) {
    initTracerBox();
    box.addHandler(h);
  }
  
  public static Handler getHandler(String name) {
    initTracerBox();
    return (box.getHandler(name));
  }
  
  public static Handler getHandler(Class name) {
    initTracerBox();
    return (box.getHandler(name));
  }
  
  public static void addTracer(Tracer t) {
    initTracerBox();
    box.addTracer(t);
  }
  
  static TracerBox getTracerBox() {
    initTracerBox();
    return (box);
  }
  
  /**
   * The <code>initTracerBox</code> method initializes the tracer box that will handle
   * all application tracers.
   *
   */
  private static void initTracerBox() {
    if (box == null) {
      synchronized (TracerManager.class) {
        if (box == null) {
          Config cnf;
          
          try {
            cnf = new Config("Tracer.properties");
          }
          
          catch (ConfigException e) {
            cnf = new Config();
            cnf.setProperty("tracer.handler.stdout.class",
                            "alcatel.tess.hometop.gateways.tracer.StandardHandler");
            cnf.setProperty("tracer.defaultTracer", "msg");
            cnf.setProperty("tracer.autoFlush", "true");
            cnf.setProperty("tracer.msg.level", "WARN");
            cnf.setProperty("tracer.msg.handlers", "stdout");
            cnf.setProperty("tracer.msg.stackTrace", "false");
            cnf.setProperty("tracer.msg.applyAllHandlers", "true");
          }
          
          try {
            box = new TracerBox(cnf, "messages");
          }
          
          catch (ConfigException e) {
            throw new RuntimeException("failed to find Tracer.properties from classpath", e);
          }
        }
      }
    }
  }
  
  /**
   * The variable <code>box</code> is a bridge to the TracerManager implementation.
   *
   */
  private static TracerBox box;
  
  /**
   * Local used to get/set the current thread tracer
   */
  private static ThreadLocal<Tracer> _currentThreadTracer = new ThreadLocal<Tracer>() {
  };
}
