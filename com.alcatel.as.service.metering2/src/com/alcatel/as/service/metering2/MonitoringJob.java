// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2;

import java.util.concurrent.Executor;

/**
 * A MonitoringJob represents a time-based (periodic) or a change-based Monitor listener.
 */
public interface MonitoringJob {
  /**
   * Returns whether the job is running.
   * @return whether the job is running
   */
  boolean isRunning();
  
  /**
   * Stops this monitoring jobs. For time-based jobs, the timer associated to the job will be stopped.
   * For change-based jobs, the meter listener will be unregistered from the meter.
   */
  void stop();
  
  /**
   * Returns the context that was passed when creating the MonitoringJob.
   * @return The context that was passed when creating the MonitoringJob.
   */
  <T> T getContext();
    
  /**
   * Returns the Meter that this job is associated to.
   * @return The name of the Meter that this job is associated to (may be null if for scheduled jobs).
   */
  Meter getMeter();
  
  /**
   * Returns the Executor used to invoke the MonitorinListener.
   * @return The Executor used to invoke the MonitorinListener.
   */
  Executor getExecutor();
}
