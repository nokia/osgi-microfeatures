// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.agent.web.container.webapp;

import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.HouseKeeper;
import org.eclipse.jetty.server.session.NullSessionDataStore;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionHandler;

import com.alcatel.as.service.appmbeans.ApplicationMBeanFactory;
import com.alcatel.as.session.distributed.SessionManager;
import com.alcatel_lucent.as.agent.web.container.deployer.ContextDeployer;
import com.alcatel_lucent.as.service.jetty.common.deployer.AbstractContextDeployer.WebAppDeplCtx;
import com.alcatel_lucent.as.service.jetty.common.webapp.AbstractWebAppContext;

public class WebAppContext extends AbstractWebAppContext {

  private ApplicationMBeanFactory factory;
  private static int webAppNumber;
  private boolean nonOsgi;
  private final static Logger _log = Logger.getLogger(WebAppContext.class);

  /**
   * constructor for external webapps
   */
  public WebAppContext() {
    this("webapp_" + (++webAppNumber), null, null, null, null, 0, null);
    nonOsgi = true;
  }

  public WebAppContext(WebAppDeplCtx webAppDeplCtx, String convergentAppName, String webAppName,
      Map<String, HttpServlet> servlets, Map<String, Filter> filters, Map<String, ServletContextListener> listeners,
      ApplicationMBeanFactory factory, int timeout) {
    super(webAppDeplCtx, convergentAppName, webAppName, servlets, filters, listeners, timeout);
    this.factory = factory;
  }

  public WebAppContext(String webappName, Map<String, HttpServlet> servlets, Map<String, Filter> filters,
      Map<String, ServletContextListener> listeners, ApplicationMBeanFactory factory, int timeout, SessionManager mgr) {
    super(webappName, servlets, filters, listeners, (mgr != null), timeout);
    this.factory = factory;
    SessionHandler sm = (SessionHandler) this.getSessionHandler();
  }

  @Override
  protected void doStart() throws Exception {
    super.doStart();
  }

  @Override
  protected ApplicationMBeanFactory getApplicationMBeanFactory() {
    return factory;
  }

  @Override
  protected SessionHandler createSessionManager(int timeout) {
    SessionHandler manager = new SessionHandler();
    DefaultSessionIdManager sessions = new DefaultSessionIdManager(this.getServer());
    if (this.getServer() != null) {
      this.getServer().setSessionIdManager(sessions);
    }

    SessionCache cache = new DefaultSessionCache(manager);
    cache.setSessionDataStore(new NullSessionDataStore());
    HouseKeeper housekeeper = new HouseKeeper();
    try {
      housekeeper.setIntervalSec(timeout);
    } catch (Exception e) {
      _log.error("could not set session cache timeout", e);
    }
    manager.setMaxInactiveInterval(timeout); // FIXME why doing this ?
    sessions.setSessionHouseKeeper(housekeeper);

    return manager;
  }

  @Override
  protected String getProtocol() {
    return ContextDeployer.PROTOCOL;
  }

  @Override
  protected boolean isNonOsgi() {
    return nonOsgi;
  }
  /*
   * private SessionHandler newSessionHandler() throws Exception { SessionHandler
   * h = new SessionHandler(); DefaultSessionCacheFactory cacheFactory = new
   * DefaultSessionCacheFactory();
   * cacheFactory.setEvictionPolicy(SessionCache.EVICT_ON_INACTIVITY);
   * SessionCache c = cacheFactory.getSessionCache(h); SessionDataStore s =
   * _storeFactory.getSessionDataStore(h); c.setSessionDataStore(s);
   * h.setSessionCache(c); return h; }
   * 
   * private SessionCache getSessionCache (SessionHandler handler) {
   * DefaultSessionCache cache = new DefaultSessionCache(handler);
   * cache.setEvictionPolicy(SessionCache.
   * cache.setSaveOnInactiveEviction(isSaveOnInactiveEvict());
   * cache.setSaveOnCreate(isSaveOnCreate());
   * cache.setRemoveUnloadableSessions(isRemoveUnloadableSessions()); return
   * cache; }
   */

}
