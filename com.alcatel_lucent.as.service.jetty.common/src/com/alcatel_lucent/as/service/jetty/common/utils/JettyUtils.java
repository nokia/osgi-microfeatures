// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.jetty.common.utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.felix.http.base.internal.AbstractHttpActivator;
import org.apache.felix.http.base.internal.DispatcherServlet;
import org.apache.felix.http.base.internal.EventDispatcher;
import org.apache.felix.http.base.internal.HttpServiceController;
import org.apache.log4j.Logger;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.providers.WebAppProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.Slf4jRequestLog;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Slf4jLog;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel_lucent.as.service.jetty.common.connector.AbstractBufferEndPoint;
import com.alcatel_lucent.as.service.jetty.common.connector.BufferConnector;
import com.alcatel_lucent.as.service.jetty.common.connector.EndPointManager;
import com.alcatel_lucent.as.service.jetty.common.connector.RequestCustomizer;
import com.alcatel_lucent.as.service.jetty.common.handler.HandlerHelper;
import com.alcatel_lucent.as.service.jetty.common.handler.HttpServiceErrorHandler;
import com.alcatel_lucent.as.service.jetty.common.handler.JwcDefaultHandler;
import com.alcatel_lucent.as.service.jetty.common.threadpool.ThreadPool;

public class JettyUtils {
  
  private final static int DEF_CONNECTOR_MAXIDLETIME = 3600; // seconds
  private final static int DEF_SCAN_INTERVAL = 60;
  private final static int DEF_SESSION_TIMEOUT = 900;
  
  static private void setSystemProperties() {
    // Force property for JNDI
    System.setProperty("alcatel_lucent.jndi.throwNameNotFound","true");
    // Force JSP compilation with the eclipse compiler (because the JRE compiler cannot find needed packages)
    System.setProperty("org.apache.jasper.compiler.disablejsr199", Boolean.TRUE.toString());   
  }

  static public void setLogger(Logger logger) {
    // 1) switch STDERR to avoid Jetty error message while instantiating the class "Log"
    PrintStream ps = System.err;
    System.setErr(new PrintStream(new NullOutputStream()));
    // 2) set our logger
    try {
      Log.setLog(new Slf4jLog());
    }
    catch (Exception ignored) {
    }
    // 3) restore STDERR
    finally {      
      System.setErr(ps);
    }
  }

  static public File getTmpDir(String relativePath) {
    String dir = System.getenv("INSTALL_DIR");
    if (dir == null) {
        dir = "/tmp/";
    }
    else {
        dir += "/var/tmp/";
    }
    dir += relativePath;
    File tmpdir = new File(dir);
    if (!tmpdir.exists()) {
        if (!tmpdir.mkdirs()) return null;
        if (!tmpdir.isDirectory()) return null;
        if (!tmpdir.canWrite()) return null;
       tmpdir.deleteOnExit();
    }
    return tmpdir;
  }
  
  static public Server createServer(ScheduledExecutorService executor, TimerService timerService, String mainPackage) {
    setSystemProperties();
    Server server = new Server(new ThreadPool(executor, timerService));
    server.setStopAtShutdown(true);
    server.setStopTimeout(0);
    MBeanServer mBeanServer = JettyUtils.getMBeanServer();
    if (mBeanServer != null) {
      // remove previous MBEANs
      cleanUpMbeans(mainPackage);
      // Before other configuration : Add JMX management 
      // (otherwise some MBEANs are not created)
      server.addBean(new MBeanContainer(mBeanServer));
    }    
    return server;
  }
  
  static public void addXmlConfiguration(Server server, String xml) throws Exception {
    XmlConfiguration configuration = new XmlConfiguration(xml);
    configuration.configure(server);
  }

  static public void addWebAppContextXmlConfiguration(WebAppContext webAppContext) throws Exception {
	  JettyWebXmlConfiguration configuration = new JettyWebXmlConfiguration();
	  configuration.configure(webAppContext);
  }

  static public BufferConnector createConnector(Server server, EndPointManager mgr, int outputBufferSize, int requestHeaderSize, int responseHeaderSize) {
      return createConnector(server, mgr, outputBufferSize, requestHeaderSize, responseHeaderSize, -1 /* non blocking */);
  }
    
  static public BufferConnector createConnector(Server server, EndPointManager mgr, int outputBufferSize, int requestHeaderSize, int responseHeaderSize, long blockingTimeout) {
    HttpConfiguration http_config = new HttpConfiguration();
    http_config.setSecureScheme("https");
    http_config.setSecurePort(8443);
    http_config.setOutputBufferSize(outputBufferSize);
    http_config.setRequestHeaderSize(requestHeaderSize);
    http_config.setResponseHeaderSize(responseHeaderSize);
    http_config.setSendServerVersion(false);
    http_config.setSendDateHeader(true);
    http_config.addCustomizer(new RequestCustomizer());
    http_config.setBlockingTimeout(blockingTimeout);
    
    // TODO add specific http2 configuration (in xml)
    
    BufferConnector connector = new BufferConnector(mgr, server, new HttpConnectionFactory(http_config), new HTTP2CServerConnectionFactory(http_config));
    connector.setIdleTimeout(DEF_CONNECTOR_MAXIDLETIME*1000);
    server.addConnector(connector);
    return connector;
  }
  
  static private MBeanServer getMBeanServer() {    
    return ManagementFactory.getPlatformMBeanServer();
  }  
  
