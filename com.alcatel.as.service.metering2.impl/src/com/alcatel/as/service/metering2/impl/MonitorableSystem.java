// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2.impl;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeterListener;
import com.alcatel.as.service.metering2.MeteringConstants;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.MonitoringJob;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.metering2.util.Meters;
import com.alcatel.as.service.metering2.util.ThresholdListener;
import com.alcatel.as.util.config.ConfigHelper;
import com.sun.management.OperatingSystemMXBean;

/**
 * Monitorable system meters. We'll report memory and cpu usage.
 * Thread safe.
 */
public class MonitorableSystem extends SimpleMonitorable {
    // Logger.
    private final static Logger _log = Logger.getLogger("as.service.metering2.system");

    // PlatformExecutors service used to create a queue for meter listeners execution
    private volatile PlatformExecutors _execs;

    // MeteringService used to create our system meters.
    private volatile MeteringService _metering; // injected

    // BundleContext, injected by DM.
    private volatile BundleContext _bctx; // injected

    // meter monitoing peak of used memory.
    private Meter _memPeakUsage;

    // meter for used memory.
    @SuppressWarnings("unused")
    private Meter _memUsage;
    
    // meter for used memory right after last collection.
    @SuppressWarnings("unused")
    private Meter _memCollectionUsage;

    // meter for free memory.
    @SuppressWarnings("unused")
    private Meter _memFree;

    // meter for max usable memory.
    @SuppressWarnings("unused")
    private Meter _memMax;

    // meter for peak of used memory
    @SuppressWarnings("unused")
    private Meter _peakMemUsage;

    // meter for percentage of system load
    private Meter _systemLoad;

    // meter for percentage of system load average
    private Meter _systemLoadAvg;

    // meter for percentage of process load
    private Meter _processLoad;
   
    // meter for process cpu time
    private Meter _processTime;
    
    // meter for the number of available cores
    private Meter _availableCores;

    // meter for the amount of virtual memory 
    private Meter _committedVirtualMemorySize;
    
    // meter for the amount of free physical memory in bytes
    private Meter _freePhysicalMemorySize;
    
    // meter for the amount of free swap space in bytes.
    private Meter _freeSwapSize;
    
    // meter for the total amount of physical memory in bytes.
    private Meter _totalPhysicalMemorySize;
    
    // meter for the total amount of swap space in bytes.
    private Meter _totalSwapSpace;

    // JMX OS mbean used to get process and cpu load
    private OperatingSystemMXBean _osBean;

    // Flag used to atomically start scheduled jobs on overload meters.
    private volatile boolean _overloadJobsInitialized;

    // List of meters that participates in the overload 
    private final List<MonitoringJob> _jobs = new ArrayList<>();

    // Queue used to execute meters listeners.
    private PlatformExecutor _queue;

    // Our configuration (optional, content can be empty)
	private Dictionary<String, String> _conf;
        	 
    public MonitorableSystem() {
        super(MeteringConstants.SYSTEM, "System Metrics");
    }
        
