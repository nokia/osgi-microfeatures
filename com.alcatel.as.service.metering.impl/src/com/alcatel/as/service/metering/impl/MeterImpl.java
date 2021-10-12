// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering.impl;

import org.apache.log4j.Logger;

import com.alcatel.as.service.metering.Meter;
import com.alcatel.as.service.metering.MeterListener;
import com.alcatel.as.service.metering.MeteringConstants;

abstract class MeterImpl implements Meter {
  protected volatile MeterListener[] _listeners = new MeterListener[0];
  protected final static Logger _logger = Logger.getLogger("as.service.metering2.MeterImpl");
  private volatile Object _attachment;
  private final String _name;
  private volatile boolean _hasListeners;
  
  /**
   * Constructor.
   * 
   * @param name The meter name.
   */
  public MeterImpl(String name) {
    _name = name;
  }
  
  public abstract String getDisplayName();
  
  public long getValue() {
    throw new IllegalStateException("Meter " + getName() + " is not stateful (see "
        + MeteringConstants.STATEFUL_METER + " javadoc)");
  }
  
  @Override
  public String toString() {
    return getName();
  }
  
  public String getName() {
    return _name;
  }
  
  public void attach(Object attachment) {
    _attachment = attachment;
  }
  
  public Object attachment() {
    return _attachment;
  }
  
  public synchronized void addMeterListener(MeterListener listener) {
    int length = _listeners.length;
    MeterListener listeners[] = new MeterListener[length + 1];
    System.arraycopy(_listeners, 0, listeners, 0, length);
    listeners[length] = listener;
    _listeners = listeners;
    _hasListeners = true;
  }
  
  public synchronized void removeMeterListener(MeterListener listener) {
    int found = -1;
    int length = _listeners.length;
    for (int i = 0; i < length; i++) {
      if (listener.equals(_listeners[i])) {
        found = i;
        break;
      }
    }
    if (found != -1) {
      MeterListener listeners[] = new MeterListener[length - 1];
      System.arraycopy(_listeners, 0, listeners, 0, found);
      System.arraycopy(_listeners, found + 1, listeners, found, listeners.length - found);
      _listeners = listeners;
    }
    _hasListeners = (_listeners.length > 0);
  }
  
  public boolean hasListeners() {
    return _hasListeners;
  }
  
  protected void doSet(long value, boolean add) {
    MeterListener[] listeners = _listeners;
    for (MeterListener listener : listeners) {
    	try {
    		listener.meterChanged(this, value, add);
    	} catch (Throwable t) {
    		_logger.error("unexpected exception while invoking listener for meter " + _name, t);
    	}
    }
  }
}
