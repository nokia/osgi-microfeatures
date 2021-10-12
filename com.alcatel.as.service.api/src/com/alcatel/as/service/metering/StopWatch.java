// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering;

/**
 * A StopWatch meter for measuring time elapsed by a method. StopWatch accumulates elapsed time
 * (in millis.) into a corresponding Counter.
 */
public interface StopWatch extends AutoCloseable {
  /**
   * Stops this stopwatch and registers the elapsed time into the associated StopWatch's counter.
   * Stopping an already stopped StopWatch has no effect.
   * 
   * @return the elapsed time, not including paused time; or 0 if this StopWatch's counter is
   *         not currently activated from log4j.
   */
  long stop();

  /**
   * Closes this stopwatch (same as stop method) and registers the elapsed time into the associated StopWatch's counter.
   * Closing an already stopped StopWatch has no effect.
   * This close method is actually used with AutoCloseable jdk7 syntax:
   * try(stopWatch=counter.start()) {
   *    ...
   * }
   */
  public void close();  
  
  /**
   * Pauses this running stopwatch. Pausing an already paused/stopped stopwatch has no effect.
   * 
   * @return the current elapsed time, or 0 if this StopWatch's counter is not currently
   *         activated from log4j.
   */
  long pause();
  
  /**
   * Resumes this stopwatch. Resuming an already resumed/stopped stopwatch has no effect.
   */
  void resume();
  
  /**
   * Returns the current elapsed time.
   * 
   * @return The current elapsed time, not including paused time, or 0 if this StopWatch's
   *         counter is not currently activated from log4j.
   */
  long getElapsedTime();
  
  /**
   * Returns the counter which this stop watch is bound to.
   * @returns this stopwatch's associated Counter
   */
  Counter getCounter();
}
