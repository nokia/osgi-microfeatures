// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.tracer;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;
import alcatel.tess.hometop.gateways.utils.ConfigListener;
import alcatel.tess.hometop.gateways.utils.Hashtable;
import alcatel.tess.hometop.gateways.utils.Utils;

/**
 * The class <code>TracerBox</code> is a tracer container that
 * manages a set of application tracers.
 *
 */
public class TracerBox implements ConfigListener {
  
  /**
   * Creates a new <code>TracerBox</code> instance.
   *
   * @param cnf a <code>Config</code> value
   * @param applInstance a <code>String</code> value
   * @exception ConfigException if an error occurs
   */
  public TracerBox(Config cnf, String applInstance) throws ConfigException {
    this.applInstance = applInstance;
    this.handlers = new Hashtable();
    this.tracers = new Hashtable();
    this.contexts = new Hashtable();
    init(cnf, applInstance);
  }
  
  public void init(Config cnf, String applInstance) throws ConfigException {
    if (this.cnf != null) {
      this.cnf.unregisterListener(this, "tracer.*");
    }
    
    this.cnf = cnf;
    this.applInstance = applInstance;
    
    Enumeration pNames = cnf.propertyNames();
    Vector vNames = new Vector();
    
    while (pNames.hasMoreElements()) {
      vNames.add((String) pNames.nextElement());
    }
    
    propertyChanged(cnf, (String[]) vNames.toArray(new String[0]));
    cnf.registerListener(this, "tracer.*");
  }
  
  public static void turnOffStdoutRedirection() {
    synchronized (LOCK) {
      catchStdout = false;
      System.setOut(stdout);
      System.setErr(stderr);
    }
  }
  
  public Config getConfig() {
    return (this.cnf);
  }
  
  String getApplInstance() {
    return (applInstance);
  }
  
  public String getLogDir() {
    return (logDir);
  }
  
  public int getMaxFileSize() {
    return (maxFileSize);
  }
  
  public int getMaxZipFiles() {
    return (maxZipFiles);
  }
  
  Handler getHandler(String name) {
    return ((Handler) handlers.get(name));
  }
  
