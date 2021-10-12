// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.agent;

import alcatel.tess.hometop.gateways.concurrent.ThreadPool;

import com.nextenso.mux.MuxContext;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.event.MuxMonitor;

public class MuxContextImpl implements MuxContext {
  private static ThreadPool threadPool;
  private MuxMonitorImpl monitor;
  
  public static void init(ThreadPool tp) {
    threadPool = tp;
  }
  
  public MuxContextImpl() {
  }
  
  public void setMuxHandler(MuxHandler handler) {
    this.monitor = new MuxMonitorImpl(handler);
  }
  
  public int getMajorVersion() {
    return AgentConstants.AGENT_VERSION >> 16;
  }
  
  public int getMinorVersion() {
    return AgentConstants.AGENT_VERSION & 0xFFFF;
  }
  
  public int getAppId() {
    return AgentConstants.AGENT_APP_ID;
  }
  
  public String getAppName() {
    return AgentConstants.AGENT_APP_NAME;
  }
  
  public String getInstanceName() {
    return AgentConstants.AGENT_INSTANCE;
  }
  
  public String getHostName() {
    return AgentConstants.AGENT_HOSTNAME;
  }
  
  public int getPid() {
    return AgentConstants.AGENT_PID;
  }
  
  public String getPlatformUid() {
    return AgentConstants.PLATFORM_UID;
  }
  
  public String getAgentUid() {
    return String.valueOf(AgentConstants.AGENT_UID);
  }
  
  public String getGroupUid() {
    return String.valueOf(AgentConstants.GROUP_UID);
  }
  
  public String getGroupName() {
    return AgentConstants.AGENT_GROUP;
  }
  
  public ThreadPool getThreadPool() {
    return threadPool;
  }
  
  public MuxMonitor getMuxMonitor() {
    return monitor;
  }
}
