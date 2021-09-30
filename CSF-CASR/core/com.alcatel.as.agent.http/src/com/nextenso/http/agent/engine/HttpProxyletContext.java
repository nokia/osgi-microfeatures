package com.nextenso.http.agent.engine;

import static com.nextenso.proxylet.admin.http.HttpBearer.REQUEST_CHAIN;
import static com.nextenso.proxylet.admin.http.HttpBearer.REQUEST_LISTENER;
import static com.nextenso.proxylet.admin.http.HttpBearer.RESPONSE_CHAIN;
import static com.nextenso.proxylet.admin.http.HttpBearer.RESPONSE_LISTENER;
import static com.nextenso.proxylet.admin.http.HttpBearer.SESSION_LISTENER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.nextenso.agent.event.AsynchronousEvent;
import com.nextenso.agent.event.AsynchronousEventScheduler;
import com.nextenso.http.agent.ext.HttpSessionActivationHandler;
import com.nextenso.http.agent.ext.HttpSessionActivationListener;
import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.engine.Context;
import com.nextenso.proxylet.engine.ProxyletApplication;
import com.nextenso.proxylet.engine.ProxyletChain;
import com.nextenso.proxylet.engine.ProxyletEnv;
import com.nextenso.proxylet.engine.xml.XMLConfigException;
import com.nextenso.proxylet.event.ProxyletEventListener;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpResponse;
import com.nextenso.proxylet.http.HttpSession;
import com.nextenso.proxylet.http.event.HttpSessionEvent;
import com.nextenso.proxylet.http.event.HttpSessionListener;
import com.nextenso.proxylet.impl.ProxyletDataImpl;

public class HttpProxyletContext extends Context implements HttpSessionActivationHandler {
  
  private static final int EVENT_SESSION_DESTROYED = 3;
  
  private final static Logger _logger = Logger.getLogger("agent.http.context");
  private ArrayList<HttpSessionListener> sessionListeners = new ArrayList<HttpSessionListener>();
  private ArrayList<ProxyletEventListener> requestListeners = new ArrayList<ProxyletEventListener>();
  private ArrayList<ProxyletEventListener> responseListeners = new ArrayList<ProxyletEventListener>();
  private ArrayList<HttpSessionActivationListener> sessionActivationListeners = new ArrayList<HttpSessionActivationListener>();
  private String nextHop = null;
  private HttpProxyletContainer _container;
  private HttpProxyletChain[] _chains;
  
  public HttpProxyletContext(HttpProxyletContainer container, ProxyletApplication app) throws Exception {
    super(_logger);
    _container = container;
    _chains = new HttpProxyletChain[] { new HttpProxyletChain(this, HttpProxyletChain.REQUEST_CHAIN),
        new HttpProxyletChain(this, HttpProxyletChain.RESPONSE_CHAIN) };
    load(app);
  }
  
  /**
   * @see com.nextenso.proxylet.engine.Context#resume(com.nextenso.proxylet.ProxyletData, int)
   */
  @Override
  public void resume(ProxyletData msg, int status) {
    msg.resume(status);
  }
  
  public String getAppName() { // FIXME never used ?
    return _container.getAppName();
  }
  
  public void load(ProxyletApplication app) throws Exception {
    setName("Http Proxylet Context");
    setDescription("List of http proxylets");
    loadChains(app);
    loadListeners(app);
  }
  
  public void init(ProxyletApplication app) throws Exception {
    super.init(app);
    // we initialize the proxylets
    log("Initializing Request Proxylets");
    _chains[0].init();
    log("Initializing Response Proxylets");
    _chains[1].init();
    log("Initialization done");
  }
  
  public HttpProxyletChain getRequestChain() {
    return _chains[0];
  }
  
  public HttpProxyletChain getResponseChain() {
    return _chains[1];
  }
  
  public void addSessionListener(HttpSessionListener listener) {
    // no need to synchronize - called at initialization
    // we avoid duplicates
    int index = sessionListeners.indexOf(listener);
    if (index == -1)
      sessionListeners.add(listener);
  }
  
  public void addRequestListener(ProxyletEventListener listener) {
    // no need to synchronize - called at initialization
    // we avoid duplicates
    int index = requestListeners.indexOf(listener);
    if (index == -1)
      requestListeners.add(listener);
  }
  
  public void addResponseListener(ProxyletEventListener listener) {
    // no need to synchronize - called at initialization
    // we avoid duplicates
    int index = responseListeners.indexOf(listener);
    if (index == -1)
      responseListeners.add(listener);
  }
  
  public void setNextHop(String value) {
    nextHop = value;
  }
  
  public void sessionCreated(HttpSession session) {
    int n = sessionListeners.size();
    if (n == 0)
      return;
    HttpSessionEvent event = new HttpSessionEvent(session);
    for (int i = 0; i < n; i++)
      sessionListeners.get(i).sessionCreated(event);
  }
  
