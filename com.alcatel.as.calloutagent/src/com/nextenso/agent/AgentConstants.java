// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.agent;

public class AgentConstants {
  
  /**
   * The Attribute name containing the protocols deployed in the CalloutAgent.
   * <br/>The value is a StringArrayValue.
   */
  public static final String SESSION_ATT_PROTOCOLS = "protocols";
  public static String[] PROTOCOLS;
  
  /**
   * The Attribute name containing the agent's host name.
   * <br/>The value is a StringValue.
   */
  public static final String SESSION_ATT_AGENT_HOSTNAME = "hostname";
  public static String AGENT_HOSTNAME;
  
  /**
   * The Attribute name containing the agent's pid.
   * <br/>The value is an IntValue.
   */
  public static final String SESSION_ATT_AGENT_PID = "pid";
  public static int AGENT_PID;
  
  /**
   * The Attribute name containing the agent's group name.
   * <br/>The value is a StringValue.
   */
  public static final String SESSION_ATT_AGENT_GROUP = "group";
  public static String AGENT_GROUP;
  
  /**
   * The Attribute name containing the agent's version (major in the upper 16 bits, minor in the lower 16).
   * <br/>The value is an IntValue.
   */
  public static final String SESSION_ATT_AGENT_VERSION = "version";
  public static int AGENT_VERSION = ((3 << 16) | 0); // version 3, release 0;
  
  public static String AGENT_APP_NAME;
  public static int AGENT_APP_ID;
  public static String AGENT_INSTANCE;
  public static long AGENT_UID;
  public static long GROUP_UID;
  public static String PLATFORM_UID;
}
