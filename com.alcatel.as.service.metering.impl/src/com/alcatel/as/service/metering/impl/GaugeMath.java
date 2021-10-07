package com.alcatel.as.service.metering.impl;

public class GaugeMath {
  private double _hits; // Number of numbers that have been entered.
  private double _sum; // The sum of all the items that have been entered.
  private double _sumOfSqare; // The sum of the squares of all the items.
  private double _max = Double.NEGATIVE_INFINITY; // Largest item seen.
  private double _min = Double.POSITIVE_INFINITY; // Smallest item seen.
  private long _lastSetTime = -1;
  private double _value;
  private static Time _time = new TimeImpl();
  
  public static void setTime(Time time) {
    _time = time;
  }
  
  public void reset() {
    /*
    _value = 0;
    _hits = 0;
    _sum = 0;
    _sumOfSqare = 0;
    _lastSetTime = _time.currentTimeMillis();
    _max = Double.NEGATIVE_INFINITY;
    _min = Double.POSITIVE_INFINITY;
    */
    _lastSetTime = -1;
    _sum = _value;
    _sumOfSqare = _value * _value;
    _hits = 1;
    _max = _min = _value;
  }
  
  public void set(double n) {
    accumulate();
    _set(n);
  }
  
  public void add(double n) {
    set(n + _value);
  }
  
  public void accumulate() {
    if (_lastSetTime == -1) {
      _lastSetTime = _time.currentTimeMillis();
      return;
    }
    long delta = (_time.currentTimeMillis() - _lastSetTime);
    
    if (delta > 1) {
      delta--;
      double s = _value * delta;
      _hits += delta;
      _sum += s;
      _sumOfSqare += (delta * (_value * _value));
    }
    _lastSetTime = _time.currentTimeMillis();
  }
  
  private void _set(double n) {
    _value = n;
    _hits++;
    _sum += n;
    _sumOfSqare += n * n;
    _max = Math.max(_max, n);
    _min = Math.min(_min, n);
  }
  
  /**
   * @return the sum of all the items that have been entered.
   */
  public double getSum() {
    return _sum;
  }
  
  /**
   * @return average of all the values that have been entered.
   */
  public double getMean() {
    return _hits == 0 ? 0 : (_sum / _hits);
  }
  
  public double getHits() {
    return _hits;
  }
  
  /**
   * @return standard deviation of all the items that have been entered.
   */
  public double getStandardDeviation() {
    if (_hits == 0) {
      return 0;
    }
    double mean = getMean();
    return Math.sqrt(_sumOfSqare / _hits - mean * mean);
  }
  
  /**
   * @return the smallest item that has been entered.
   */
  public double getMin() {
    return _hits == 0 ? 0 : _min;
  }
  
  /**
   * @return the largest item that has been entered
   */
  public double getMax() {
    return _hits == 0 ? 0 : _max;
  }
  
  public double get() {
    return _value;
  }
  
  public static class TimeTest implements Time {
    private static long _time;
    
    public static void set(long time) {
      _time = time;
    }
    
    public static void add(long time) {
      _time += time;
    }
    
    public long currentTimeMillis() {
      return _time;
    }
  }
  
  public static void main(String ... args) {
    foo5();
  }
  
  static void foo5() {
    TimeTest time = new TimeTest();
    GaugeMath g = new GaugeMath();
    g.setTime(time);
    
    g.set(10);
    time.add(5000);
    g.set(5);
    time.add(5000);
    
    g.accumulate();
    System.out.println("mean=" + g.getMean() + ", deviation=" + g.getStandardDeviation() + ", min="
        + g.getMin() + ", max=" + g.getMax());
    
    g.reset();
    g.set(3);
    time.add(2500);
    g.set(7);
    time.add(2500);
    g.accumulate();
    System.out.println("mean=" + g.getMean() + ", deviation=" + g.getStandardDeviation() + ", min="
        + g.getMin() + ", max=" + g.getMax());
  }
  
  static void foo4() {
    TimeTest time = new TimeTest();
    GaugeMath g = new GaugeMath();
    g.setTime(time);
    
    g.set(2);
    g.set(4);
    g.set(4);
    g.set(4);
    g.set(5);
    g.set(5);
    g.set(7);
    g.set(9);
    
    //time.add(5000);
    //g.add(10);
    
    g.accumulate();
    System.out.println("mean=" + g.getMean() + ", deviation=" + g.getStandardDeviation());
    
    g.reset();
    g.accumulate();
    System.out.println("mean=" + g.getMean() + ", deviation=" + g.getStandardDeviation());
    
  }
  
  static void foo() {
    TimeTest t = new TimeTest();
    GaugeMath g = new GaugeMath();
    g.setTime(t);
    
    for (int i = 0; i < 10; i++) {
      g.add(10);
      t.add(100);
      
      g.add(-10);
      t.add(100);
    }
    
    g.accumulate();
    System.out.println("mean=" + g.getMean() + ", deviation=" + g.getStandardDeviation());
  }
  
  static void foo2() {
    TimeTest t = new TimeTest();
    GaugeMath g = new GaugeMath();
    g.setTime(t);
    
    g.add(5);
    t.add(100 * 10);
    
    g.accumulate();
    System.out.println("mean=" + g.getMean() + ", deviation=" + g.getStandardDeviation());
  }
  
  static void foo3() {
    TimeTest time = new TimeTest();
    GaugeMath g = new GaugeMath();
    g.setTime(time);
    
    g.add(1);
    time.add(4000);
    g.add(1);
    time.add(6000);
    g.add(-1);
    
    g.accumulate();
    System.out.println("mean=" + g.getMean() + ", deviation=" + g.getStandardDeviation());
  }
  
}
