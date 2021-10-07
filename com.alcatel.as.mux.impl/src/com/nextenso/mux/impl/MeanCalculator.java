package com.nextenso.mux.impl;

/**
 * This class compute the average of incoming values, based on LENGTH_OF_SUITE
 * accumulated values.
 */
public class MeanCalculator {
  
  private int _nbCall = 0;
  private int _xAccumulator = 0;//is a accumulator of the x last results. x vary between 0 and LENGTH_OF_AVERAGE_SUBSCRIBE.
  private int _currentAverage = 0;
  private int _lenghtOfSuite;
  
  public MeanCalculator(int length) {
    _lenghtOfSuite = length;
  }
  
  public int addValueInTheSerie(int currentDelay) {
    _nbCall++;
    if (_nbCall < 0) {
      _nbCall = 0;
      _xAccumulator = 0;
      _currentAverage = 0;
    }
    if (_nbCall <= _lenghtOfSuite) {
      _xAccumulator = _xAccumulator + currentDelay;
      _currentAverage = _xAccumulator / _nbCall;
    } else {
      _xAccumulator = _xAccumulator - _currentAverage;
      _xAccumulator = _xAccumulator + currentDelay;
      _currentAverage = _xAccumulator / _lenghtOfSuite;
    }
    return _currentAverage;
  }
  
  public int setAverage(int value) {
    _nbCall = _lenghtOfSuite;
    _currentAverage = value;
    _xAccumulator = _lenghtOfSuite * value;
    return _currentAverage;
  }
  
  public int getCurrentAverage() {
    return _currentAverage;
  }
  
  @Override
  public String toString() {
    return "Mean calculator : current average=" + _currentAverage;
  }
}
