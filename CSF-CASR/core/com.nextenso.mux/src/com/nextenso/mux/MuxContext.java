package com.nextenso.mux;

import alcatel.tess.hometop.gateways.concurrent.ThreadPool;

import com.nextenso.mux.event.MuxMonitor;

public interface MuxContext
{
    public int getMajorVersion();

    public int getMinorVersion();

    public int getAppId();

    public String getAppName();

    public String getInstanceName();

    public String getHostName();

    public int getPid();

    public String getAgentUid();

    public String getGroupUid();

    public String getPlatformUid();

    public MuxMonitor getMuxMonitor();
}
