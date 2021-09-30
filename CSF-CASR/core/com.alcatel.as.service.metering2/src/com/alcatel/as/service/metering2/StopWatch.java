package com.alcatel.as.service.metering2;

/**
 * A StopWatch which adds elapsed nano time (when closed) to a given Meter.
 * A StopWatch returned by a {@link Meter} is not enabled by default. To enable stopwatches calculation, 
 * you have to either start a monitoring job on the associated meter, or you have to create the stopwatch
 * with force=true.
 */
public interface StopWatch extends AutoCloseable {
  /**
   * Stops the stopwatch and adds the elapsed time in nanos to the counter from which it was started.
   */
  void close();
}
