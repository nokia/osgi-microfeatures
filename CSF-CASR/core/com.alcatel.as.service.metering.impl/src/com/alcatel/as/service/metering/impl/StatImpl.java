package com.alcatel.as.service.metering.impl;

import java.text.DecimalFormat;

import com.alcatel.as.service.metering.Meter;
import com.alcatel.as.service.metering.Stat;

/**
 * Meter Statistics, which are logged into log4j. You can also get yourself meter statistics
 * using the {@link Meter#createSampler()} method.
 */
public class StatImpl implements Stat {
  /** The Meter for which this stat has been calculated for */
  private Meter meter;
  /** Formatter used to display double values */
  private final static DecimalFormat df = new DecimalFormat("0.000");
  /** Gauge value */
  private final double value;
  /** Gauge value accumulated since jvm startup */
  private final double valueAcc;
  /** Average gauge value */
  private final double mean;
  /** Average accumulated since jvm startup */
  private final double meanAcc;
  /** Standard deviation */
  private final double deviation;
  /** Standard deviation accumulated since jvm startup */
  private final double deviationAcc;
  /** Min Gauge value */
  private final double min;
  /** Min Gauge value accumulated since jvm startup */
  private final double minAcc;
  /** Max Gauge value */
  private final double max;
  /** Max Gauge value since jvm startup */
  private final double maxAcc;
  
  public StatImpl(Meter meter, double value, double valueAcc, double mean, double meanAcc, double deviation,
                  double deviationAcc, double min, double minAcc, double max, double maxAcc) {
    this.meter = meter;
    this.value = value;
    this.valueAcc = valueAcc;
    this.mean = mean;
    this.meanAcc = meanAcc;
    this.deviation = deviation;
    this.deviationAcc = deviationAcc;
    this.min = min;
    this.minAcc = minAcc;
    this.max = max;
    this.maxAcc = maxAcc;
  }
  
  /**
   * @return the _meter
   */
  public Meter getMeter() {
    return meter;
  }
  
  /**
   * @return the value
   */
  public double getValue() {
    return value;
  }
  
  /**
   * @return the valueAcc
   */
  public double getValueAcc() {
    return valueAcc;
  }
  
  /**
   * @return the mean
   */
  public double getMean() {
    return mean;
  }
  
  /**
   * @return the meanAcc
   */
  public double getMeanAcc() {
    return meanAcc;
  }
  
  /**
   * @return the deviation
   */
  public double getDeviation() {
    return deviation;
  }
  
  /**
   * @return the deviationAcc
   */
  public double getDeviationAcc() {
    return deviationAcc;
  }
  
  /**
   * @return the min
   */
  public double getMin() {
    return min;
  }
  
  /**
   * @return the minAcc
   */
  public double getMinAcc() {
    return minAcc;
  }
  
  /**
   * @return the max
   */
  public double getMax() {
    return max;
  }
  
  /**
   * @return the maxAcc
   */
  public double getMaxAcc() {
    return maxAcc;
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    String meterName = ((MeterImpl) meter).getDisplayName();
    sb.append("[").append(meterName).append("] Value=").append(toString(value));
    sb.append("/").append(toString(valueAcc));
    sb.append("; Mean=").append(toString(mean));
    sb.append("/").append(toString(meanAcc));
    sb.append("; Deviation=").append(toString(deviation));
    sb.append("/").append(toString(deviationAcc));
    sb.append("; Min=").append(toString(min));
    sb.append("/").append(toString(minAcc));
    sb.append("; Max=").append(toString(max));
    sb.append("/").append(toString(maxAcc));
    return sb.toString();
  }
  
  private final String toString(double d) {
    if (Double.isNaN(d) || d == Double.NEGATIVE_INFINITY || d == Double.POSITIVE_INFINITY) {
      return "0";
    }
    String s = df.format(d);
    return (s.endsWith(".000")) ? s.substring(0, s.length() - 4) : s;
  }
}
