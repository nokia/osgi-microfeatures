package com.alcatel.as.service.metering;

/**
 * A Gauge for exposing how long a resource is used. application states variations. Gauges 
 * values can increase or decrease during the application life cycle. 
 * For example, use Gauges for calculating variations about established connections, memory used, 
 * free memory, number of active threads, etc ...
 * <p>
 * Gauges have two methods:
 * <ul>
 * <li>add (for adding a delta, which can be negative)</li>
 * <li>set (for setting an absolute value).</li>
 * </ul>
 * Basically, you will invoke the method Gauge.add(1) each time you handle a new connection, and
 * you will invoke set() when you set the current number of JVM free memory. Gauges takes into
 * account elapsed time.
 * <p>
 * The two following examples won't give the same results:
 * <ul>
 * <li>gauge.add(10); sleep(5000); gauge.add(20); -> the gauge will have a value of 10 for a
 * period of 5 seconds; hence, the mean equals 11.66 ((10 * 5 + 20) / 6).</li> </li>
 * <li>gauge.add(10); sleep(10000); gauge.add(20); -> the gauge will have a value of 10 for a
 * period of 10 seconds; hence, the means equals 10.9 ((10 * 10 + 20) / 11)</li>
 * </ul>
 */
public interface Gauge extends Meter {
  /**
   * Adds a delta to this Gauge and eventually notify interested listeners.
   * 
   * @param delta the positive or negative delta value added to this gauge.
   */
  void add(long delta);
  
  /**
   * Sets this gauge to a given value and eventually notify interested listeners.
   * 
   * @param value the new value assigned to this gauge.
   */
  void set(long value);
}