  @SuppressWarnings("rawtypes")
  static public void cleanUpMbeans(String mainPackage) {
    MBeanServer mBeanServer = getMBeanServer();
    // remove all registered MBEANs during previous test case
    String jettyMainPackage = Server.class.getPackage().getName();
    String[] domains = mBeanServer.getDomains();
    for (int i = 0; i < domains.length; i++) {
      if ((domains[i].startsWith(mainPackage)) || (domains[i].startsWith(jettyMainPackage))) {
        try {
          Set mbeans = mBeanServer.queryNames(new ObjectName(domains[i] + ":*"), null);
          for (Iterator iter = mbeans.iterator(); iter.hasNext();) {
            ObjectName element = (ObjectName) iter.next();
            mBeanServer.unregisterMBean(element);
          }
        } catch (Exception e) { }
      }
    }
  }
  
  public static StatisticsHandler buildHandlers(ContextHandlerCollection chc, String logger, 
                                                ServletContextHandler httpServiceContext, HandlerHelper handlerHelper) {
    int nbHandlers = 3;
    if (httpServiceContext != null) nbHandlers++;
    RequestLogHandler rlh = new RequestLogHandler();
    rlh.setRequestLog(new AsrSlf4jRequestLog());
    // handlers list ...
    int currentHandler = 0;
    Handler[] handlers = new Handler[nbHandlers];
    handlers[currentHandler++] = chc;
    if (httpServiceContext != null) handlers[currentHandler++] = httpServiceContext;
    handlers[currentHandler++] = new JwcDefaultHandler(handlerHelper);
    handlers[currentHandler++] = rlh;
    // ... in a handler collection ...
    HandlerCollection handlerCollection = new HandlerCollection();
    handlerCollection.setHandlers(handlers);
    // ... in the statistics handler
    StatisticsHandler statisticsHandler = new StatisticsHandler();
    statisticsHandler.setHandler(handlerCollection);
    return statisticsHandler;
  }
  public static ServletContextHandler createHttpService(Server server, BundleContext _bundleContext, HandlerHelper helper, 
      String path, SessionIdManager sessionIdManager) {
    return createHttpService(server, _bundleContext, helper, path, sessionIdManager, DEF_SESSION_TIMEOUT);
  }
  
  public static ServletContextHandler createHttpService(Server server, BundleContext _bundleContext, HandlerHelper helper, 
                                                        String path, SessionIdManager sessionIdManager, int timeout) {
    if (!path.startsWith("/")) path = "/" + path;
    ServletContextHandler httpServiceContext = new ServletContextHandler(server, path, ServletContextHandler.SESSIONS);
    httpServiceContext.setErrorHandler(new HttpServiceErrorHandler(helper));
    httpServiceContext.setClassLoader(Thread.currentThread().getContextClassLoader()); // FIXME ???
    httpServiceContext.getSessionHandler().setSessionIdManager(sessionIdManager);
    httpServiceContext.getSessionHandler().setMaxInactiveInterval(timeout);
    httpServiceContext.setDisplayName("HTTP Service");
    HttpActivator hsa = new HttpActivator();
    try {
      Hashtable<String, Object> hsProps = new Hashtable<String, Object>();
      hsProps.put("provider", "com.alcatel_lucent.as.jetty");
      hsa.start(_bundleContext); //registers HttpService
      hsa.controller().setProperties(hsProps);
    }
    catch (Exception e) {
      Log.getRootLogger().warn("cannot start HTTP service", e);
      return null;
    } 
    httpServiceContext.addEventListener(hsa.eventDispatcher());
    httpServiceContext.getSessionHandler().addEventListener(hsa.eventDispatcher());
    httpServiceContext.addServlet(new ServletHolder(hsa.dispatcher()), "/*");
    return httpServiceContext;
  }

  public static DeploymentManager createExternalContextProvider(Server server, ContextHandlerCollection chc, String contextDirName) {
    DeploymentManager manager = null;
    if ((contextDirName != null) && (contextDirName.trim().length()>0)) {
      File contextDir = new File(contextDirName);
      if (contextDir.exists() && contextDir.isDirectory() && contextDir.canRead()) {   
        manager = new DeploymentManager();
        manager.setContexts(chc);
        WebAppProvider provider = new WebAppProvider();
        provider.setMonitoredDirName(contextDirName);
        provider.setScanInterval(DEF_SCAN_INTERVAL);
        manager.addAppProvider(provider);
        server.addBean(manager);
      }
    }
    return manager;
  }

  //////// classes for HttpService ////////
  static class HttpActivator extends AbstractHttpActivator {
    public EventDispatcher eventDispatcher()  { return getEventDispatcher(); }
    public DispatcherServlet dispatcher()     { return getDispatcherServlet(); }
    public HttpServiceController controller() { return getHttpServiceController(); }
  }

  private static class NullOutputStream extends OutputStream {

    public NullOutputStream() {
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
    }

    @Override
    public void write(byte[] b) throws IOException {
    }

    @Override
    public void write(int b) throws IOException {
    }

  }
  
  private final static class AsrSlf4jRequestLog extends Slf4jRequestLog {

    @Override
    public void log(Request request, Response response) {
      if (response.getStatus() == HttpStatus.SWITCHING_PROTOCOLS_101) {
        EndPoint endPoint = request.getHttpChannel().getEndPoint();
        if (endPoint != null && (endPoint instanceof AbstractBufferEndPoint)) {
          ((AbstractBufferEndPoint) endPoint).prepareUpgrade();
        }
      }
      super.log(request, response);
    }
    
  }

}
