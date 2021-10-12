// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.agent.web.container.deployer;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Filter;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.util.log.Log;
import org.osgi.framework.Bundle;

import com.alcatel.as.service.appmbeans.ApplicationMBeanFactory;
import com.alcatel.as.session.distributed.SessionManager;
import com.alcatel_lucent.as.agent.web.container.webapp.WebAppContext;
import com.alcatel_lucent.as.service.jetty.common.deployer.AbstractContextDeployer;
import com.alcatel_lucent.as.service.jetty.common.utils.JettyUtils;

public class ContextDeployer extends AbstractContextDeployer
{

  public static final String PROTOCOL = "Web";
  
  private ApplicationMBeanFactory factory;
  private int timeout;
  private SessionManager mgr;
  private ConcurrentHashMap<Long, WebAppContext> contexts = new ConcurrentHashMap<Long, WebAppContext>();

  public ContextDeployer(ApplicationMBeanFactory factory, int timeout, SessionManager mgr) {
    super(PROTOCOL);
    this.factory = factory;
    this.timeout = timeout;
    this.mgr = mgr;
  }

  @Override
  protected ApplicationMBeanFactory getApplicationMBeanFactory() {
    return factory;
  }

  @Override
  public void deploy(Bundle bundle, String contextPath, File tmpDirBase,
                     Map<String, HttpServlet> servlets, Map<String, Filter> filters,
                     Map<String, ServletContextListener> listeners) throws IOException {
    WebAppContext webAppContext = null;
    try {
      webAppContext = new WebAppContext(getWebappName(bundle), servlets, filters, listeners, factory, timeout, mgr);
      contexts.put(bundle.getBundleId(), webAppContext);
      JettyUtils.addWebAppContextXmlConfiguration(webAppContext);
    } catch (Exception e) {
      Log.getRootLogger().debug(e);
      IOException ioe = new IOException(e.getMessage());
      ioe.initCause(e);
      throw ioe;
    }
    doDeploy(webAppContext, bundle, contextPath, tmpDirBase);
  }

  public void undeploy(Bundle bundle, String contextPath) throws IOException {
    contexts.remove(bundle.getBundleId());
    if (contextPath == null) {
      super.undeploy(bundle);
    }
    else {
      super.undeploy(contextPath);
    }    
  }
  
  public int getSessionsActive() {
    int nb = 0;
    for(WebAppContext context : contexts.values()) {
//      nb += ((AbstractSessionManager) context.getSessionHandler().getSessionManager()).getSessions();
    }    
    return nb;
  }

}
