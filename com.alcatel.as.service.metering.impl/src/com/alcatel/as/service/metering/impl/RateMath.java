package com.alcatel.as.service.metering.impl;

/**
 * Class used to calculate the number of hits per seconds. Usage:
 * 
 * <pre>
 * 	RateMath rate = new RateMath();
 * 	rate.hit(1);
 * 	rate.hit(1);
 * 	sleep(4000);
 * 	rate.accumulate();
 * 	rate.getMean() will return 2/4 = 0.5 msg/seconds
 * </pre>
 */
public class RateMath {
  private static Time _time = new TimeImpl();
  private static long _startTime = _time.currentTimeMillis();
  private double _mean;
  private double _sumOfQuareOfHitsPerSec;
  private double _deviation;
  private double _hitsSinceLastAccum;
  private double _hits;
  private double _maxHitsPerSec;
  private double _minHitsPerSec;
  private long _lastAccumTime;
  private long _start;
  
  public static void setTime(Time time) {
    _time = time;
    _startTime = time.currentTimeMillis();
  }
  
  public RateMath() {
    _minHitsPerSec = -1;
    _start = _lastAccumTime = _startTime;
  }
  
  public double getMean() {
    return _mean;
  }
  
  public double getStandardDeviation() {
    return _deviation;
  }
  
  public void reset() {
    _minHitsPerSec = -1;
    _mean = 0;
    _maxHitsPerSec = 0;
    _hitsSinceLastAccum = 0;
    _sumOfQuareOfHitsPerSec = 0;
    _hits = 0;
    _deviation = 0;
    _start = _lastAccumTime = _time.currentTimeMillis();
  }
  
  public double getMax() {
    return _maxHitsPerSec;
  }
  
  public double getMin() {
    return _minHitsPerSec == -1 ? 0 : _minHitsPerSec;
  }
  
  public double getHits() {
    return _hits;
  }
  
  public void hit(double hits) {
    accumulatePreviousHits();
    _hitsSinceLastAccum += hits;
  }
  
  public void accumulate() {
    double now = _time.currentTimeMillis();
    doAccumulatePreviousHits((long) now);
    
    double deltaSec = (now - _start) / 1000;
    if (deltaSec == 0) {
      deltaSec = 1;
    }
    _mean = (_hits == 0) ? 0 : _hits / deltaSec;
    _deviation = Math.sqrt(_sumOfQuareOfHitsPerSec / deltaSec - _mean * _mean);
  }
  
  /**
   * Accumulates hits that was entered during last period (>= 1 seconds).
   */
  private void accumulatePreviousHits() {
    long now = _time.currentTimeMillis();
    if (now - _lastAccumTime >= 1000) {
      doAccumulatePreviousHits(now);
    }
  }
  
  private void doAccumulatePreviousHits(long now) {
    if (_hitsSinceLastAccum > 0) {
      _hits += _hitsSinceLastAccum;
      _sumOfQuareOfHitsPerSec += (_hitsSinceLastAccum * _hitsSinceLastAccum);
      _maxHitsPerSec = Math.max(_hitsSinceLastAccum, _maxHitsPerSec);
    }
    
    if (now - _lastAccumTime <= 2000) {
      _minHitsPerSec = _minHitsPerSec == -1 ? _hitsSinceLastAccum : Math.min(_hitsSinceLastAccum,
                                                                             _minHitsPerSec);
    } else {
      _minHitsPerSec = 0;
    }
    _hitsSinceLastAccum = 0;
    _lastAccumTime = now;
  }
  
  /*
  public static void main(String ... args) throws Exception {
  TimeTest time = new TimeTest();
  long now = System.currentTimeMillis();
  time.setTime(now);
  RateMath rm = new RateMath();
  rm.setTime(time);
  
  rm.hit(10);
  time.setTime(now + 2000);
  rm.hit(5);
  time.setTime(now + 3000);
  rm.accumulate();
  System.out.println(rm.getMean() + "/" + rm.getStandardDeviation());
  
  StatHelper helper = new StatHelper();
  helper.set(10);
  helper.set(0);
  helper.set(5);
  System.out.println(helper.getMean() + "/" + helper.getDeviation());
  }
  */
}
