package com.nextenso.proxylet.engine;

import java.util.Dictionary;
import java.util.Observer;

import com.nextenso.proxylet.engine.criterion.Criterion;

public interface ProxyletApplication {
  public final static String CONTEXT_LISTENER = "context-listener";
  
  public final static String SESSION_LISTENER = "session-listener";
  
  public final static String REQUEST_LISTENER = "request-listener";
  
  public final static String RESPONSE_LISTENER = "response-listener";
  
  public final static String REQUEST_CHAIN = "request-chain";
  
  public final static String RESPONSE_CHAIN = "response-chain";
  
  boolean isEmpty();
  
  ClassLoader getClassLoader(String type, String clazz); // cf Context.loadListeners (proxyAppEnv.setClassLoader)
  
  String[] getListeners(String type); // cf Context.java (loadListeners/loadCommonListener/loadListener)
  
  //String getListenerReference(String type, String listenerClazz); // cf HttpProxyletContext.loadListeners()
  
  Object getListener(String type, String listenerClazz); // cf HttpProxyletContext.loadListeners()
  
  String getListenerGivenName(String type, String listenerClazz); // cf HttpProxyletContext.loadListeners()
  
  String getProxyAppName(String type, String clazz);
  
  String getProxyAppVersion(String type, String clazz);
  
  String[] getProxylets(String type); // see HttpProxyletContext.loadChains()
  
  String getProxyletName(String type, String pxletClass);
  
  String getProxyletDesc(String type, String pxletClass);
  
  Object getProxylet(String type, String pxletClass);
  
  //FIXME maybe more efficient with a Map
  Dictionary getProxyletParams(String type, String pxletClass); //see DiameterProxyletContext.loadChains()
  
  String getProxyletCriterionName(String type, String pxletClass);
  
  String getProxyletCriterionDesc(String type, String pxletClass);
  
  Criterion getProxyletCriterionValue(String type, String pxletClass);
  
  Dictionary getProperties();
  
  void observeProperties(Observer propertyObserver);
  
  /**
   * used by the agent to notify the deployer
   * that the application was deployed
   */
  void initDone();
  
  /**
   * Returns the application scope (null if application is unscoped).
   */
  String getScope();
}
