package com.nextenso.http.agent.impl;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alcatel_lucent.convergence.services.PrivateAttributesNames;
import com.alcatel_lucent.ha.services.FlatField;
import com.alcatel_lucent.ha.services.Flattable;
import com.alcatel_lucent.ha.services.FlattableSupport;
import com.alcatel_lucent.ha.services.HAContext;
import com.alcatel_lucent.ha.services.annotation.Flat;
import com.nextenso.http.agent.Utils;
import com.nextenso.http.agent.engine.HttpProxyletContext;
import com.nextenso.proxylet.ProxyletContext;
import com.nextenso.proxylet.http.HttpSession;

public class HttpSessionFacade implements HttpSession {
  
  private final static String K_PREFIX = "@";
  private final static int K_PREFIX_LEN = 1;
  private final static String K_MAX_INACTIVE_INTERVAL = "#MII";
  
  public HttpSessionFacade(Utils utils) {
    this.attributes = new ConcurrentHashMap<String, Object>();
    this.modifiedParams = new ConcurrentHashMap<String, Boolean>();
    destroyed = new AtomicBoolean();
    this.utils = utils;
  }
  
  public HttpSessionFacade(SessionManager sessionMngr, boolean secure, Utils utils) {
    this(utils);
    this.sessionMngr = sessionMngr;
    this.secure = secure;
    this.creationTime = System.currentTimeMillis();
    this.accessedTime = this.creationTime;
  }
    
  void setSessionId(String sessionId) {
	  this.sessionId = sessionId;
  }
  
  public String getSessionId() {
	  return sessionId;
  }
  
  public long getId() {
    return sessionId == null ? -1 : utils.getAgent().getSessionPolicy().hash64(sessionId);
  }
  
  public void setRemoteAddr(String clip) {
    this.clip = clip;
  }
  
  public void setRemoteId(String key) {
    this.clid = key;
  }
  
  public long getCreationTime() {
    return (this.creationTime);
  }
  
  public void setCreationTime(long time) {
    this.creationTime = time;
  }
  
  public long getAccessedTime() {
    return (this.accessedTime);
  }
  
  public void updateAccessedTime() {
    this.accessedTime = System.currentTimeMillis();
  }
  
  public void updateAccessedTimeOnReq() {
    lastAccessedTime = reqAccessedTime;
    updateAccessedTime();
    reqAccessedTime = accessedTime;
  }
  
  public long getLastAccessedTime() {
    return (this.lastAccessedTime);
  }
  
  public Object getAttribute(Object name) {
    return attributes.get(name.toString());
  }
  
  @SuppressWarnings("unchecked")
  public Enumeration<String> getAttributeNames() {
    if (attributes.size() > 0) {
      return Collections.enumeration(attributes.keySet());
    }
    return Collections.enumeration(Collections.EMPTY_LIST);
  }
  
  public void setAttribute(Object name, Object value) {
    if (value == null)
      removeAttribute(name);
    else {
      String key = K_PREFIX + name.toString();
      attributes.put(key.substring(K_PREFIX_LEN), value);
      if (this.secure) {
        if (diff(false) != null)
          diff(false).add("attributes" + Flattable.MAPSEP + name);
        modifiedParams.put(key, Boolean.TRUE);
      }
      this.accessed = true;
    }
  }
  
  public Object removeAttribute(Object name) {
    String key = K_PREFIX + name.toString();
    Object value = attributes.remove(key.substring(K_PREFIX_LEN));
    if (this.secure && (value != null)) {
      if (diff(false) != null)
        diff(false).add("attributes" + Flattable.MAPSEP + name);
      modifiedParams.put(key, Boolean.FALSE);
    }
    this.accessed = true;
    return value;
  }
  
  public void restoreAttribute(String name, Object value) {
    attributes.put(name, value);
  }
  
  public void setAllAttributesModified() {
    for(String name : attributes.keySet()) {
      modifiedParams.put(K_PREFIX + name, Boolean.TRUE);
    }
  }
  
  public String getRemoteAddr() {
    return (this.clip);
  }
  
  public String getRemoteHost() {
    return (this.clip);
  }
  
  public void setRemoteHost(String clip) {
    this.clip = clip;
  }
  
  public String getRemoteId() {
    return (this.clid);
  }
  
  public void sessionCookieSet() {
    this.accessed = true;
    this.cookieSet = true;
  }
  
  public boolean isSessionCookieSet() {
    return this.cookieSet;
  }
  
  public void setContextPath(String contextPath) {
    if (contextPath != null)
      this.contextPath = contextPath;
    else
      this.contextPath = "";
  }
  
  public String getContextPath() {
    return this.contextPath;
  }
  
  public boolean isAccessed() {
    return this.accessed;
  }
  
  public ProxyletContext getProxyletContext() {
    return this.context;
  }
  
  public void setProxyletContext(HttpProxyletContext ctx) {
    this.context = ctx;
  }
  
