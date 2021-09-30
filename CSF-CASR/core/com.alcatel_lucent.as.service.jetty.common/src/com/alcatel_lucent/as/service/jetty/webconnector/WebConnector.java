package com.alcatel_lucent.as.service.jetty.webconnector;

// Jdk
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.alcatel_lucent.as.service.jetty.common.deployer.WebApplication;

/**
 * This interface provides a web container connector service.
 */
public interface WebConnector {
  /**
   * These configurations keys are passed as properties to the init method ...
   */
  final String CNF_HANDLER = "webconnector.handler";
  final String CNF_MBEANSERVER = "webconnector.mBeanServer";
  final String CNF_INTERNAL_ID = "webconnector.internalId";  
  final String CNF_TMP_DIR = "webconnector.tmp.dir";
  final String CNF_THREAD_POOL = "webconnector.threadpool";
  
  final String DYN_SESSION_TIMEOUT = "httpagent.session.tmout";
  
  // ---------- Lifecycle
  
  /**
   * Configure the web container connector.
   * @param properties The configuration which will initialize this web container (see all CNF_XXX parameters).
   */
  void init(Properties properties) throws IOException;
  
  /**
   * Update the configuration of the web container connector.
   * @param properties The updated configuration .
   */
  void update(Properties properties);

  /**
   * Shutdown the web container.
   */
  void shutdown();

  // ---------- Webapps
  
  /**
   * Deploy a war or context.xml file through the web container.
   * @param war the file to be deployed.
   */
  void deploy(File war) throws IOException;

  /**
   * Deploy a WebApplication through the web container.
   * @param webapp the WebApplication to be deployed.
   */
  void deploy(WebApplication webapp) throws IOException;

  /**
   * Deploy a WebApplication through the web container.
   * @param contextPath the name of the context of the webapp (ie : "/foo")
   * @param war the war to be deployed. 
   */
  void deploy(String contextPath, WebApplication webapp) throws IOException;

  /**
   * Undeploy a war from the web container.
   * @param war the war to be undeployed. 
   *	    If war instanceof File: --> by default context path = the war file name.
   *        If war instanceof Bundle: --> undeploy a OSGi bundle
   *        If war instanceof String: --> undeploy a context path (ie : "/foo")
   */
  void undeploy(Object war) throws IOException;

  // ---------- Channels
  
  /**
   * Setup  a fresh channel to the container which will be used when sending an http request.
   */
  WebEndPoint newChannel(Object clientContext, boolean secure, String remoteAddr);

  // ---------- Sessions

  /**
   * Notifies the web container about a new session creation.
   */
  void sessionCreated(WebSession session);

  /**
   * Notifies the web container about a session destruction.
   */
  void sessionDestroyed(String sessionId);

  /**
   * Notifies the web container about a session renewal.
   */
  void sessionDidActivate(String sessionId, String contextPath);
  
  /**
   * Notifies the web container about a session passivation.
   */
  void sessionWillPassivate(String sessionId);

  // ---------- Statistics

  /**
   * @return Number of webapps currently deployed.
   */
  public int getDeployments();

  /**
   * @return Number of sessions currently active.
   */
  public int getSessionsActive();

  /**
   * @return Get the number of requests handled by this web connector
   */
  public int getRequests();

  /**
   * @return Number of requests currently active.
   */
  public int getRequestsActive();

  /**
   * @return Maximum number of active requests
   */
  public int getRequestsActiveMax();

  /** 
   * @return Average duration of request handling in milliseconds 
   */
  public long getRequestsDurationAve();

  /** 
   * @return Get maximum duration in milliseconds of request handling
   */
  public long getRequestsDurationMax();

  /**
   * @return the number of dispatches, excluding active dispatches
   */
  public int getDispatched();

  /**
   * @return the current number of dispatches, including resumed requests
   */
  public int getDispatchedActive();
  
  /**
   * @return the number of requests handled, including resumed requests
   */
  public int getSuspends();

  /**
   * @return the number of requests currently suspended.
   */
  public int getSuspendsActive();

  /**
   * @return the number of requests that have been resumed
   */
  public int getResumes();

  /**
   * @return the number of requests that expired while suspended.
   */
  public int getExpires();

  /**
   * @return Get the number of responses with a 1xx status returned
   */
  public int getResponses1xx();

  /**
   * @return Get the number of responses with a 2xx status returned
   */
  public int getResponses2xx();

  /**
   * @return Get the number of responses with a 3xx status returned
   */
  public int getResponses3xx();

  /**
   * @return Get the number of responses with a 4xx status returned
   */
  public int getResponses4xx();

  /**
   * @return Get the number of responses with a 5xx status returned
   */
  public int getResponses5xx();

}
