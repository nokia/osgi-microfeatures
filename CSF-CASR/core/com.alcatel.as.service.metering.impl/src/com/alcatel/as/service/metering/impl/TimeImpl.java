package com.alcatel.as.service.metering.impl;

public class TimeImpl implements Time {
  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }
}