  Handler getHandler(Class name) {
    Enumeration e = handlers.keys();
    while (e.hasMoreElements()) {
      Handler h = (Handler) handlers.get((String) e.nextElement());
      if (h.getClass().equals(name)) {
        return (h);
      }
    }
    
    return (null);
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
  public Tracer getTracer() {
    return (getTracer(defaultTracerName));
  }
  
  public void addHandler(Handler h) {
    handlers.put(h.getName(), h);
  }
  
  public void addTracer(Tracer t) {
    tracers.put(t.getName(), t);
  }
  
  /**
   * Gets a particular named tracer. The name must be one
   * of the names specified in the property tracer.tracers
   *
   * @param name a <code>String</code> value corresponding to one
   *	    one tracer from the property "tracer.tracers"
   * @return a <code>Tracer</code> value
   */
  public Tracer getTracer(String name) {
    if (name == null) {
      name = defaultTracerName;
    }
    Tracer t = (Tracer) tracers.get(name);
    if (t == null) {
      return (Tracer) tracers.get(defaultTracerName);
    }
    return (t);
  }
  
  /**
   * Call all tracers' clear methods.
   *
   */
  public void clear() {
    Enumeration e = tracers.elements();
    while (e.hasMoreElements()) {
      TracerImpl tracer = (TracerImpl) e.nextElement();
      tracer.clear();
    }
  }
  
  /**
   * Call all tracers' flush methods.
   *
   */
  public void flush() {
    Enumeration e = tracers.elements();
    while (e.hasMoreElements()) {
      TracerImpl tracer = (TracerImpl) e.nextElement();
      tracer.flush();
    }
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
  public void setContext(String ctx) {
    Thread currentThread = Thread.currentThread();
    
    synchronized (contexts) {
      if (ctx != null) {
        contexts.put(currentThread, ctx);
      } else {
        contexts.remove(currentThread);
      }
    }
  }
  
  /**
   * Removes the per-thread logger context.
   */
  public void removeContext() {
    Thread currentThread = Thread.currentThread();
    
    synchronized (contexts) {
      contexts.remove(currentThread);
    }
  }
  
  public String getContext() {
    Thread currentThread = Thread.currentThread();
    
    synchronized (contexts) {
      return ((String) contexts.get(currentThread));
    }
  }
  
  String getContext(Thread t) {
    synchronized (contexts) {
      return ((String) contexts.get(t));
    }
  }
  
  /**
   * The <code>isContextEnabled</code> method checks if the context
   * of the tracing thread is enabled or not to trace.
   *
   * @return a <code>boolean</code> value equals to true if the tracing
   *	thread is able to trace a log, or false if not.
   */
  boolean isContextEnabled() {
    Thread currentThread = Thread.currentThread();
    
    synchronized (contexts) {
      //
      // Is tracer context filtering active or not ?
      //
      if (enabledContexts != null) {
        //
        // Is there a context available for the current tracing thread ?
        //
        String ctx = (String) contexts.get(currentThread);
        
        //
        // If not, enable the trace.
        //
        if (ctx == null) {
          return (true);
        }
        
        //
        // If the tracing thread hava a context not present in our context filters:
        // Disable this trace
        //
        if (enabledContexts.indexOf(ctx) == -1) {
          return (false);
        }
      }
      
      //
      // Enable this trace.
      //
      return (true);
    }
  }
  
  public void reloadEnabledContexts(Config cnf) {
    initEnabledContexts(cnf);
  }
  
  /**
   * The <code>propertyChanged</code> method is called whenever a
   * property has changed from configuration (by calling 
   * {@link alcatel.tess.hometop.gateways.utils.Config#setProperty(String,String)} 
   * and {@link alcatel.tess.hometop.gateways.utils.Config#notifyListeners()}
   * methods).
   *
   * @param cnf a <code>Config</code> value containing all config
   * @param props a <code>String[]</code> value containing all property changed 
   *		  names
   * @exception ConfigException if an error occurs
   */
  public void propertyChanged(Config cnf, String[] props) throws ConfigException {
    handleEvent(new ConfigEvent(cnf, props));
  }
  
  synchronized void handleEvent(LogEvent event) {
    try {
      if (Debug.enabled)
        Debug.p(this, "handleEvent", "received event: " + event);
      
      if (event == null) { // flush
        Enumeration e = tracers.elements();
        while (e.hasMoreElements()) {
          TracerImpl tracer = (TracerImpl) e.nextElement();
          event = FlushEvent.acquire(tracer, FlushEvent.FLUSH);
          tracer.handleEvent(event);
        }
        return;
      }
      
      if (event instanceof ConfigEvent) {
        ConfigEvent ce = (ConfigEvent) event;
        reloadProperties(ce.getConfig(), ce.getPropertyChanged());
        return;
      }
      
      TracerImpl lgr = (TracerImpl) event.getTracer();
      lgr.handleEvent(event);
    }
    
    catch (Throwable t) {
      if (Debug.enabled) {
        Debug.p(this, "handleEvent", "Got unexpected exception: " + Utils.parse(t));
      }
    }
  }
  
  /**
   * The constant <code>err</code> is used to print messages to standard
   * output stream. We use this stream to write message to standard output
   * instead of the System.err stream because by default the System.err is
   * System.out is redirected to to the default tracer.
   *
   */
  public final static PrintStream err = new PrintStream(new BufferedOutputStream(new FileOutputStream(
      FileDescriptor.err), 128), true);
  
  /**
   * The constant <code>err</code> is used to print messages to standard
   * error stream.  We use this stream to write message to standard output
   * instead of the System.err stream because by default the System.out
   * is redirected to the default tracer.
   *
   */
  public final static PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(
      FileDescriptor.out), 128), true);
  
  public final static PrintStream stdout = System.out;
  public final static PrintStream stderr = System.err;
  
  private String getHandlerName(String s) {
    return (s.substring("tracer.handler.".length(), s.lastIndexOf(".")));
  }
  
  private String getTracerName(String s) {
    return (s.substring("tracer.".length(), s.lastIndexOf(".")));
  }
  
  private void initHandler(Config cnf, String name) throws ConfigException {
    Handler handler = (Handler) handlers.get(name);
    
    if (handler == null) {
      if (Debug.enabled)
        Debug.p(this, "handlersListChanged", "creating handler " + name);
      
      handler = (Handler) cnf.getClass("tracer.handler." + name + ".class", null, false);
      handler.init(cnf, applInstance, name);
      handlers.put(name, handler);
    }
  }
  
