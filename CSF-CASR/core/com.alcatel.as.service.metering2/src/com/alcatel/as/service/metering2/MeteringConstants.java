package com.alcatel.as.service.metering2;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Metering Constants.
 */
@ProviderType
public interface MeteringConstants {
    /**
     * Each monitorable services must be provided in the registry with this service property.
     */
    final static String MONITORABLE_NAME = "monitorable.name";

    /**
     * Monitorable System Name.
     */
    public final static String SYSTEM = "as.service.system";
    
    /**
     * Monitorable System Meter: recent percentage of cpu load for the whole system (value ranges from 0 to 100).
     * This meter is a value-supplied meter.
     */
    public final static String SYSTEM_CPU_SYSTEM_LOAD = "cpu.system.load";
    
    /**
     * Monitorable System Meter: the system load average for the last minute. The returned value is a percentage (0 <= value <= 100).
     * The system load average is the sum of the number of runnable entities queued to the available processors and the number 
     * of runnable entities running on the available processors averaged over a period of time. 
     * The way in which the load average is calculated is operating system specific but is typically a damped time-dependent average.
     * If the load average is not available, 0 is returned.
     * This method is designed to provide a hint about the system load and may be queried frequently. The load average may be unavailable 
     * on some platform where it is expensive to implement this method.
	 * This meter is a value-supplied meter.
     */
    public final static String SYSTEM_CPU_SYSTEM_LOAD_AVERAGE = "cpu.system.load.average";

    /**
     * Monitorable System Meter: recent percentage of process cpu load (value ranges from 0 to 100)
     * This meter is a value-supplied meter.
     */
    public final static String SYSTEM_CPU_PROCESS_LOAD = "cpu.process.load";
    
    /**
     * Monitorable System Meter: the CPU time used by the process on which the Java virtual machine is running in nanoseconds.
     * This meter is a value-supplied meter.
     */
    public final static String SYSTEM_CPU_PROCESS_TIME = "cpu.process.time";
    
    /**
     * Monitorable System Meter: Provides the number of available cores.
     * This meter is a value-supplied meter.
     */
    public final static String SYSTEM_CPU_CORES = "cpu.cores";

    /**
     * Monitorable System Meter: percentage of used memory (value ranges from 0 to 100).
     * This meter type is a value-supplied meter.
     */
    public final static String SYSTEM_MEM_USAGE = "memory.usage";
    
    /**
     * Monitorable System Meter: percentage of used memory after the occurrence of the last Full GC (value ranges from 0 to 100).
     * This meter type is a value-supplied meter.
     */
    public final static String SYSTEM_MEM_COLLECTION_USAGE = "memory.collection.usage";

    /**
     * Monitorable System Meter: percentage of free memory (value ranges from 0 to 100).
     * This meter type is a value-supplied meter.
     */
    public final static String SYSTEM_MEM_FREE = "memory.free";

    /**
     * Monitorable System Meter: Returns the maximum amount of memory in bytes that can be used for memory management. 
     * This method returns -1 if the maximum memory size is undefined..
     * This meter type is a value-supplied meter.
     */
    public final static String SYSTEM_MEM_MAX = "memory.max";

    /**
     * Monitorable System Meter: percentage of (peak of) used memory (value ranges from 0 to 100)
     * This meter is a value-supplied meter.
     */
    public final static String SYSTEM_MEM_PEAK_USAGE = "memory.peak.usage";
        
    /**
     * Monitorable System Meter: The amount of virtual memory that is guaranteed to be available to the running process in bytes, 
     * or -1 if this operation is not supported.
     * This meter is a value-supplied meter.
     */
    public final static String SYSTEM_MEM_COMMITTED_VIRTUAL_SIZE = "memory.committed.virtual.size";  

    /**
     * Monitorable System Meter:  the amount of free physical memory in bytes.
     * This meter is a value-supplied meter.
     */
    public final static String SYSTEM_MEM_FREE_PHYSICAL_SIZE = "memory.free.physical.memory.size";  

    /**
     * Monitorable System Meter: the amount of free swap space in bytes.
     * This meter is a value-supplied meter.
     */
    public final static String SYSTEM_MEM_FREE_SWAP_SIZE = "memory.free.swap.size";  

    /**
     * Monitorable System Meter: the total amount of swap space in bytes.
     * This meter is a value-supplied meter.
     */
    public final static String SYSTEM_MEM_TOTAL_SWAP_SIZE = "memory.total.swap.size";  

    /**
     * Monitorable System Meter: the total amount of physical memory in bytes.
     * This meter is a value-supplied meter.
     */
    public final static String SYSTEM_MEM_TOTAL_PHYSICAL_SIZE = "memory.total.physical.size";  

    /**
     * Monitorable System Meter: uptime of the process in seconds.
     * This meter is a value-supplied meter.
     */
    public final static String SYSTEM_UPTIME = "uptime";
    
    /**
     * Monitorable System Meter: Overload meter. value=0(normal), 1 (overload).
     * This meter type is an absolute meter.
     */
    public final static String SYSTEM_OVERLOAD = "overload";
}
