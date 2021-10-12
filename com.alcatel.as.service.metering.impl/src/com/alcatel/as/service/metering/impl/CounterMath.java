// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering.impl;

public class CounterMath {
  private double _hits; // Number of numbers that have been entered.
  private double _sum; // The sum of all the items that have been entered.
  private double _sumOfSquare; // The sum of the squares of all the items.
  private double _max = Double.NEGATIVE_INFINITY; // Largest item seen.
  private double _min = Double.POSITIVE_INFINITY; // Smallest item seen.
  private double _value;
  
  public void set(double num) {
    _value = num;
    enter(num);
  }
  
  public void add(double num) {
    _value += num;
    enter(num);
  }
  
  public double getHits() {
    return _hits;
  }
  
  public double getSum() {
    return _sum;
  }
  
  /**
   * @return average of all the items that have been entered
   */
  public double getMean() {
    return _hits == 0 ? 0 : _sum / _hits;
  }
  
  /**
   * @return standard deviation of all the items that have been entered
   */
  public double getStandardDeviation() {
    if (_hits == 0) {
      return 0;
    }
    double mean = getMean();
    return Math.sqrt(_sumOfSquare / _hits - mean * mean);
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
  
  public void reset() {
    _sum = _sumOfSquare = _hits = 0;
    _value = 0;
    _max = Double.NEGATIVE_INFINITY;
    _min = Double.POSITIVE_INFINITY;
  }
  
  public double get() {
    return _value;
  }
  
  private void enter(double num) {
    _hits++;
    _sum += num;
    _sumOfSquare += num * num;
    _max = Math.max(_max, num);
    _min = Math.min(_min, num);
  }
}
