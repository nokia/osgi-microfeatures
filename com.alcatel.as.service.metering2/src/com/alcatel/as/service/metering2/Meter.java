// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2;

import java.util.Collection;
import java.util.concurrent.Executor;

/** 
 * Base interface for all types of meters.
 */
public interface Meter extends ValueSupplier {
  
  /**
   * The different kind of meters.
   */
  public static enum Type {
    /**
     * An absolute meter can only be set with an absolute value
     * @see Meter#set(long)
     * @see Meter#getAndSet(long)
     * @see Meter#getAndReset()
     */
    ABSOLUTE,
    
    /**
     * An incremental meter can only be incremented/decremented
     * @see Meter#getAndReset()
     * @see Meter#inc(long)
     * @see Meter#dec(long)
     */
    INCREMENTAL,
    
    /**
     * A Supplier meter is reported using a java callback and can't be updated
     */
    SUPPLIED 
  };
  
  /**
   * Returns the Meter type
   * @return the Meter type
   */
  public Type getType();
  
  /**
   * Sets an absolute meter
   * @param value the absolute value
   * @return this meter
   */
  public Meter set(long value); 
    
  /**
   * Gets then sets this meter
   * @param value the absolute value
   * @return this meter
   */
  public long getAndSet(long value);
  
  /**
   * Gets then resets this meter
   * @return this meter
   */
  public long getAndReset(); 
  
  /**
   * Increments the current gauge value.
   * Any change-based listeners are notified when the gauge is modified.
   */
  Meter inc(long delta);
  
  /**
   * Decrements the current gauge value.
   * Any change-based listeners are notified when the gauge is modified.
   */
  Meter dec(long delta);
  
  /**
   * Returns the meter name, which has to be unique among the {@link Monitorable}'s meters.
   * @return The meter name.
   */
  String getName();
  
  /**
   * @return true if some monitoring jobs are currently registered on this meter.
   */
  boolean hasJobs();
  
  /**
   * Notify any monitoring jobs that this meter has changed.
   */
  void updated();
  
  /**
   * Starts a change based MonitoringJob.
   * @param context the listener context, passed to the listener when being updated
   * @param listener The listener which is called on each meter change.
   * @param executor The executor used to invoke the listener. Can be null
   * @return The MonitoringJob which can be used to stop the monitoring job
   */
  MonitoringJob startJob(MeterListener<?> listener, Object context, Executor executor);
  
  /**
   * Starts a time based/periodic MonitoringJob on a given Monitorable service.
   * @param listener The monitoring job periodically invoked
   * @param context the listener context, passed to the listener when being updated
   * @param executor The executor used to invoke the monitoring job. Can be null.
   * @param schedule The delay in millis between two samples. 
   * @param reportCount The number of samples, 0 for unlimited reports. When the report count is positive, the
   * monitoring job will be automatically stopped.
   * @return The MonitoringJob,  which be used to stop the associated timer.
   */
  MonitoringJob startScheduledJob(MeterListener<?> listener, Object context, Executor executor, long schedule,
                                  int reportCount);
  
  /**
   * Starts a stopwatch to monitor the rate and how fast a piece of java code is being executed.
   * When the stopWatch is closed, the elapsed nano time is accumulated in this counter.
   * The returned stopwatch is not enabled by default (noop). To activate it, you have to
   * start a monitoring job on this meter, or you have to pass force=true, when invoking the start method.
   * 
   * @param force true if the stopwatch should be active even if no monitor job is currently tracking this meter.
   *        using a true value means the stopwatch calculation will take place even if no listeners are currently registered.
   * @return Starts a stopwatch to monitor the rate and how fast a piece of java code is being executed. 
   */
  StopWatch startWatch(boolean force);
  
  /**
   * Returns the current list of registered jobs.
   * @return the List of currently active monitoring job
   */
  Collection<MonitoringJob> getJobs();
  
  /**
   * Stop all pending jobs
   */
  void stopAllJobs();
  
  /**
   * Attaches an object to this Meter.
   * @param x the object to attach
   * @return x (useful for forward chaining)
   */
  <T> T attach (T x);

  /**
   * Returns this Meter attachment
   * @return the attachment
   */
  <T> T attachment ();
}
