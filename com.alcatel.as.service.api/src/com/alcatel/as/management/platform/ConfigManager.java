// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.management.platform;

import java.util.Map;
import java.util.List;
import java.util.jar.JarFile;
import java.net.JarURLConnection;

import org.json.JSONObject;

import com.alcatel.as.management.blueprint.Blueprint;
import com.alcatel.as.service.metatype.InstanceProperties;
import com.alcatel.as.service.metatype.PropertiesDescriptor;

/**
 * ASR configuration service, used to read and set all components properties and deployment information.
 */
public interface ConfigManager 
{

  /**
   * all modifications are done via a component level transaction
   */
  Tx txBegin(String component_path) throws Exception ;

  /**
   * component level transaction
   */
  public interface Tx 
  {
    String component();

    void commit(String msg) throws Exception ;
    void rollback(Throwable t) throws Exception ;

    /**
     * set the component blueprint during a create or update transaction
     */
    Tx setBlueprint(Blueprint bp);
    /**
     * return the current blueprint if set in the transaction (or null)
     */
    Blueprint blueprint();

    /**
     * Instanciate the config for one bundle
     * 
     * @param jar The jar containing configuration
     * @return map of pid to InstanceProperties
     * @throws Exception
     */
    Map<String, InstanceProperties> registerComponentBundle(JarURLConnection jar) throws Exception;

    /**
     * Add a new instance of this component
     * 
     * @param instance_name The instance name 
     * @return map of pid to InstanceProperties
     * @throws Exception
     */
    Map<String, InstanceProperties> addInstance(String instance_name) throws Exception;

    /**
     * Remove an instance of this component
     * 
     * @param instance_name The instance name 
     * @return map of pid to InstanceProperties (after removal)
     * @throws Exception
     */
    Map<String, InstanceProperties> removeInstance(String instance_name) throws Exception;

    /**
     * Set property value for one or all instances of the current component
     * 
     * @param key pid/name[/instance]
     */
    Tx setProperty(String key, String value) throws Exception;

    /**
     * batchConfigure operation for the current component
     * Takes a JSON input like:
     * 
     * <pre>
     * { 
     *   "pid1": {
     *      "k1": { "*": "v1" },
     *      "k2": { "i2": "v2", "i3": "v3" }
     *      "k3": { "*": "/tmp/filedata" }
     *   },
     *   "pid2": { ... }
     * }
     * </pre>
     * 
     * and merge with current known properties.
     * @param source The jar file from which the descriptor was extracted, or null
     * @param json The json string to parse
     * @return a list of warnings about unknown properties
     * @throws Exception
     */
    List<String> batchConfigure(JarFile source, JSONObject json) throws Exception;

    /**
     * Checks existance of a configuration property
     * 
     * @param key pid/name[/instance]
     * @return true if the property exists
     */
    boolean hasProperty(String key) ;

    /**
     * Get property value for one or all instances of a component
     * 
     * @param key pid/name[/instance]
     */
    String getProperty(String key) throws Exception;

    /**
     * @return <pid:InstanceProperties>
     */
    Map<String, InstanceProperties> lookupProperties()
      throws Exception;
  }

  /**
   * Checks existance of a configuration property
   * 
   * @param key p/g/c/pid/name[/instance]
   * @return true if the property exists
   */
  boolean hasProperty(String key) ;

  /**
   * Get property value for one or all instances of a component
   * 
   * @param key p/g/c/pid/name[/instance]
   */
  String getProperty(String key) throws Exception;

  /**
   * @param key p/g/c[/pid[/name]]
   * @return <pid:InstanceProperties>
   */
  Map<String, InstanceProperties> lookupProperties(String key)
    throws Exception;

  /**
   * @param key
   *            bsn-bv[/pid[/name]]
   * @return <pid:PropertiesDescriptor>
   */
  Map<String, PropertiesDescriptor> lookupDescriptors(String key)
    throws Exception;

  /**
   * Returns runtime properties merged with their descriptors
   * 
   * @param key  p/g/c[/pid[/name]]
   * @return <pid:InstanceProperties>
   */
  Map<String, InstanceProperties> lookupPropertiesWithDescriptors(String key)
    throws Exception;

  /**
   * find other components using a specific pid/property within the given scope
   *
   * @param scope p[/g[/c]]
   * @param pid the searched pid
   * @param name the searched property (or null if pid only matters)
   * @return a Map of (pgc -> (i -> value)) for all pgc containing the property
   */
  Map<String,Map<String,String>> findInScope(final String scope, final String pid, final String name)
    throws Exception;

  /**
   * Set property value for one or all instances of a component
   * 
   * @param key pid/name[/instance]
   */
  ConfigManager setProperty(String key, String value) throws Exception;

  /**
   * Takes a JSON input string like:
   * 
   * <pre>
   * { "p1/g1/c1": {
   *      "pid1": {
   *         "k1": { "*": "v1" },
   *         "k2": { "i2": "v2", "i3": "v3" }
   *         "k3": { "*": "/tmp/filedata" }
   *      }
   *    },
   *   "p2/g2/c2": { ... }
   *    ...
   * }
   * </pre>
   * 
   * and merge with current known properties.
   * @param source The jar file from which the descriptor was extracted, or null
   * @param json The json string to parse
   * @return a list of warnings about unknown properties
   * @throws Exception
   */
  List<String> batchConfigure(JarFile source, String json) throws Exception;

  /**
   * Same as previous but with a JSON object
   * 
   * @param source The jar file from which the descriptor was extracted, or null
   * @param json The JSON object
   * @return a list of warnings about unknown properties
   * @throws Exception
   */
  List<String> batchConfigure(JarFile source, JSONObject json) throws Exception;

  /**
   * Dumps a component config in "batchConfigure" format.
   * 
   * @param component_path The component path
   * @return The JSON representation
   * @throws Exception
   */
  JSONObject dumpBatchConfiguration(String component_path) throws Exception;

  /**
   * Destroy a registered component's configuration
   * 
   * @param component_path The component path
   * @throws Exception
   */
  ConfigManager destroyComponent(String component_path) throws Exception;

  /**
   * Return a map of all deployments
   * 
   * @return The deployments
   * @throws Exception
   */
  Deployments getDeployments() throws Exception;

  /**
   * Refresh the map
   * 
   * @throws Exception
   */
  ConfigManager refresh() throws Exception;
}
