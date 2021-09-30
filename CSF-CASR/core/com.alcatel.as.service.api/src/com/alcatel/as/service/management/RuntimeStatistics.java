package com.alcatel.as.service.management;

/**
  * this interface provide some aggregated information with respect to runtime
  * measurements.
  * 
  * @deprecated use metering service2 "system" meters.
  */
public interface RuntimeStatistics
{

    /** return the current value of the real time computed jvm response time */
    public int getAverageResponseTime();

    /** return the current memory occupation in percentage of available memory */
    public int getMemoryOccupation();

    /** return the current memory level */
    public int getMemoryLevel();

    /** return the time in second sent into the normal memory level state */
    public int getNormalMemoryLevelSec();

    /** return the time in second sent into the normal memory level state */
    public int getHighMemoryLevelSec();

    /** return the time in second sent into the normal memory level state */
    public int getEmergencyMemoryLevelSec();

    /** return the average host cpu in percent */
    public int getHostCpuLoad();

    /** return true if the we are operating under concurrent garbage collection */
    public boolean hasConcurrentGc();

}