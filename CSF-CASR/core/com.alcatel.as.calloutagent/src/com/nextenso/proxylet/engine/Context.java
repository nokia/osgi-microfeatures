package com.nextenso.proxylet.engine;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.Hashtable;

import com.nextenso.agent.event.AsynchronousEvent;
import com.nextenso.agent.event.AsynchronousEventListener;
import com.nextenso.agent.event.AsynchronousEventScheduler;
import com.nextenso.mux.MuxHandler;
import com.nextenso.proxylet.Proxylet;
import com.nextenso.proxylet.ProxyletContext;
import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionWrapper;
import com.nextenso.proxylet.event.ProxyletContextEvent;
import com.nextenso.proxylet.event.ProxyletContextListener;
import com.nextenso.proxylet.mgmt.Monitor;
import com.nextenso.proxylet.mgmt.impl.ProxyletMonitorImpl;

public abstract class Context implements ProxyletContext, AsynchronousEventListener, Observer {
  protected static final int EVENT_CONTEXT_CONSTRUCTED = 1;
  protected static final int EVENT_CONTEXT_NOT_CONSTRUCTED = 2;
  private static final ProxyletContextListener[] INIT_LISTENERS = new ProxyletContextListener[0];
  @SuppressWarnings("unchecked")
  private static final Map<String, ProxyAppEnv> PROXY_APP_ENVS = new HashMap<String, ProxyAppEnv>();
  private final static Object LOCK = new Object();
  private static ThreadLocal<MuxHandler> _muxHandlerLocal = new ThreadLocal<MuxHandler>();
  private Logger _logger = Logger.getLogger("callout"); // default value to be overridden
  private String _name, _description = "", _id;
  private Object _listenersLock = new Object();
  private ProxyletContextListener[] _contextListeners = INIT_LISTENERS;
  private Hashtable _initParams = new Hashtable();
  private Hashtable _attributes = new Hashtable();
  private CriterionWrapper _criterion;
  private MuxHandler _muxHandler;
  @SuppressWarnings("unchecked")
  private Dictionary _properties; // proxylet properties
  @SuppressWarnings("unchecked")
  private Dictionary _systemProperties;
  private ProxyletMonitorImpl _monitor;
  private volatile ProxyletApplication _app;
  
  @SuppressWarnings("unchecked")
  public void setSystemProperties(Dictionary systemProperties) {
    _systemProperties = systemProperties;
  }
  
  public Context(Logger logger) {
    _logger = logger;
    _muxHandler = _muxHandlerLocal.get();
    _monitor = new ProxyletMonitorImpl(this);
  }
  
  @SuppressWarnings("deprecation")
  public void resume(ProxyletData msg, int status) {
    msg.resume(status);
  }
  
  public static void setMuxHandlerLocal(MuxHandler handler) {
    _muxHandlerLocal.set(handler);
  }
  
  public abstract void load(ProxyletApplication app) throws Exception;
  
  public void init(ProxyletApplication app) throws Exception {
    log("Initialization Starting");
    _properties = app.getProperties();
    _app = app;
    
    ProxyletChain[] chains = getProxyletChains();
    for (int k = 0; k < chains.length; k++) {
      ProxyletEnv[] envs = chains[k].getValue();
      for (int i = 0; i < envs.length; i++) {
        envs[i].setProperties(_properties);
        envs[i].setSystemProperties(_systemProperties);
      }
    }
    
    // For launcher support, we must provide launcher arg in the proxylet context.
    if (_systemProperties != null) {
      Object launcherArg = _systemProperties.get(ProxyletConstants.PARAM_NAME_LAUNCHER_ARG);
      if (launcherArg != null) {
        setAttribute(ProxyletConstants.PARAM_NAME_LAUNCHER_ARG, launcherArg);
      }
    }
    
    // Listen for property changes.
    app.observeProperties(this /* we implement Observer */);
  }
  
  // java.util.Observer interface
  @SuppressWarnings("unchecked")
  public void update(Observable o, Object arg) {
		if (_app != null) {
			_properties = _app.getProperties();
			String[] modified = (String[]) arg;

			// Notify proxylet config listeners.
			ProxyletChain[] chains = getProxyletChains();
			for (int k = 0; k < chains.length; k++) {
				ProxyletEnv[] envs = chains[k].getValue();
				for (int i = 0; i < envs.length; i++) {
					envs[i].setProperties(_properties);
					envs[i].updateProperties(modified);
				}
			}
		}
	  }
  
