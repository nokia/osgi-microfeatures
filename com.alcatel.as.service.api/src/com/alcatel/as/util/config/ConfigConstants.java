// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.config;

/**
 * Constants used for system wide configurations.
 */
public interface ConfigConstants {
  /**
   * Represents a component property which by convention indicates if the component configuration is enabled or not.
   * When a Component declares a property which names matches this constant value, and if the default value is "false", it 
   * means that the configuration won't be created at all, in order to block the component activation. And from the webadmin,
   * if you set the property to true, then the configuration will be dynamically created in order to unblock the activation of the
   * component.
   */
  final static String COMPONENT_ENABLED = "com.alcatel.as.service.configuration.component.operator-enabled"; 

  /**
   * represents the root directory of the ASR installation
   */
  final static String INSTALL_DIR = "INSTALL_DIR";  

  /**
   * service.pid identifier for the current process system configuration
   */
  final static String SYSTEM_PID = "system";

  /**
   * part of the system configuration:
   * the "platform" represents a high level application encompassing one or more groups
   */
  final static String PLATFORM_NAME = "platform.name";

  /**
   * part of the system configuration:
   * a numerical identifier for the "platform"
   */
  final static String PLATFORM_ID = "platform.id";

  /**
   * part of the system configuration:
   * the "group" represents a high level application unit comprised of IO handlers and scalable processing nodes. 
   * the "group" is also the logical scope boundary for distributed sessions.
   * processing agents and IO handlers only connect within their group.
   * a group may spawn accross several hardware nodes. the same hardware nodes may be used by several groups.
   */
  final static String GROUP_NAME = "group.name";

  /**
   * part of the system configuration:
   * a numerical identifier for the "group"
   */
  final static String GROUP_ID = "group.id";

  /**
   * part of the system configuration:
   * the "component" represents a type of process which may be identically instantiated several times for scalability.
   */
  final static String COMPONENT_NAME = "component.name";

  /**
   * part of the system configuration:
   * a numerical identifier for the "component"
   */
  final static String COMPONENT_ID = "component.id";

  /**
   * part of the system configuration:
   * it represents the current "instance" of the parent component.
   */
  final static String INSTANCE_NAME = "instance.name";

  /**
   * part of the system configuration:
   * a numerical identifier for the "instance"
   */
  final static String INSTANCE_ID = "instance.id";

  /**
   * part of the system configuration:
   * the process id assigned by the OS. 
   */
  final static String INSTANCE_PID = "instance.pid";

  /**
   * part of the system configuration:
   * the local host name
   */
  final static String HOST_NAME = "host.name";

    /**
     * part of the system configuration:
     * the external ip where the admin is running
     */
    final static String ADMIN_EXTERNAL_IP = "environment.EXTERNAL_IP";

    /**
     * part of the system configuration:
     * the external ip where the admin is running
     */
    final static String CLUSTER_NAME = "environment.CLUSTER_NAME";

    /**
     * NOT part of the system configuration, internal use.
     * The type of middleware used to install this cluster (one of: mCas, nff, legacy)
     */
    final static String ADMIN_MIDDLEWARE_TYPE = "middleware.type";

    /**
     * part of the system configuration:
     * the internal ip where the admin is running
     */
    final static String ADMIN_INTERNAL_IP = "environment.INTERNAL_IP";

  /**
   * NOT part of the system configuration.
   */
  final static String PROTOCOL_NAME = "protocol.name";

  /**
   * NOT part of the system configuration.
   * a "module" represents a software entity within a process.
   */
  final static String MODULE_NAME = "module.name";

  /**
   * NOT part of the system configuration, internal use.
   * this id comes from a predefined list of well known logical entities.
   */
  final static String MODULE_ID = "module.id";

  /**
   * NOT part of the system configuration, internal use.
   * IP of an advertised network service
   */
  final static String SERVICE_IP = "service.ip";

  /**
   * NOT part of the system configuration, internal use.
   * port of an advertised network service
   */
  final static String SERVICE_PORT = "service.port";

  /**
   * NOT part of the system configuration, internal use.
   */
  final static String SERVICE_NEEDED_FEATURE = "service.neededFeature";

  /**
   * NOT part of the system configuration, internal use.
   */
  final static String SERVICE_INTERNAL_TRANSPORT = "service.internal.transport";

  /**
   * NOT part of the system configuration, internal use.
   */
  final static String SERVICE_EXTERNAL_TRANSPORT = "service.external.transport";

  /**
   * NOT part of the system configuration, internal use.
   */
  final static String STANDBY_MODE = "standby.mode";

    /**
     * NOT part of the system configuration, internal use.
     */
    final static String WEBAPP_ADVERT_TYPE = "webapp.advert.type";

    /**
     * NOT part of the system configuration, internal use.
     */
    final static String WEBAPP_ADVERT_URL = "webapp.advert.url";

    /**
     * NOT part of the system configuration, internal use.
     * The service type used to discover ASR services
     */
    final static String ADVERT_SERVICE_TYPE = "_asrsrv._tcp";

    /**
     * NOT part of the system configuration, internal use.
     * The service type used to discover ASR webapp
     * Remote webapp dans notre jargon !
     */
    final static String ADVERT_WEBAPP_TYPE = "_asrWebApp._tcp";

    /**
     * NOT part of the system configuration, internal use.
     * The service type used to discover Proxy Webb
     * Mixte mode. Top webapp inside the Jetty, some contents need to be proxied
     */
    final static String ADVERT_PROXY_TYPE = "_asrProxy._tcp";

    /**
     * NOT part of the system configuration, internal use.
     * The service type used to discover ASR cluster
     */
    final static String ADVERT_CLUSTER_TYPE = "_asrAdmin._tcp";

    /**
     * NOT part of the system configuration, internal use.
     * The key used for service type. Default is ADVERT_SERVICE_TYPE
     */
    final static String ADVERT_TYPE = "advert.type";
}
