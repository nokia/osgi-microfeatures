package com.nextenso.proxylet.engine;

public class ProxyletConstants extends PlatformConstants {
  
  //
  // Container constants
  //
  public static final int MAJOR_VERSION = 1;
  public static final int MINOR_VERSION = 0;
  public static final String SERVER_INFO = "ProxyletEngine/v3.0";
  
  //
  // Config constants
  //
  public static final String CONFIG_CONTEXT_PREFIX = "pxlet.";
  public static final String CONFIG_ALL_CONTEXTS_PREFIX = CONFIG_CONTEXT_PREFIX + "0.";
  public static final String CONFIG_CONTAINER_GROUP = "pxlet.container.group";
  
  //
  // ProxyletConfig constants
  //
  public static final String PARAM_NAME_TYPE = "proxylet.type";
  
  public static final String PARAM_NAME_APP_NAME = "application.name";
  public static final String PARAM_NAME_APP_ID = "application.id";
  public static final String PARAM_NAME_APP_PID = "application.pid";
  public static final String PARAM_NAME_APP_INSTANCE = "application.instance";
  public static final String PARAM_NAME_APP_HOST = "application.host";
  public static final String PARAM_NAME_DB_HOST = "db.host";
  public static final String PARAM_NAME_DB_PORT = "db.port";
  public static final String PARAM_NAME_APP_GROUP = "application.group";
  public static final String PARAM_NAME_APP_PLATFORM_ID = "application.platformUID";
  public static final String PARAM_NAME_APP_INSTANCE_ID = "application.instanceUID";
  
  //
  // ProxyletContext attributes related to launcher
  //
  public static final String PARAM_NAME_LAUNCHER_MODE = "launcher.mode";
  public static final String PARAM_NAME_LAUNCHER_ARG = "launcher.arg";
  
  public static final String ATTR_NAME_DNS_RESULT = "dns.result.";
  
  /** This attributes is used to retrieve the log4j logger from a proxylet. */
  public static final String ATTR_NAME_LOGGER = "org.apache.log4j.Logger";
  
  /** This attributes is used to retrieve the callout thread pool from a proxylet. */
  public static final String ATTR_NAME_THREAD_POOL = "alcatel.tess.hometop.gateways.concurrent.ThreadPool";
  
  // 
  // FastCache instance key
  //
  public static final String ATTR_NAME_DBHANDLE = "db.handle";
}