  public void sessionInvalidated(HttpSession session) {
    int n = sessionListeners.size();
    if (n == 0)
      return;
    HttpSessionEvent event = new HttpSessionEvent(session);
    for (int i = 0; i < n; i++)
      sessionListeners.get(i).sessionDestroyed(event);
  }
  
  public void sessionDestroyed(HttpSession session) {
    if (sessionListeners.size() == 0)
      return;
    AsynchronousEventScheduler.schedule(new AsynchronousEvent(this, session,
        HttpProxyletContext.EVENT_SESSION_DESTROYED));
  }
  
  public void sessionDidActivate(HttpSession session) {
    int n = sessionActivationListeners.size();
    if (n == 0)
      return;
    HttpSessionEvent event = new HttpSessionEvent(session);
    for (int i = 0; i < n; i++)
      sessionActivationListeners.get(i).sessionDidActivate(event);
  }
  
  public void sessionWillPassivate(HttpSession session) {
    int n = sessionActivationListeners.size();
    if (n == 0)
      return;
    HttpSessionEvent event = new HttpSessionEvent(session);
    for (int i = 0; i < n; i++)
      sessionActivationListeners.get(i).sessionWillPassivate(event);
  }
  
  public void init(HttpRequest req) {
    // we register the static request listeners
    int n = requestListeners.size();
    if (n != 0) {
      for (int i = 0; i < n; i++)
        req.registerProxyletEventListener((ProxyletEventListener) requestListeners.get(i));
    }
    // we register the static response listeners
    n = responseListeners.size();
    if (n != 0) {
      HttpResponse resp = req.getResponse();
      for (int i = 0; i < n; i++)
        resp.registerProxyletEventListener((ProxyletEventListener) responseListeners.get(i));
    }
    // we set the next-hop if necessary
    if (nextHop != null)
      req.setNextHop(nextHop);
    // we set the proxylet context
    ((ProxyletDataImpl) req).setProxyletContext(this);
    ((ProxyletDataImpl) (req.getResponse())).setProxyletContext(this);
  }
  
  /********************************************
   * Implementation of AsynchronousEventListener
   ********************************************/
  
  @Override
  public void asynchronousEvent(Object data, int type) {
    if (type == EVENT_SESSION_DESTROYED) {
      // the data is the destroyed session
      int size = sessionListeners.size();
      if (size == 0)
        return;
      com.nextenso.proxylet.http.HttpSession session = (com.nextenso.proxylet.http.HttpSession) data;
      HttpSessionEvent event = new HttpSessionEvent(session);
      for (int i = 0; i < size; i++)
        sessionListeners.get(i).sessionDestroyed(event);
    } else {
      super.asynchronousEvent(data, type);
    }
  }
  
  /**
   * Implementation of HttpActivationSessionHandler
   */
  
  public void registerActivationSessionListener(HttpSessionActivationListener listener) {
    synchronized (sessionActivationListeners) {
      // we avoid duplicates
      int index = sessionActivationListeners.indexOf(listener);
      if (index == -1)
        sessionActivationListeners.add(listener);
    }
  }
  
  public void deregisterActivationSessionListener(HttpSessionActivationListener listener) {
    synchronized (sessionActivationListeners) {
      sessionActivationListeners.remove(listener);
    }
  }
  
  /**
   * Load the listener into this context
   */
  protected void loadListeners(ProxyletApplication app) throws XMLConfigException {
    super.loadListeners(app);
    // Load session/request/response listeners.
    String type;
    for (String name : app.getListeners((type = SESSION_LISTENER))) {
      addSessionListener((HttpSessionListener) app.getListener(type, name));
    }
    for (String name : app.getListeners((type = REQUEST_LISTENER))) {
      addRequestListener((ProxyletEventListener) app.getListener(type, name));
    }
    for (String name : app.getListeners((type = RESPONSE_LISTENER))) {
      addResponseListener((ProxyletEventListener) app.getListener(type, name));
    }
  }
  
  /**
     Loads the different proxylet chains into this context.
  */
  private void loadChains(ProxyletApplication app) throws XMLConfigException {
    @SuppressWarnings("serial")
    Map<String, List<ProxyletEnv>> chains = new HashMap<String, List<ProxyletEnv>>() {
      {
        put(REQUEST_CHAIN, new ArrayList<ProxyletEnv>());
        put(RESPONSE_CHAIN, new ArrayList<ProxyletEnv>());
      }
    };
    
    super.loadChains(chains, app);
    
    if (chains.get(REQUEST_CHAIN).size() > 0)
      getRequestChain().setValue(chains.get(REQUEST_CHAIN));
    
    if (chains.get(RESPONSE_CHAIN).size() > 0)
      getResponseChain().setValue(chains.get(RESPONSE_CHAIN));
  }
  
//  private static void throwXMLConfigException(String msg, Throwable t) throws XMLConfigException {
//    XMLConfigException e = new XMLConfigException(msg);
//    e.initCause(t);
//    throw e;
//  }
  
  @Override
  public ProxyletChain[] getProxyletChains() {
    return _chains;
  }
}