    public void start() {
        _osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        final MemoryPoolMXBean oldMemPool = findOldMemPool();
        if (oldMemPool == null) {
            throw new RuntimeException("Can not initialize system meters: Old Memory Pool not found from JVM");
        }

        _queue = _execs.createQueueExecutor(_execs.getProcessingThreadPoolExecutor());

        // Meter for the percentage of the used memory, since last monitoring.
        _memPeakUsage = createValueSuppliedMeter(_metering, MeteringConstants.SYSTEM_MEM_PEAK_USAGE, () -> {
        	long peekUsagePerc = getUsedPercentage(oldMemPool.getPeakUsage());
        	oldMemPool.resetPeakUsage();
        	return peekUsagePerc;
        });

        // Meter for the percentage of the used memory.
        _memUsage = createValueSuppliedMeter(_metering, MeteringConstants.SYSTEM_MEM_USAGE, 
        		() -> getUsedPercentage(oldMemPool.getUsage()));
        
        // Meter for the percentage of the used memory after the occurrence of the last Full GC.
        _memCollectionUsage = createValueSuppliedMeter(_metering, MeteringConstants.SYSTEM_MEM_COLLECTION_USAGE, 
        		() -> getUsedPercentage(oldMemPool.getCollectionUsage()));
                
        // Meter for the percentage of the free memory.
        _memFree = createValueSuppliedMeter(_metering, MeteringConstants.SYSTEM_MEM_FREE, 
        		() -> getFreePercentage(oldMemPool.getUsage()));

        // Meter for the max available memory.
        _memMax = createValueSuppliedMeter(_metering, MeteringConstants.SYSTEM_MEM_MAX, 
        		() -> oldMemPool.getUsage().getMax());

        // Meter for the percentage of host system cpu load
        _systemLoad = createValueSuppliedMeter(_metering, MeteringConstants.SYSTEM_CPU_SYSTEM_LOAD, 
        		() -> getCpuLoad(_osBean.getSystemCpuLoad()));
        
        // Meter for the percentage of process cpu load
        _processLoad = createValueSuppliedMeter(_metering, MeteringConstants.SYSTEM_CPU_PROCESS_LOAD, 
        		() -> getCpuLoad(_osBean.getProcessCpuLoad()));
        
        // meter for process cpu time
        _processTime = createValueSuppliedMeter(_metering, MeteringConstants.SYSTEM_CPU_PROCESS_TIME, 
        		() -> getCpuLoad(_osBean.getProcessCpuTime()));

        // Meter for the percentage of host system cpu load
        _systemLoadAvg = createValueSuppliedMeter(_metering, MeteringConstants.SYSTEM_CPU_SYSTEM_LOAD_AVERAGE, 
        		() -> getSystemLoadAverage(_osBean));
        
        // Meter for the number of available cores
        _availableCores = createValueSuppliedMeter(_metering, MeteringConstants.SYSTEM_CPU_CORES, 
        		_osBean::getAvailableProcessors);
        
        // meter for the amount of virtual memory
        _committedVirtualMemorySize = createValueSuppliedMeter(_metering, MeteringConstants.SYSTEM_MEM_COMMITTED_VIRTUAL_SIZE,
        		_osBean::getCommittedVirtualMemorySize);
        
        // meter for the amount of free physical memory in bytes
        _freePhysicalMemorySize = createValueSuppliedMeter(_metering, MeteringConstants.SYSTEM_MEM_FREE_PHYSICAL_SIZE,
        		_osBean::getFreePhysicalMemorySize);

        // meter for the amount of free swap space in bytes.
        _freeSwapSize = createValueSuppliedMeter(_metering, MeteringConstants.SYSTEM_MEM_FREE_SWAP_SIZE,
        		_osBean::getFreeSwapSpaceSize);
        
        // meter for the amount of free swap space in bytes.
        _totalSwapSpace = createValueSuppliedMeter(_metering, MeteringConstants.SYSTEM_MEM_TOTAL_SWAP_SIZE,
        		_osBean::getTotalSwapSpaceSize);

        // meter for the total amount of physical memory in bytes.
        _totalPhysicalMemorySize = createValueSuppliedMeter(_metering, MeteringConstants.SYSTEM_MEM_TOTAL_PHYSICAL_SIZE,
        		_osBean::getTotalPhysicalMemorySize);

        // Meter for uptime
        addMeter (Meters.createUptimeMeter (_metering));
        super.start(_bctx);        
    }
        
    @Override
    public ConcurrentMap<String, Meter> getMeters() {
        return super.getMeters();
    }
    
    private MemoryPoolMXBean findOldMemPool() {
        MemoryPoolMXBean oldMemPool = null;
        List<MemoryPoolMXBean> mempoolsmbeans = ManagementFactory.getMemoryPoolMXBeans();

        for (MemoryPoolMXBean m : mempoolsmbeans) {
            info("Looking for old gen memory pool: checking %s", m.getName());
        }

        for (MemoryPoolMXBean m : mempoolsmbeans) {
            info("Looking for old gen memory pool: checking %s", m.getName());
                        
            if (m.getType() == MemoryType.HEAP && m.isUsageThresholdSupported()) {
                oldMemPool = m;
                if (_log.isInfoEnabled()) {
                	info("Found Old Generation Memory Pool %s", info(m));
                }
                break;
            }
        }

        if (oldMemPool == null) {
            _log.error("Did not find any pool with usage threshold support. Flow Control service can't be started.");
        }

        return oldMemPool;
    }

    private String info(MemoryPoolMXBean pool) {
        String s = String.format("OLD[n=\"%s\" u=%4s cu=%4s]", pool.getName(),
        		// usage includes unreachable objects that are not collected yet
        		info(pool.getUsage()),
        		// usage as of right after the last collection
        		info(pool.getCollectionUsage()));

        return s;
    }

    private String info(MemoryUsage mu) {
        return String.format("%3s%%", getUsedPercentage(mu));
    }
    
    private void info(String format, Object ... args) {
        if (_log.isEnabledFor(Level.INFO)) {
        	_log.info(String.format(format, args));
        }
    }
    
    private int getUsedPercentage(MemoryUsage mu) {
    	return (int) Math.round(((double) (mu.getUsed() * 100L) / (double) mu.getMax()));
    }
    
    private int getFreePercentage(MemoryUsage mu) {
        return (int) Math.round((double) ((mu.getMax() - mu.getUsed()) * 100) / (double) mu.getMax());
    }
    
    private long getSystemLoadAverage(OperatingSystemMXBean osBean) {
        double sysloadAvg = osBean.getSystemLoadAverage();
        long syslogAvgPerc = sysloadAvg < 0 ? 0 : (long) ((sysloadAvg * 100F) / osBean.getAvailableProcessors());
        return Math.min(syslogAvgPerc, 100);
    }
    
    private long getCpuLoad(double cpuLoad) {
        // usually takes a couple of seconds before we get real values (a negative value means we can't currently get the actual process cpu load). 
        return cpuLoad < 0 ? 0 : (long) (cpuLoad * 100f);
    }

}