  private void initTracer(Config cnf, String name) throws ConfigException {
    //
    // Creates tracer
    //
    TracerImpl tracer = (TracerImpl) tracers.get(name);
    
    if (tracer == null) {
      if (Debug.enabled)
        Debug.p(this, "tracersListChanged", "creating tracer " + name);
      
      tracer = new TracerImpl(cnf, name, this);
      tracers.put(name, tracer);
    }
  }
  
  private void reloadProperties(Config cnf, String props[]) {
    try {
      //
      // Initialize misc parameters
      //
      for (int i = 0; i < props.length; i++) {
        if (props[i].equals("tracer.enabledContexts")) {
          initEnabledContexts(cnf);
          continue;
        }
        
        if (props[i].equals("tracer.logDir")) {
          logDir = cnf.getProperty("tracer.logDir");
          continue;
        }
        
        if (props[i].equals("tracer.maxFileSize")) {
          maxFileSize = cnf.getInt("tracer.maxFileSize", 0);
          continue;
        }
        
        if (props[i].equals("tracer.maxZipFiles")) {
          maxZipFiles = cnf.getInt("tracer.maxZipFiles", 10);
          if (this.maxZipFiles < 2) {
            this.maxZipFiles = 2;
          }
          continue;
        }
        
        if (props[i].equals("tracer.defaultTracer")) {
          defaultTracerName = cnf.getString("tracer.defaultTracer");
          continue;
        }
      }
      
      // 
      // Reload all current handlers.
      //
      Enumeration e = handlers.elements();
      while (e.hasMoreElements()) {
        ((Handler) e.nextElement()).propertyChanged(cnf, props);
      }
      
      //
      // Check if new handlers must be created.
      //
      e = cnf.getKeys("tracer.handler.*.class");
      while (e.hasMoreElements()) {
        String handlerName = getHandlerName((String) e.nextElement());
        if (handlers.get(handlerName) == null) {
          initHandler(cnf, handlerName);
        }
      }
      
      // 
      // Reload all current tracers.
      //
      e = tracers.elements();
      while (e.hasMoreElements()) {
        ((TracerImpl) e.nextElement()).propertyChanged(cnf, props);
      }
      
      //
      // Check if new tracers must be created.
      //
      e = cnf.getKeys("tracer.*.level");
      while (e.hasMoreElements()) {
        String tracerName = getTracerName((String) e.nextElement());
        if (tracers.get(tracerName) == null) {
          initTracer(cnf, tracerName);
        }
      }
    }
    
    catch (ConfigException e) {
      configException = e;
    }
  }
  
  private void initEnabledContexts(Config cnf) {
    synchronized (contexts) {
      this.enabledContexts = cnf.getString("tracer.enabledContexts", null);
    }
  }
  
  /**
   * The variable <code>handlers</code> contains all log handlers
   * registered in the property "tracers.handlers".
   *
   */
  private Hashtable handlers;
  
  /**
   * The variable <code>tracers</code> contains all tracers 
   * registered in the property "tracer.tracers".
   *
   */
  private Hashtable tracers;
  
  /**
   * The variable <code>contexts</code> contains all per-thread 
   * log context.
   *
   */
  private Hashtable contexts;
  
  /**
   * List of enabled contexts.
   */
  private String enabledContexts;
  
  /**
   * The variable <code>applInstance</code> corresponds to the
   * symbolic logged application name. If is also the directory
   * name under which logs are written.
   *
   */
  private String applInstance;
  
  /**
   * The variable <code>cnf</code> is the config given in argument
   * of the {@link #init(Config,String} method.
   *
   */
  private Config cnf;
  
  /**
   * The variable <code>configException</code> is an eventual config
   * exception occured while parsing the tracer configuration.
   *
   */
  private ConfigException configException;
  
  /**
   * The variable <code>defaultTracerName</code> here is the name of the
   * default tracer used.
   *
   */
  private String defaultTracerName;
  
  private String logDir;
  private int maxFileSize;
  private int maxZipFiles;
  private static Object LOCK = new Object();
  private static boolean catchStdout = true;
  
  private final static String OUT_OF_MEMORY_ERROR = "java.lang.OutOfMemoryError";
  private final static String OUT_OF_MEMORY_ERROR_MSG = "Application exiting on OutOfMemory error: ";
  private final static boolean EXIT_ON_OUTOFMEM = (System.getProperty("NxTracer.exitOnOutOfMemory", "true")
      .equals("true"));
}