  public String newSession() {
    return sessionMngr.newSession();
  }
  
  // refreshes the session for the client
  public void destroy() {
    if (destroyed.compareAndSet(false, true)) {
      if (sessionMngr != null)
        sessionMngr.invalidateSession(this);
      if (context != null) {
        context.sessionDestroyed(this);
      }
    }
  }
  
  public void jsr154Invalidate() {
    if (destroyed.get() == false) {
      // first, call the listeners (servlet 2.5)
      if (context != null) {
        context.sessionInvalidated(this);
      }
      // Then, mark the session "invalidated"
      if (sessionMngr != null)
        sessionMngr.invalidateSession(this);
      this.jsr154Invalidated = true;
    }
  }
  
  public boolean isJsr154Invalidated() {
    return this.jsr154Invalidated;
  }
  
  public int getMaxInactiveInterval() {
    return sessionMngr.getMaxInactiveInterval();
  }
  
  public void setMaxInactiveInterval(int interval) {
    sessionMngr.setMaxInactiveInterval(interval);
    if (this.secure) {
      modifiedParams.put(K_MAX_INACTIVE_INTERVAL, Boolean.TRUE);
    }
  }
  
  public void complete() {
    sessionMngr.complete();
  }
  
  // called to close the session
  public void close() {
    if (destroyed.compareAndSet(false, true)) {
      if (context != null) // context may be null if an Exception occurs before the first request
        // is passed to the pxlet engine
        context.sessionDestroyed(this);
    }
  }
  
  public Map<String, Boolean> getModifiedParams() {
    return modifiedParams;
  }
  
  public interface SessionManager {
    /**
     * used when invalidating a session and for session management
     */
    public void invalidateSession(HttpSessionFacade session);
    
    public int getMaxInactiveInterval();
    
    public void setMaxInactiveInterval(int interval);
    
    public void complete();
    
    public String newSession();

    public String changeSessionId();
  }
  
  private SessionManager sessionMngr;
  private long id;
  private String sessionId;
  @Flat(key = "T")
  private long creationTime;
  private long accessedTime;
  private long reqAccessedTime;
  private long lastAccessedTime;
  @Flat(key = "P")
  private String clip;
  @Flat(key = "D")
  private String clid;
  @Flat(key = "A")
  private Map<String, Object> attributes;
  private Map<String, Boolean> modifiedParams;
  private HttpProxyletContext context;
  private AtomicBoolean destroyed;
  private boolean jsr154Invalidated;
  private volatile boolean accessed;
  private boolean cookieSet;
  private boolean secure;
  @Flat(key = "X")
  private String contextPath = "";
  private Utils utils;
  
  public String toString() {
    String res = "HttpSessionFacade=";
    res = res + "id: " + id + "\n";
    res = res + "clip: " + clip + "\n";
    res = res + "clid: " + clid + "\n";
    return res;
  }
  
  //------------------------------------- Flattable Interface -------------------------------------
  transient java.util.Set<String> _diff;
  transient int _k = 0;
  
  int tmpmaxinterval = 0;
  
  public int key(int key) {
    if (_k == 0)
      _k = key;
    return _k;
  }
    
  public void readDone() {
    //FIXME callback listeners here...
  }
    
  public Set<String> diff(boolean create) {
    if (_diff != null)
      return _diff;
    return create ? _diff = FlattableSupport.buildDiff() : null;
  }
  
  public void setSessionMngr(SessionManager mngr) {
    sessionMngr = mngr;
  }
  
  public void updateMaxInactiveInterval() {
    setMaxInactiveInterval(tmpmaxinterval);
  }
  
  public void setSecure(boolean s) {
    secure = s;
  }
  
  // For New HA and convergence
  private Object _as = null;
  private Object _ctx = null;
  
  public void setHAContext(HAContext ctx) {
    _ctx = ctx;
  }
  
  public HAContext getHAContext() {
    return (HAContext) _ctx;
  }
  
  public Object getPrivateAttribute(PrivateAttributesNames name) {
    switch (name) {
    case SipApplicationSession:
      return _as;
    case HAContext:
      return _ctx;
    case ContextPath:
      return contextPath;
    default:
      return null;
      
    }
  }
  
  public void setPrivateAttribute(PrivateAttributesNames name, Object attribute) {
    switch (name) {
    case SipApplicationSession:
      _as = attribute;
      return;
    case HAContext:
      _ctx = attribute;
      return;
    case RemoteID:
      clid = (String) attribute;
      return;
    default:
      return;
    }
  }
  
  public Object readField(Field f) throws IllegalArgumentException, IllegalAccessException {
    return f.get(this);
  }
  
  public void writeField(Field f, Object value) throws IllegalArgumentException, IllegalAccessException {
    f.set(this, value);
  }
  
  private static List<FlatField> _flatfields;
  static {
    _flatfields = FlattableSupport.buildFields(HttpSessionFacade.class);
  }
  
  public List<FlatField> fields() {
    return _flatfields;
  }
  
}