  public void destroy() {
    ProxyletChain[] chains = getProxyletChains();
    for (int k = 0; k < chains.length; k++) {
      chains[k].destroy();
    }
    ProxyletContextListener[] listeners = _contextListeners;
    int n = listeners.length;
    if (n == 0)
      return;
    ProxyletContextEvent event = new ProxyletContextEvent(this, this);
    for (int i = 0; i < n; i++)
      listeners[i].contextDestroyed(event);
  }
  
  public MuxHandler getMuxHandler() {
    return _muxHandler;
  }
  
  public void setId(String id) {
    this._id = id;
  }
  
  public String getId() {
    return this._id;
  }
  
  public void setName(String name) {
    this._name = name;
  }
  
  public String getName() {
    return this._name;
  }
  
  public void setDescription(String desc) {
    this._description = desc;
  }
  
  public String getDescription() {
    return this._description;
  }
  
  public void addInitParameter(String name, String value) {
    if (name == null)
      return;
    if (value != null)
      _initParams.put(name, value);
    else
      _initParams.remove(name);
  }
  
  public abstract ProxyletChain[] getProxyletChains();
  
  public void setCriterionWrapper(CriterionWrapper criterion) {
    this._criterion = criterion;
  }
  
  public CriterionWrapper getCriterionWrapper() {
    return _criterion;
  }
  
  public Criterion getCriterion() {
    return _criterion.getCriterion();
  }
  
  /********************************************
   * Implementation of AsynchronousEventListener
   ********************************************/
  
  public void asynchronousEvent(Object data, int type) {
    if (_contextListeners.length == 0)
      return;
    
    ProxyletContextEvent event = null;
    if (type == EVENT_CONTEXT_CONSTRUCTED) {
      // the data is the event
      event = (ProxyletContextEvent) data;
    } else if (type == EVENT_CONTEXT_NOT_CONSTRUCTED) {
      // the data is the source of the event
      event = new ProxyletContextEvent(data, this);
    }
    
    if (event != null) {
      for (ProxyletContextListener listener : _contextListeners) {
        listener.contextEvent(event);
      }
    }
    
  }
  
  /********************************************
   * Implementation of ProxyletContext
   ********************************************/
  
  /**
   * Gets the configuration found from all mbeans-descriptors (not from xml)
   */
  public String getInitParameter(String name) {
    return (String) _properties.get(name);
  }
  
