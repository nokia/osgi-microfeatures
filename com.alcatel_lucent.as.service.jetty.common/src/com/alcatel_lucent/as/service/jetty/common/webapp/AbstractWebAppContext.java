// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.jetty.common.webapp;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.management.JMException;
import javax.management.modelmbean.ModelMBean;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipSessionsUtil;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.log.Log;

import com.alcatel.as.service.appmbeans.ApplicationMBeanFactory;
import com.alcatel.as.util.osgi.ServiceRegistry;
import com.alcatel_lucent.as.service.jetty.common.deployer.AbstractContextDeployer;
import com.alcatel_lucent.convergence.services.ConvergenceService;
import com.alcatel_lucent.convergence.services.ConvergenceService.EventPropagation;
import com.alcatel_lucent.convergence.services.ConvergenceService.SessionManagement;

public abstract class AbstractWebAppContext extends org.eclipse.jetty.webapp.WebAppContext {

  private final static char NAME_SEPARATOR = '_';
  private final static int DEFAULT_SESSION_TIMEOUT = 900;

  private static String DEFAULT_WEB_XML = AbstractWebAppContext.class.getPackage()
      .getName().replace('.', '/')
      + "/webdefault.xml";

  private static String[] configClasses = {
    "org.eclipse.jetty.webapp.WebInfConfiguration",
    "org.eclipse.jetty.webapp.WebXmlConfiguration",
    "org.eclipse.jetty.webapp.MetaInfConfiguration",
    "org.eclipse.jetty.webapp.FragmentConfiguration",
    // export the SipFactory in the jndi context (was "org.eclipse.jetty.plus.webapp.EnvConfiguration"
    "com.alcatel_lucent.as.service.jetty.common.webapp.ConvergentEnvConfiguration", 
    "org.eclipse.jetty.plus.webapp.PlusConfiguration",
    "org.eclipse.jetty.annotations.AnnotationConfiguration", 
    "org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
    // Deprecated in jetty-8, see META-INF/services/javax.servlet.ServletContainerInitializer
  /* "org.eclipse.jetty.webapp.TagLibConfiguration" */ };

  private static final String[] DEFAULT_PROTECTED_OSGI_TARGETS = {"/osgi-inf", "/osgi-opts"};

  protected SessionHandler _sessionManager;
  private AbstractContextDeployer.WebAppDeplCtx _ctx;

  // Convergence
  private javax.servlet.sip.SipFactory _sipfactory;
  private javax.servlet.ServletContext _sipservletctx;
  private ConvergenceService.SessionManagement _css;
  @SuppressWarnings("unused")
  private ConvergenceService.EventPropagation _cse;

  private boolean _isConvergent = false;
  private String _convergentAppName;
  private String webAppName;
  private ServiceRegistry _serviceRegistry;
  private Map<String, ServletContextListener> listeners;
  private Map<String, HttpServlet> servlets;
  private Map<String, Filter> filters;

  protected boolean ha;

  protected abstract ApplicationMBeanFactory getApplicationMBeanFactory();  
  protected SessionHandler createSessionManager() {
    return createSessionManager(DEFAULT_SESSION_TIMEOUT);
  }
  protected abstract SessionHandler createSessionManager(int timeout);
  protected abstract String getProtocol();
  protected abstract boolean isNonOsgi();

  
  /**
   * constructor for simple webapp
   * using Configuration class for annotations
   */
  public AbstractWebAppContext(String webappName, Map<String, HttpServlet> servlets,
                               Map<String, Filter> filters, Map<String, ServletContextListener> listeners,
                               boolean ha) {
    this(webappName, servlets, filters, listeners, ha, DEFAULT_SESSION_TIMEOUT);
  }
  /**
   * constructor for simple webapp
   * using Configuration class for annotations
   */
  public AbstractWebAppContext(String webappName, Map<String, HttpServlet> servlets,
                               Map<String, Filter> filters, Map<String, ServletContextListener> listeners,
                               boolean ha, int timeout) {
    super(null, /* SessionHandler */
          null /* securityHandler */, 
          new ServletHandler(), 
          null /* errorPageHandler */);
    this.ha = ha;
    setSessionHandler(createSessionManager(timeout));

    if (Log.getRootLogger().isDebugEnabled())
      Log.getRootLogger().debug("Creating webapp ctx " + webappName);

    setSecurityHandler(newSecurityHandler());

    // setAttribute(AnnotationConfiguration.SERVLET_CONTAINER_INITIALIZER_ORDER, "org.apache.jasper.runtime.TldScanner, *");

    _scontext = new AsrServletContext();
    // set config classes for annotations
    setConfigurationClasses(configClasses);

    this._sessionManager = (SessionHandler) getSessionHandler();
    this.webAppName = webappName;
    this.setDisplayName(webappName);
    this.listeners = listeners;
    this.servlets = servlets;
    this.filters = filters;

    String[] targets = getProtectedTargets();
    String[] updatedTargets = null;
    if (targets != null) {
      updatedTargets = new String[targets.length+DEFAULT_PROTECTED_OSGI_TARGETS.length];
      System.arraycopy(targets, 0, updatedTargets, 0, targets.length);
    }
    else
      updatedTargets = new String[DEFAULT_PROTECTED_OSGI_TARGETS.length];
    System.arraycopy(DEFAULT_PROTECTED_OSGI_TARGETS, 0, updatedTargets, targets.length, DEFAULT_PROTECTED_OSGI_TARGETS.length);
    setProtectedTargets(updatedTargets);
  }
  
