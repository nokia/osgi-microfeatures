package com.alcatel.as.service.metering.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.alcatel.as.service.metering.Counter;
import com.alcatel.as.service.metering.Gauge;
import com.alcatel.as.service.metering.Meter;
import com.alcatel.as.service.metering.MeterListener;
import com.alcatel.as.service.metering.Rate;
import com.alcatel.as.service.metering.Stat;
import com.alcatel.as.util.config.ConfigHelper;

/**
 * This is the default sampler task which periodically collects all meters and their
 * corresponding statistics, and dump them into log4j.
 */
public class DefaultStatSampler implements Runnable, MeteringProperties {
  private Map<String, Listener> _listeners = new HashMap<String, Listener>();
  private volatile Thread _thread;
  private volatile boolean _someActiveLoggers = false;
  private volatile boolean _running = false;
  private volatile long _delay = 10000;
  private volatile double _threshold = 0;
  private boolean _calculateAccumulated;
  private final static Logger _logger = Logger.getLogger("as.service.metering.DefaultStatSampler");
  
  public DefaultStatSampler(Time time) {
    GaugeMath.setTime(time);
    RateMath.setTime(time);
  }
  
  /**
   * Handles our configuration.
   * 
   * @param conf Our service configuration.
   */
  public synchronized void updated(Dictionary<?, ?> conf) {
    _delay = ConfigHelper.getLong(conf, METERING_DELAY, 10) * 1000;
    _threshold = ConfigHelper.getLong(conf, METERING_THRESHOLD, 0);
    _calculateAccumulated = ConfigHelper.getBoolean(conf, METERING_CALCULATEACCUM, false);
    if (_logger.isDebugEnabled()) {
      _logger.debug("bound configuration: delay=" + _delay + ", threshold=" + _threshold);
    }
  }
  
  /**
   * Stops our Service. Method not called by DM, but by ourself.
   */
  public synchronized void start() {
    if (_thread != null) {
      return;
    }
    if (_logger.isDebugEnabled()) {
      _logger.debug("Metering sampler thread starting");
    }
    _running = true;
    _thread = new Thread(this, "MeteringService");
    _thread.setDaemon(true);
    _thread.start();
  }
  
  /**
   * Stops our Service.
   */
  public void stop() {
    if (_logger.isDebugEnabled()) {
      _logger.debug("Metering sampler is stopping");
    }
    
    _running = false;
    if (_thread != null) {
      _thread.interrupt();
      try {
        _thread.join();
      } catch (InterruptedException e) {
        _logger.error("Could not stop DefaultStatSampler service", e);
      }
    }
  }
  
  /**
   * Adds a log enabled Meter into our list of monitored meters. If the corresponding logger
   * is not enabled, then we won't monitor it.
   * 
   * @param meter the new Meter to be monitored.
   */
  public synchronized void addMeter(Meter meter) {
    Logger meterLogger = Logger.getLogger(meter.getName());
    if (!meterLogger.isInfoEnabled()) {
      if (_logger.isDebugEnabled()) {
        _logger.debug("addMeter: meter not active: " + meter.getName());
      }
      return;
    }
    
    if (_logger.isDebugEnabled()) {
      _logger.debug("addMeter: " + meter.getName());
    }
    
    Listener l = null;
    if (meter instanceof Gauge) {
      l = new GaugeListener(meter, meterLogger);
    } else if (meter instanceof Counter) {
      l = new CounterListener(meter, meterLogger);
    } else if (meter instanceof Rate) {
      l = new RateListener(meter, meterLogger);
    }
    
    _listeners.put(meter.getName(), l);
    _someActiveLoggers = true;
  }
  
  /**
   * Unregister a Meter from our list of monitored meters.
   * 
   * @param meter
   */
  public synchronized void clearMeters() {
    _logger.info("Clearing all active meters");
    for (Listener listener : _listeners.values()) {
      listener.getMeter().removeMeterListener(listener);
    }
    _listeners.clear();
    _someActiveLoggers = false;
  }
  
  /**
   * Our thread which periodically scans the list of monitored meters.
   */
  public void run() {
    while (_running) {
      try {
        Thread.sleep(_delay);
        if (_someActiveLoggers) {
          synchronized (this) {
            if (_logger.isInfoEnabled()) {
              _logger.info("Calculating statistics over last " + (_delay / 1000) + " seconds ...\n");
            }
            
            for (Listener l : getSortedListeners()) {
              if (l.isEnabled()) {
                l.doStatistics();
              }
            }
          }
        } else {
          if (_logger.isInfoEnabled()) {
            _logger.info("No Actives logger currently configured.\n");
          }
        }
      } catch (Throwable t) {
        if (!_running) {
          break;
        }
        _logger.error("Got unexpected excetion while generating meter statistics", t);
      }
    }
    
    _logger.debug("Metering sampler thread stopped");
  }
  
  /**
   * Retrieve the list of sorted listeners.
   * 
   * @return the list sorted by the listener name.
   */
  private Listener[] getSortedListeners() {
    Listener[] array = new Listener[_listeners.size()];
    array = _listeners.values().toArray(array);
    Arrays.sort(array, new ListenerComparator());
    return array;
  }
  
  /**
   * Class used to sort Listener instances
   */
  static class ListenerComparator implements Comparator<Listener> {
    public int compare(Listener l1, Listener l2) {
      return l1.getMeter().getName().compareTo(l2.getMeter().getName());
    }
  }
  
  /**
   * Base class for our meter listeners.
   */
  private abstract class Listener implements MeterListener {
    protected final Meter _meter;
    protected final Logger _logger;
    