  /**
   * Gets the list of configurations found from all mbeans-descriptors (not from xml)
   * 
   */
  @SuppressWarnings("unchecked")
  public Enumeration getInitParameterNames() {
    return _properties.keys();
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#getMajorVersion()
   */
  public int getMajorVersion() {
    return ProxyletConstants.MAJOR_VERSION;
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#getMinorVersion()
   */
  public int getMinorVersion() {
    return ProxyletConstants.MINOR_VERSION;
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#getServerInfo()
   */
  public String getServerInfo() {
    return ProxyletConstants.SERVER_INFO;
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#fireProxyletContextEvent(java.lang.Object, boolean)
   */
  public void fireProxyletContextEvent(Object source, boolean asynchronous) {
    ProxyletContextListener[] listeners = _contextListeners;
    int n = listeners.length;
    if (n == 0)
      return;
    
    if (asynchronous) {
      AsynchronousEventScheduler.schedule(new AsynchronousEvent(this, source, EVENT_CONTEXT_NOT_CONSTRUCTED));
    } else {
      ProxyletContextEvent event = new ProxyletContextEvent(source, this);
      for (int i = 0; i < n; i++)
        listeners[i].contextEvent(event);
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#fireProxyletContextEvent(com.nextenso.proxylet.event.ProxyletContextEvent, boolean)
   */
  public void fireProxyletContextEvent(ProxyletContextEvent event, boolean asynchronous) {
    ProxyletContextListener[] listeners = _contextListeners;
    int n = listeners.length;
    if (n == 0)
      return;
    if (asynchronous) {
      AsynchronousEventScheduler.schedule(new AsynchronousEvent(this, event, EVENT_CONTEXT_CONSTRUCTED));
    } else {
      for (int i = 0; i < n; i++) {
        listeners[i].contextEvent(event);
      }
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#registerProxyletContextListener(com.nextenso.proxylet.event.ProxyletContextListener)
   */
  public void registerProxyletContextListener(ProxyletContextListener listener) {
    // Decorate the listener with our dynamic proxy, which sets proper class loader.
    ProxyletContextListener theListener = (ProxyletContextListener) ProxyletInvocationHandler.newInstance(
        listener.getClass().getClassLoader(), listener);
    
    synchronized (_listenersLock) {
      int n = _contextListeners.length;
      ProxyletContextListener[] clone = new ProxyletContextListener[n + 1];
      System.arraycopy(_contextListeners, 0, clone, 0, n);
      clone[n] = theListener;
      _contextListeners = clone;
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#deregisterProxyletContextListener(com.nextenso.proxylet.event.ProxyletContextListener)
   */
  public void deregisterProxyletContextListener(ProxyletContextListener listener) {
    synchronized (_listenersLock) {
      int n = _contextListeners.length;
      int index = -1;
      for (int k = 0; k < n; k++) {
        // Warning: contextListeners[k] is a Proxy to the listener and we must get the inner
        // object ..
        if (ProxyletInvocationHandler.getProxylet(_contextListeners[k]) == listener) {
          index = k;
          break;
        }
      }
      if (index == -1)
        return;
      ProxyletContextListener[] clone = new ProxyletContextListener[n - 1];
      if (index > 0)
        System.arraycopy(_contextListeners, 0, clone, 0, index);
      if (index != (n - 1))
        System.arraycopy(_contextListeners, index + 1, clone, index, n - 1 - index);
      _contextListeners = clone;
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#isLogEnabled()
   */
  public boolean isLogEnabled() {
    return _logger.isInfoEnabled();
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#log(java.lang.String)
   */
  public void log(String message) {
    if (_logger.isInfoEnabled()) {
      _logger.info(message);
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#log(java.lang.String, java.lang.Throwable)
   */
  public void log(String message, Throwable t) {
    if (_logger.isInfoEnabled()) {
      _logger.info(message, t);
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#isDebugEnabled()
   */
  public boolean isDebugEnabled() {
    return _logger.isDebugEnabled();
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#debug(java.lang.String)
   */
  public void debug(String message) {
    if (_logger.isDebugEnabled()) {
      _logger.debug(message);
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#debug(java.lang.String, java.lang.Throwable)
   */
  public void debug(String message, Throwable t) {
    if (_logger.isDebugEnabled()) {
      _logger.debug(message, t);
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#isWarnEnabled()
   */
  public boolean isWarnEnabled() {
    return _logger.isEnabledFor(Level.WARN);
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#warn(java.lang.String)
   */
  public void warn(String message) {
    if (isWarnEnabled()) {
      _logger.warn(message);
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#warn(java.lang.String, java.lang.Throwable)
   */
  public void warn(String message, Throwable t) {
    if (isWarnEnabled()) {
      _logger.warn(message, t);
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#getProxyletContextName()
   */
  public String getProxyletContextName() {
    return _name;
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#getAttribute(java.lang.String)
   */
  public Object getAttribute(String name) {
    synchronized (_attributes) {
      return _attributes.get(name);
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#getAttributeNames()
   */
  @SuppressWarnings("unchecked")
  public Enumeration getAttributeNames() {
    synchronized (_attributes) {
      return _attributes.keys();
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#setAttribute(java.lang.String, java.lang.Object)
   */
  public void setAttribute(String name, Object value) {
    synchronized (_attributes) {
      _attributes.put(name, value);
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#removeAttribute(java.lang.String)
   */
  public Object removeAttribute(String name) {
    synchronized (_attributes) {
      return _attributes.remove(name);
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#getResourceAsStream(java.lang.String)
   */
  public InputStream getResourceAsStream(String name) throws IOException {
    return ClassLoader.getSystemClassLoader().getResourceAsStream(name);
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletContext#getMonitor()
   */
  public Monitor getMonitor() {
    return _monitor;
  }
  
  @SuppressWarnings("unchecked")
  protected static void setProxyAppEnv(String proxyAppId, ProxyAppEnv env) {
    synchronized (LOCK) {
      PROXY_APP_ENVS.put(proxyAppId, env);
    }
  }
  
  public static ProxyAppEnv getProxyAppEnv(String proxyAppId) {
    return PROXY_APP_ENVS.get(proxyAppId);
  }
  
  public static Iterator getProxyAppEnvs() {
    return PROXY_APP_ENVS.values().iterator();
  }
  
  /**
   * Loads the different listeners into this context.
   */
  protected void loadListeners(ProxyletApplication app)
      throws com.nextenso.proxylet.engine.xml.XMLConfigException {
    for (String listenerClass : app.getListeners(ProxyletApplication.CONTEXT_LISTENER)) {
      ProxyAppEnv proxyAppEnv = getProxyAppEnv(app.getProxyAppName(ProxyletApplication.CONTEXT_LISTENER,
          listenerClass));
      if (proxyAppEnv == null) {
        proxyAppEnv = new ProxyAppEnv(
            app.getProxyAppName(ProxyletApplication.CONTEXT_LISTENER, listenerClass), app.getProxyAppVersion(
                ProxyletApplication.CONTEXT_LISTENER, listenerClass));
        proxyAppEnv.setClassLoader(app.getClassLoader(ProxyletApplication.CONTEXT_LISTENER, listenerClass));
        setProxyAppEnv(app.getProxyAppName(ProxyletApplication.CONTEXT_LISTENER, listenerClass), proxyAppEnv);
      }
      loadProxyletContextListener(app, listenerClass);
    }
  }
  
  protected void loadChains(Map<String, List<ProxyletEnv>> chains, ProxyletApplication app) {
    if (chains == null)
      throw new IllegalArgumentException("no chains defined");
    if (app == null)
      throw new IllegalArgumentException("no application to load");
    for (Entry<String, List<ProxyletEnv>> entry : chains.entrySet()) {
      String chainType = entry.getKey();
      for (String name : app.getProxylets(chainType)) {
        String pxletAppName = app.getProxyAppName(chainType, name);
        String pxletAppVersion = app.getProxyAppVersion(chainType, name);
        String pxletAppId = pxletAppName + "/" + pxletAppVersion;
        String pxletName = app.getProxyletName(chainType, name);
        String pxletDesc = app.getProxyletDesc(chainType, name);
        ClassLoader cl = app.getClassLoader(chainType, name);
        Proxylet pxlet = (Proxylet) app.getProxylet(chainType, name);
        
        ProxyletEnv env = new ProxyletEnv();
        env.setContext(this);
        if (_logger.isDebugEnabled()) {
          _logger.debug("loadChains: Loading proxylet <" + pxletName + "> (" + pxlet.getClass().getName()
              + ") from application : <" + pxletAppId + ">");
        }
        ProxyAppEnv proxyAppEnv = getProxyAppEnv(pxletAppId);
        if (proxyAppEnv == null) {
          proxyAppEnv = new ProxyAppEnv(pxletAppName, pxletAppVersion);
          proxyAppEnv.setClassLoader(cl);
          setProxyAppEnv(pxletAppId, proxyAppEnv);
        }
        
        env.setProxyAppEnv(proxyAppEnv);
        env.setProxyletName(pxletName);
        env.setProxyletDescription(pxletDesc);
        
        env.setProxylet(pxlet);
        
        // Loads criterion
        CriterionWrapper wrapper = new CriterionWrapper();
        wrapper.setName(app.getProxyletCriterionName(chainType, name));
        wrapper.setDescription(app.getProxyletCriterionDesc(chainType, name));
        wrapper.setCriterion(app.getProxyletCriterionValue(chainType, name));
        log("Criterion for proxylet <" + pxletName + ">: " + wrapper.getCriterion());
        env.setCriterionWrapper(wrapper);
        
        // Loads parameters
        Dictionary params = app.getProxyletParams(chainType, name);
        if (params != null)
          for (Enumeration e = params.keys(); e.hasMoreElements();) {
            String k = (String) e.nextElement();
            env.addParameter(k, (String) params.get(k));
          }
        
        if (addingProxylet(chainType, pxlet))
          entry.getValue().add(env);
      }
    }
  }
  
  @SuppressWarnings("unused")
  protected boolean addingProxylet(String chainType, Proxylet p) {
    // to be overridden if needed
    return true;
  }
  
  /**
   * Loads the given common listener. Note that listener is loaded only if its type is
   * "context-listener", and if the class implements ProxyletContextListener.
   * 
   */
  private void loadProxyletContextListener(ProxyletApplication app, String clazz)
      throws com.nextenso.proxylet.engine.xml.XMLConfigException {
    try {
      Object o = app.getListener(ProxyletApplication.CONTEXT_LISTENER, clazz);
      if (o instanceof ProxyletContextListener) {
        ProxyletContextListener ctxListener = (ProxyletContextListener) o;
        registerProxyletContextListener(ctxListener);
      }
    } catch (Throwable t) {
      throw new com.nextenso.proxylet.engine.xml.XMLConfigException(
          "Exception while loading proxylet context listener class: " + clazz + ": " + t);
    }
  }
}
