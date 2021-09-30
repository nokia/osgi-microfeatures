package com.nextenso.proxylet.engine;

public class ProxyAppEnv {
  private String name;
  
  private String version;
  
  private ClassLoader classLoader;
  
  public ProxyAppEnv(String name, String version) {
    this.name = name;
    this.version = version;
  }
  
  public String getName() {
    return name;
  }
  
  public String getVersion() {
    return version;
  }
  
  public void setClassLoader(ClassLoader cl) {
    this.classLoader = cl;
  }
  
  public ClassLoader getClassLoader() {
    return classLoader;
  }
  
  public String getAppId() {
    return getId(name, version);
  }
  
  public final static String SEPARATOR = "_";
  
  /** Separator used to encode group/instance names */
  public static final String GROUP_PREFIX = "__";
  
  /** Reserved group for instances which do not belong to a group*/
  public static final String ADMIN_GROUP = ".";
  
  public static String getId(String name, String version) {
    StringBuilder sb = new StringBuilder(name);
    sb.append(SEPARATOR).append(version);
    return sb.toString();
  }
  
  public static String getNameFromId(String id) {
    if (id == null)
      return null;
    
    int index;
    
    if ((index = id.indexOf(SEPARATOR)) < 0)
      return id;
    else
      return id.substring(0, index);
  }
  
  public static String getVersionFromId(String id) {
    if (id == null)
      return null;
    
    int index;
    int begin;
    
    if ((index = id.indexOf(SEPARATOR)) < 0)
      return null;
    
    if ((begin = (index + SEPARATOR.length())) >= id.length())
      return null;
    else {
      return id.substring(begin);
    }
  }
  
  public static int getIntVersionFromId(String appId) {
    String sVersionId = getVersionFromId(appId);
    
    if (sVersionId == null)
      return 0;
    
    if (sVersionId.indexOf(".") != -1) {
      // transform dotted notation into int
      StringBuffer noDot = new StringBuffer(sVersionId.length());
      for (int i = 0; i < sVersionId.length(); i++) {
        char c = sVersionId.charAt(i);
        if ((c >= '0') && (c <= '9'))
          noDot.append(c);
      }
      sVersionId = noDot.toString();
    }
    
    try {
      return Integer.parseInt(sVersionId);
    } catch (NumberFormatException nfe) {
      return 0;
    }
  }
  
  /**
   Retrieve the group name from a encoded instance name

   @param encodedName Encoded instance name
   @return Group name
   */
  public static final String getGroupFromInstance(String instanceName) {
    if (instanceName == null || instanceName.equals("")) {
      return null;
    }
    int index = instanceName.indexOf(GROUP_PREFIX);
    if (index == -1) {
      return ADMIN_GROUP;
    }
    return instanceName.substring(0, index);
  }
}