  /**
   * Constructor for convergent application
   */
  public AbstractWebAppContext(AbstractContextDeployer.WebAppDeplCtx ctx,
                               String convergentAppName, String webappName,
                               Map<String, HttpServlet> servlets, Map<String, Filter> filters,
                               Map<String, ServletContextListener> listeners) {
    this(ctx, convergentAppName, webappName, servlets, filters, listeners, DEFAULT_SESSION_TIMEOUT);
  }

  /**
   * Constructor for convergent application
   */
  public AbstractWebAppContext(AbstractContextDeployer.WebAppDeplCtx ctx,
                               String convergentAppName, String webappName,
                               Map<String, HttpServlet> servlets, Map<String, Filter> filters,
                               Map<String, ServletContextListener> listeners, int timeout) {
    this(webappName, servlets, filters, listeners, true, timeout);
    _isConvergent = true;
    _ctx = ctx;

    if (Log.getRootLogger().isDebugEnabled())
      Log.getRootLogger().debug("The webapp ctx is convergent");
    // The application is convergent so we use the convergent servlet
    // context that enables the dialog with the SipApplication
    _scontext = new ConvergentAsrServletContext();
    _convergentAppName = convergentAppName;
    // Log.debug("EventListener array: " + getEventListeners().toString());
    addEventListener(new InternalCtxListener());
  }

  /**
   * Set the temp directory for the webapp
   * 
   * @param tmpDirBase
   *            Dir basename
   * @param id
   *            bundle id
   * @param webAppName
   *            webapp nane
   * @param contextPath
   *            context path
   * @throws IOException
   */
  public void setTempDirectory(File tmpDirBase, long id, String webAppName,
                               String contextPath) throws IOException {
    String path = contextPath.replace('/', '_');
    path = path.replace('.', '_');
    path = path.replace('\\', '_');
    StringBuffer buffer = new StringBuffer();
    buffer.append(tmpDirBase.getCanonicalPath());
    buffer.append(File.separatorChar);
    buffer.append(id);
    buffer.append(NAME_SEPARATOR);
    buffer.append(webAppName);
    buffer.append(NAME_SEPARATOR);
    buffer.append(path);
    File tmpDir = new File(buffer.toString());
    if (tmpDir.exists()) {
      IO.delete(tmpDir);
    }
    setTempDirectory(tmpDir);
  }

  @Override
  public String getDefaultsDescriptor() {
    return DEFAULT_WEB_XML;
  }

  @Override
  protected void doStart() throws Exception {
    Log.getRootLogger().warn("Deploy  " + this);
    // start ...
    super.doStart();
  }

  @Override
  public void setServer(Server server)
  {
    if (server != null)
    {
      updateBean(null, getSessionHandler());
      updateBean(null, getServletHandler());
    }
    super.setServer(server);
  }

  @Override
  public boolean isServerClass(String name)
  {
    boolean res = super.isServerClass(name);
    // FIXME when WebSocket will be standardized
    if (isNonOsgi() && res && name.startsWith("org.eclipse.jetty.websocket")) {
      Log.getRootLogger().warn("override isServerClass to 'false' for " + name);
      res = false;
    }
    return res;
  }

  // ------------------- Mbean -----------------------
  public int getSessionsActive() {
    try {
    	return 0; // TODO
//      return _sessionManager.getSessions();
    } catch (Exception e) {
      return 0;
    }
  }

