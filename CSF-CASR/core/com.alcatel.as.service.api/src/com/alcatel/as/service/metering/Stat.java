package com.alcatel.as.service.metering;

/**
 * Meter Statistics, which are logged into log4j. You can also get yourself meter statistics
 * using the {@link Meter#createSampler()} method.
 */
public interface Stat {
  /**
   * @return the Meter associated to this Stat instance.
   */
  Meter getMeter();
  
  /**
   * @return the value of this Stat instance. For gauges, the value corresponds to the last
   *         gauge value. For Counters/Rates, the value corresponds to the accumulated
   *         values which have been added into the Counter.
   */
  double getValue();
  
  /**
   * @return the value accumulated since the metering service was started.
   */
  double getValueAcc();
  
  /**
   * @return the mean for all values which has been added into this Stat's Meter.
   */
  double getMean();
  
  /**
   * @return the mean for all values which has been added into this Stat's Meter, since the
   *         metering service startup.
   */
  double getMeanAcc();
  
  /**
   * @return the standard deviation for all values which have been added into this Stat's Meter.
   */
  double getDeviation();
  
  /**
   * @return the standard deviation for all values which have been added into this Stat's Meter,
   *         since the metering service startup.
   */
  double getDeviationAcc();
  
  /**
   * @return the min value of the Stat's Meter.
   */
  double getMin();
  
  /**
   * @return the min value of the Stat's Meter, since the metering service startup.
   */
  double getMinAcc();
  
  /**
   * @return the max value of the Stat's Meter, since the metering service startup.
   */
  double getMax();
  
  /**
   * @return the max value of the Stat's Meter, since the metering service startup, since the
   *         metering service startup.
   */
  double getMaxAcc();
  
  /**
   * @return a String representation for this Stat object.
   */
  public String toString();
}
