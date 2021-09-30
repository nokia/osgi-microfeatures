package com.alcatel.as.management.platform;

import java.net.JarURLConnection;

public interface CreateInstancePlugin 
{
  /**
   * @return the configuration pid this plugin deals with, or "*" if any
   */
  String getPid();
  
  /**
   * @return the configuration property this plugin deals with, or "*" if any
   */
  String getProperty();
  
  /**
   * 
   * @param tx The current component's configuration transaction 
   * @return the plugin's stateful context
   * @throws Exception
   */
  Object begin(ConfigManager.Tx tx) throws Exception;

  /**
   * @param jar The jar file
   * @param tx The current component's configuration transaction 
   * @param context the plugin's stateful context
   * @throws Exception
   */
  void scan(JarURLConnection jar, ConfigManager.Tx tx, Object context) throws Exception;

  /**
   * @param tx The current component's configuration transaction 
   * @param context the plugin's stateful context
   * @throws Exception
   */
  void end(ConfigManager.Tx tx, Object context) throws Exception;

    /**
       @return true is this plugin can be called when updated component
     */
    boolean supportUpdate();
}