  public int getSessionsActiveMax() {
    try {
    	return 0; // TODO
//      return _sessionManager.getSessionsMax();
    } catch (Exception e) {
      return 0;
    }
  }

  public int getSessionTimeout() {
    try {
      return _sessionManager.getMaxInactiveInterval();
    } catch (Exception e) {
      return 0;
    }
  }

  // ------------------- DM Service callbacks -----------------------

  // Fake to avoid start method called by OSGi, because by defaut OSGi will
  // call a "start" method but this method is already existing and used by the
  // LifeCycle interface
  public void finishDeploy() {

  }

  protected void bindFactory(javax.servlet.sip.SipFactory sf) {
    if (Log.getRootLogger().isDebugEnabled())
      Log.getRootLogger().debug("bindFactory called " + sf);
    _sipfactory = sf;
  }

  protected void bindSipContext(javax.servlet.ServletContext sc) {
    if (Log.getRootLogger().isDebugEnabled())
      Log.getRootLogger().debug("bindSipContext called " + sc);
    _sipservletctx = sc;
  }

  protected void bindConvergentServiceSession(SessionManagement css) {
    _css = css;
  }

  protected void bindConvergentServiceSession(EventPropagation cse) {
    _cse = cse;
  }

  protected void bindServiceRegistry(ServiceRegistry sr) {
    _serviceRegistry = sr;
    // As we now have the Service Registry, we register the servlet context in
    // the OSGi white-board
    // This context will be used by the sip application to get data or propagate
    // events
    Hashtable<String, String> serviceProps = new Hashtable<String, String>() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      {
        put("name", webAppName);
        put("type", "http");
      }
    };