    Listener(Meter meter, Logger logger) {
      _meter = meter;
      _logger = logger;
    }
    
    /**
     * Returns the meter which is listened.
     * 
     * @return
     */
    Meter getMeter() {
      return _meter;
    }
    
    /**
     * Check if the log4j logger for this meter is active or not.
     */
    public boolean isEnabled() {
      return _logger.isInfoEnabled();
    }
    
    /**
     * Calculate statistics for this meter.
     * 
     * @return true if the statistics has been logged, of false if the meter logger is not
     *         enabled.
     */
    void doStatistics() {
      if (_logger.isInfoEnabled()) {
        Object rendered = null;
        synchronized (this) {
          rendered = render();
        }
        if (rendered != null) {
          _logger.info(rendered);
        }
      }
    }
    
    /**
     * Handle a meter change event.
     */
    public void meterChanged(final Meter meter, final long count, final boolean add) {
      if (_logger.isInfoEnabled()) {
        synchronized (this) {
          handleMeterChanged(meter, count, add);
        }
      }
    }
    
    /**
     * Subclasses are called here for for displaying stats into log4j.
     * 
     * @return the object that will be logged into log4j.
     */
    protected abstract Object render();
    
    /**
     * Subclasses are called here for handling meter events.
     * 
     * @param meter The meter which value is changing.
     * @param count the new value
     * @param add true if the count is being added into the meter, of false if the value is
     *          absolute.
     */
    protected abstract void handleMeterChanged(Meter meter, long count, boolean add);
  }
  
  /**
   * This listener accumulates Gauge events.
   */
  private class GaugeListener extends Listener {
    protected GaugeMath _gaugeMath;
    protected GaugeMath _gaugeMathAcc;
    
    GaugeListener(Meter meter, Logger logger) {
      super(meter, logger);
      _gaugeMath = new GaugeMath();
      _gaugeMathAcc = new GaugeMath();
      _meter.addMeterListener(this);
    }
    
    @Override
    protected void handleMeterChanged(Meter meter, long count, boolean add) {
      if (add) {
        _gaugeMath.add(count);
        if (_calculateAccumulated) {
          _gaugeMathAcc.add(count);
        }
      } else {
        _gaugeMath.set(count);
        if (_calculateAccumulated) {
          _gaugeMathAcc.set(count);
        }
      }
    }
    
    @Override
    protected Object render() {
      if (_gaugeMath.get() <= _threshold) {
        return null;
      }
      
      _gaugeMath.accumulate();
      _gaugeMathAcc.accumulate();
      Stat event = new StatImpl(getMeter(), _gaugeMath.get(), _gaugeMathAcc.get(), _gaugeMath.getMean(),
          _gaugeMathAcc.getMean(), _gaugeMath.getStandardDeviation(), _gaugeMathAcc.getStandardDeviation(),
          _gaugeMath.getMin(), _gaugeMathAcc.getMin(), _gaugeMath.getMax(), _gaugeMathAcc.getMax());
      _gaugeMath.reset();
      return event;
    }
  }
  
  /**
   * This listener accumulates Counter events.
   */
  private class CounterListener extends Listener {
    protected CounterMath _counterMath = new CounterMath();
    protected CounterMath _counterMathAcc = new CounterMath();
    
    CounterListener(Meter meter, Logger logger) {
      super(meter, logger);
      _meter.addMeterListener(this);
    }
    
    @Override
    protected void handleMeterChanged(Meter meter, long count, boolean add) {
      _counterMath.set(count);
      if (_calculateAccumulated) {
        _counterMathAcc.set(count);
      }
    }
    
    @Override
    protected Object render() {
      if (_counterMath.get() <= _threshold) {
        return null;
      }
      Stat stat = new StatImpl(getMeter(), _counterMath.getSum(), _counterMathAcc.getSum(),
          _counterMath.getMean(), _counterMathAcc.getMean(), _counterMath.getStandardDeviation(),
          _counterMathAcc.getStandardDeviation(), _counterMath.getMin(), _counterMathAcc.getMin(),
          _counterMath.getMax(), _counterMathAcc.getMax());
      _counterMath.reset();
      return stat;
    }
  }
  
  /**
   * This listener accumulates HitCounter events.
   */
  private class RateListener extends Listener {
    protected RateMath _hitCounterMath = new RateMath();
    protected RateMath _hitCounterMathAcc = new RateMath();
    
    RateListener(Meter meter, Logger logger) {
      super(meter, logger);
      _meter.addMeterListener(this);
    }
    
    @Override
    protected void handleMeterChanged(Meter meter, long count, boolean add) {
      _hitCounterMath.hit(count);
      if (_calculateAccumulated) {
        _hitCounterMathAcc.hit(count);
      }
    }
    
    @Override
    protected Object render() {
      if (_hitCounterMath.getHits() <= _threshold) {
        return null;
      }
      
      _hitCounterMath.accumulate();
      _hitCounterMathAcc.accumulate();
      Stat stat = new StatImpl(getMeter(), _hitCounterMath.getHits(), _hitCounterMathAcc.getHits(),
          _hitCounterMath.getMean(), _hitCounterMathAcc.getMean(), _hitCounterMath.getStandardDeviation(),
          _hitCounterMathAcc.getStandardDeviation(), _hitCounterMath.getMin(), _hitCounterMathAcc.getMin(),
          _hitCounterMath.getMax(), _hitCounterMathAcc.getMax());
      _hitCounterMath.reset();
      return stat;
    }
  }
}
