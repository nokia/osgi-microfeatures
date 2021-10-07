package com.nextenso.proxylet.engine;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;

import com.nextenso.proxylet.ProxyletConfig;

public class ProxyletUtils {
  
  private final static Logger LOGGER = Logger.getLogger("calloutserver.util.proxylet");

  private static class PxletClientLicenseMgr  {
    
    private final static PxletClientLicenseMgr LICENSE_MANAGER = new PxletClientLicenseMgr();
    private final static String PX_CLIENT_SUFFIX = ".Output";
    
    // Contains the counters of the different licensed componants having performed a request
    // Key is the componant name suffixed by the keyword pxclient (componant_name".PxletClient") - Value is a Long corresponding to the counter value
    private static final Map<String, Long> COMPONENT_COUNTERS = new Hashtable<String, Long>(1);
    
    // Contains the licenses
    // Key is an Integer representing the licenseId - Value is the componant name (without PxletClient suffix)
    private final static Map<Integer, String> LICENCE_IDS = new Hashtable<Integer, String>(1);
    
    private PxletClientLicenseMgr() {
    }
    
    public static PxletClientLicenseMgr getPxletClientLicenseMgr() {
      return LICENSE_MANAGER;
    }
    
    /**
     * Method called by the different clients (HttpClient, SmsClient...) to
     * check if the given componant is allowed to send request.
     * 
     * @return true if the given componant is allowed to send request (http, sms
     *         or whatever). False if componantName is null.
     * @throw NoValidLicenseException if the given componant is not allowed to
     *        send request
     */
    public static boolean maySendRequest(String componentName) {
      return true;
    }
    
    /**
     * Method called by the different clients before sending a request in order
     * to update component counters.
     */
    public static void sendRequest(String componentName) {
    }
    
    /**
     * @see com.nextenso.licensemgr.interfaces.ThroughputListener#getThroughputCounter(java.lang.String)
     */
    public long getThroughputCounter(String key) {
      int size = COMPONENT_COUNTERS.size();
      if (size == 0) {
        return 0;
      }
      synchronized (COMPONENT_COUNTERS) {
        Long counter = COMPONENT_COUNTERS.get(key);
        if (counter != null) {
          return counter.longValue();
        }
        return 0;
      }
    }
  }
  
  public static boolean maySendRequest(String componentName) throws Exception {
    return true;
  }
  
  public static void sendRequest(String componentName) {
    if (componentName == null) {
      return;
    }
    PxletClientLicenseMgr.sendRequest(componentName);
  }
  
  /**
   * Gets Config object from a ProxyletConfig object
   */
  public static Config getConfig(ProxyletConfig cnf) {
    Config config = new Config();
    Enumeration enumer = cnf.getParameterNames();
    while (enumer.hasMoreElements()) {
      String name = (String) enumer.nextElement();
      config.setProperty(name, cnf.getStringParameter(name, null));
    }
    // we purge the changes
    try {
      config.notifyListeners();
    } catch (ConfigException e) {
      // cannot happen - no listeners
    }
    return config;
  }
  
  private static boolean IS_IN_LAUNCHER_MODE = false;
  
  public static void setIsInAgentMode(boolean isInAgentMode) {
    IS_IN_LAUNCHER_MODE = isInAgentMode;
  }
  
  public static boolean isInLauncherMode() {
    return (IS_IN_LAUNCHER_MODE || Boolean.getBoolean("agent.launcherMode"));
  }
  
  public static boolean isInAgentMode() {
    return (!isInLauncherMode());
  }
}