    _serviceRegistry.registerService(null,
                                     ServletContext.class.getName(), _scontext,
                                     serviceProps);
    if (Log.getRootLogger().isDebugEnabled())
      Log.getRootLogger().debug("Register HTTP ServletCtx for webapp " + webAppName);
  }

  // ------------------- Getter/Setter -----------------------

  public SipFactory getSipFactory() {
    return _sipfactory;
  }

  public ServletContext getSipServletContext() {
    return _sipservletctx;
  }

  public SipSessionsUtil getSipSessionsUtil() {
    return (SipSessionsUtil) _sipservletctx
        .getAttribute("javax.servlet.sip.SipSessionsUtil");
  }

  public SessionManagement getConvergentSessionManager() {
    return _css;
  }

  public void setConvergent() {
    _isConvergent = true;
  }

  public boolean isConvergent() {
    return _isConvergent;
  }

  public AbstractContextDeployer.WebAppDeplCtx getDplCtx() {
    return _ctx;
  }

  public String getWebAppName() {
    return webAppName;
  }

  public String getConvergentAppName() {
    return _convergentAppName;
  }

  private ServletContextListener getInstanciedListener(String name) {
    if (listeners != null)
      return listeners.get(name);
    return null;
  }

  private Servlet getInstanciedServlet(String name) {
    if (servlets != null) return servlets.get(name);
    return null;
  }

  private Filter getInstanciedFilter(String name) {
    if (filters != null) return filters.get(name);
    return null;
  }

  // ------------------- This listener allows to propagate an event to the
  // SipServlet context -----------------------
  /**
   * This class is a listener for ServletContext event that propagate the
   * events to the Sip servlet context of the convergent sip application
   */
  public class InternalCtxListener implements
  ServletContextAttributeListener, ServletContextListener {

    public void attributeAdded(ServletContextAttributeEvent event) {
      Log.getRootLogger().debug("InternalCtxListener.attributeAdded called");
      if (event.getSource() != _sipservletctx)
        ((ConvergenceService.EventPropagation) _sipservletctx)
        .propagateServletCtxAttributeEvent(event, "ADDED");
    }

    public void attributeRemoved(ServletContextAttributeEvent event) {
      Log.getRootLogger().debug("InternalCtxListener.attributeRemoved called");
      if (event.getSource() != _sipservletctx)
        ((ConvergenceService.EventPropagation) _sipservletctx)
        .propagateServletCtxAttributeEvent(event, "REMOVED");
    }

    public void attributeReplaced(ServletContextAttributeEvent event) {
      Log.getRootLogger().debug("InternalCtxListener.attributeReplaced called");
      if (event.getSource() != _sipservletctx)
        ((ConvergenceService.EventPropagation) _sipservletctx)
        .propagateServletCtxAttributeEvent(event, "REPLACED");
    }

    public void contextDestroyed(ServletContextEvent event) {
      Log.getRootLogger().debug("InternalCtxListener.contextDestroyed called");
      if (event.getSource() != _sipservletctx)
        ((ConvergenceService.EventPropagation) _sipservletctx)
        .propagateServletCtxEvent(event, "DESTROYED");
    }

    public void contextInitialized(ServletContextEvent event) {
      Log.getRootLogger().debug("InternalCtxListener.contextInitialized called");
      if (event.getSource() != _sipservletctx)
        ((ConvergenceService.EventPropagation) _sipservletctx)
        .propagateServletCtxEvent(event, "INITIALIZED");
    }

  }


  public class AsrServletContext extends org.eclipse.jetty.webapp.WebAppContext.Context {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T createInstance(Class<T> clazz) throws ServletException {
      Log.getRootLogger().info("createInstance " + clazz.getName() +
                               " " + EventListener.class.isAssignableFrom(clazz) +
                               " " + Filter.class.isAssignableFrom(clazz) +
                               " " + Servlet.class.isAssignableFrom(clazz) 
          );
      if (EventListener.class.isAssignableFrom(clazz)) {
        EventListener listener = getInstanciedListener(clazz.getName());
        if (listener != null) {
          Log.getRootLogger().info(webAppName + " createInstance ListenerAsService: Re-using " + listener);
          return (T) listener;
        }
      }
      return super.createInstance(clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException
    {
      EventListener listener = getInstanciedListener(clazz.getName());
      if (listener != null) {
        Log.getRootLogger().info(webAppName + " createListener ListenerAsService: Re-using " + listener);
        return (T) listener;
      }
      return super.createListener(clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Filter> T createFilter(Class<T> c) throws ServletException
    {
      Filter filter = getInstanciedFilter(c.getName());
      if (filter != null) {
        Log.getRootLogger().info(webAppName + " createFilter FilterAsService: Re-using " + filter);
        return (T) filter;
      }
      Log.getRootLogger().info(webAppName + " createFilter " + c.getName());
      return super.createFilter(c);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Servlet> T createServlet(Class<T> c) throws ServletException
    {
      Servlet servlet = getInstanciedServlet(c.getName());
      if (servlet != null) {
        Log.getRootLogger().info(webAppName + " createServlet ServletAsService: Re-using " + servlet);
      }
      else {  
        servlet = super.createServlet(c);
        Log.getRootLogger().info(webAppName + " createServlet " + servlet);
      }
      if (servlet != null) {
        registerServlet(servlet, c);
      }
      return (T) servlet;
    }

    @SuppressWarnings("rawtypes")
    private void registerServlet(Servlet servlet, Class c) {
      ApplicationMBeanFactory factory = getApplicationMBeanFactory();
      if (factory != null) {
        ServletHolder[] holders = AbstractWebAppContext.this.getServletHandler().getServlets();
        for (ServletHolder holder : holders)
        {
          String cname = holder.getClassName();
          if (cname.equals(c.getName())) {
            try {                
              ModelMBean mbean = factory.registerServlet(
                                                         webAppName, 
                                                         getProtocol(), 
                                                         servlet, 
                                                         null, // holder
                                                         holder.getName(),
                                                         this.getMajorVersion(),
                                                         this.getMinorVersion()
                  );
              this.setAttribute(ApplicationMBeanFactory.ATTR_PREFIX_MODEL_MBEAN + holder.getName(), mbean);
            } catch (JMException e) {
              Log.getRootLogger().warn("Could not create mbean for servlet " + holder.getName(), e);
            }                
            break;
          }
        }
      }
    }

    @Override
    public ClassLoader getClassLoader()
    {
      ClassLoader wacl = AbstractWebAppContext.this.getClassLoader();
      if (wacl == Thread.currentThread().getContextClassLoader()) return wacl;
      return super.getClassLoader();
    }

  }

  // ------------------- ServletContext implementation for Convergent Appli
  // -----------------------
  /**
   * Specific servlet context that allows to interrogate Sip servlet context
   * to retrieve parameters or attributes
   */
  public class ConvergentAsrServletContext extends AsrServletContext 
  implements ConvergenceService.EventPropagation, ConvergenceService.AttributesViewer {

    @Override
    public String getInitParameter(String name) {
      String tmp = super.getInitParameter(name);
      if (tmp == null)
        tmp = ((ConvergenceService.AttributesViewer) _sipservletctx)
        .getInternalInitParameter(name);
      return tmp;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Enumeration getInitParameterNames() {
      Vector v = new Vector();
      for (Enumeration tmp = super.getInitParameterNames(); tmp
          .hasMoreElements();) {
        v.add(tmp.nextElement());
      }
      for (Enumeration tmp = ((ConvergenceService.AttributesViewer) _sipservletctx)
          .getInternalInitParameterNames(); tmp.hasMoreElements();) {
        v.add(tmp.nextElement());
      }

      return v.elements();
    }

    @Override
    public synchronized Object getAttribute(String name) {
      Object tmp = super.getAttribute(name);
      if (tmp == null)
        tmp = ((ConvergenceService.AttributesViewer) _sipservletctx)
        .getInternalAttribute(name);
      return tmp;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public synchronized Enumeration getAttributeNames() {
      Vector v = new Vector();
      for (Enumeration tmp = super.getAttributeNames(); tmp
          .hasMoreElements();) {
        v.add(tmp.nextElement());
      }
      for (Enumeration tmp = ((ConvergenceService.AttributesViewer) _sipservletctx)
          .getInternalAttributeNames(); tmp.hasMoreElements();) {
        v.add(tmp.nextElement());
      }

      return v.elements();
    }

    @Override
    public synchronized void removeAttribute(String name) {
      super.removeAttribute(name);
    }

    @Override
    public synchronized void setAttribute(String name, Object value) {
      super.setAttribute(name, value);
    }

    // API to propagate the events that have been generated on the
    // ServletContext of the SIP application
    @Override
    public void propagateServletCtxAttributeEvent(
                                                  ServletContextAttributeEvent event, String method) {
      Log.getRootLogger().debug("propagateServletCtxAttributeEvent called method: "
          + method);
      ClassLoader callerloader = Thread.currentThread()
          .getContextClassLoader();
      Thread.currentThread().setContextClassLoader(getClassLoader());
      Log.getRootLogger().debug("Class Loader: "
          + Thread.currentThread().getContextClassLoader());
      try {
        EventListener[] eventListeners = getEventListeners();
        for (int i = 0; eventListeners != null
            && i < eventListeners.length; i++) {
          EventListener listener = eventListeners[i];

          if (listener instanceof ServletContextAttributeListener) {
            // Log.debug("Got a listener to call");
            if ("REMOVED".equalsIgnoreCase(method)) {
              ((ServletContextAttributeListener) listener)
              .attributeRemoved(event);
            } else if ("REPLACED".equalsIgnoreCase(method)) {
              ((ServletContextAttributeListener) listener)
              .attributeReplaced(event);
            } else if ("ADDED".equalsIgnoreCase(method)) {
              ((ServletContextAttributeListener) listener)
              .attributeAdded(event);
            }
          }
        }
      } finally {
        Thread.currentThread().setContextClassLoader(callerloader);
      }
    }

    @Override
    public void propagateServletCtxEvent(ServletContextEvent event,
                                         String method) {
      Log.getRootLogger().debug("propagateServletCtxEvent called method: " + method);
      ClassLoader callerloader = Thread.currentThread()
          .getContextClassLoader();
      Thread.currentThread().setContextClassLoader(getClassLoader());

      Log.getRootLogger().debug("Class Loader: "
          + Thread.currentThread().getContextClassLoader());
      try {

        EventListener[] eventListeners = getEventListeners();
        for (int i = 0; eventListeners != null
            && i < eventListeners.length; i++) {
          EventListener listener = eventListeners[i];

          if (listener instanceof ServletContextListener) {
            // Log.debug("Got a listener to call");
            if ("INITIALIZED".equalsIgnoreCase(method)) {
              ((ServletContextListener) listener)
              .contextInitialized(event);
            } else if ("DESTROYED".equalsIgnoreCase(method)) {
              ((ServletContextListener) listener)
              .contextDestroyed(event);
            }
          }

        }
      } finally {
        Thread.currentThread().setContextClassLoader(callerloader);
      }
    }

    @Override
    public String getInternalInitParameter(String name) {
      return super.getInitParameter(name);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getInternalInitParameterNames() {
      return super.getInitParameterNames();
    }

    @Override
    public synchronized Object getInternalAttribute(String name) {
      return super.getAttribute(name);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public synchronized Enumeration getInternalAttributeNames() {
      return super.getAttributeNames();
    }

  }

}
