package com.nextenso.mux.impl;

import java.util.ArrayList;
import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.management.RuntimeStatistics;
import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.metering2.MonitoringJob;

import alcatel.tess.hometop.gateways.utils.Log;

/**
 * FlowControl for IOH handler mux connections. When we detect low memory or high cpu activity,
 * we notify registered observers (MuxConnectionImpl).
 */
@ProviderType
public class FlowControl implements RuntimeStatistics {
    // Our logger
    private final static Log _log = Log.getLogger("as.service.flowcontrol");

    // PlatformExecutors service.
    volatile PlatformExecutors _execs;

    // Our Monitorable System, injected from the activator.
    volatile Monitorable _system;

    // The three levels sent to IO handlers
    public static final int MEMORY_LEVEL_NORMAL = 0;
    public static final int MEMORY_LEVEL_HIGH = 1;
    public static final int MEMORY_LEVEL_EMERGENCY = 2;

    // stores MUX_FLOW_CONTROL_HIGH value
    private volatile int _cfgHighMemoryLevel;

    // stores MUX_FLOW_CONTROL_LOW value
    private volatile int _cfgLowMemoryLevel;

    // stores MUX_FLOW_CONTROL_EMERGENCY value
    private volatile int _cfgEmergencyMemoryLevel;

    // total time passed in the NORMAL level
    private volatile long _normalMemElapsed = 0;

    // total time passed in the HIGH level
    private volatile long _highMemElapsed = 0;

    // total time passed in the EMERGENCY level
    private volatile long _emergMemElapsed = 0;

    // System meter used to get current percentage of process cpu load
    private Meter _systemCpuLoad;

    // System meter used to get current percentage of memory used
    private Meter _memPeakUsage;

    // System meter used to detect if we are currently overloaded
    private Meter _overload;

    // Flag telling if we are currently overloaded
    private volatile boolean _overloaded;

    // List of meter listeners 
    private final List<MonitoringJob> _jobs = new ArrayList<>();

    public int getMemoryOccupation() {
        return (int) _memPeakUsage.getValue();
    }

    public int getMemoryLevel() {
    	return _memPeakUsage.getValue() > 80 ? MEMORY_LEVEL_HIGH : MEMORY_LEVEL_NORMAL;
    }

    public int getHostCpuLoad() {
        return (int) _systemCpuLoad.getValue();
    }

    public int getNormalMemoryLevelSec() {
        return 0; // not implemented
    }

    public int getHighMemoryLevelSec() {
        return 0; // not implemented
    }

    public int getEmergencyMemoryLevelSec() {
        return 0; // not implemented
    }

    public boolean hasConcurrentGc() {
        return false;
    }

    public int getAverageResponseTime() {
        // Not supported anymore. Using a probe inside a threadpool is a mess, and brings more problems than solutions.
        return 0;
    }
}
