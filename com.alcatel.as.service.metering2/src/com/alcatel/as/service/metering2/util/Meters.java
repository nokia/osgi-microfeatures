// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeterListener;
import com.alcatel.as.service.metering2.MeteringConstants;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.metering2.MonitoringJob;
import com.alcatel.as.service.metering2.StopWatch;
import com.alcatel.as.service.metering2.ValueSupplier;

/**
 * This class provides default Meters listeners that can be registered on any existing meters
 */
public class Meters {

   /**
    * A Null Meter implementation that does nothing
    */
  public static final Meter NULL_METER = new NullMeter();

  private static class NullMeter implements Meter {

      @Override
      public long getValue() {
         return 0;
      }

      @Override
      public Type getType() {
         return null;
      }

      @Override
      public Meter set(long value) {
         return this;
      }

      @Override
      public long getAndSet(long value) {
         return 0;
      }

      @Override
      public long getAndReset() {
         return 0;
      }

      @Override
      public Meter inc(long delta) {
         return this;
      }

      @Override
      public Meter dec(long delta) {
         return this;
      }

      @Override
      public String getName() {
         return null;
      }

      @Override
      public boolean hasJobs() {
         return false;
      }

      @Override
      public void updated() {
      }

      @Override
      public MonitoringJob startJob(MeterListener<?> listener, Object context, Executor executor) {
         return null;
      }

      @Override
      public MonitoringJob startScheduledJob(MeterListener<?> listener, Object context, Executor executor,
            long schedule, int reportCount) {
         return null;
      }

      @Override
      public StopWatch startWatch(boolean force) {
         return null;
      }

      @Override
      public Collection<MonitoringJob> getJobs() {
         return null;
      }

      @Override
      public void stopAllJobs() {
      }

      @Override
      public <T> T attach(T x) {
         return null;
      }

      @Override
      public <T> T attachment() {
         return null;
      }
  }

  /**
   * Helper used to invoke a listener when a given meter value exceeds a given treshold
   *
   * @param <C> the type of the object returned by the MeterListener.updated method.
   */
  private static class ThresholdListenerWrapper<C> implements MeterListener<C> {
    private ThresholdListener<C> _trigger;
    private long _threshold;
    private boolean _above, _init = true;
    
    private ThresholdListenerWrapper(long threshold, ThresholdListener<C> trigger) {
      _threshold = threshold;
      _trigger = trigger;
    }
    
    public C updated(Meter meter, C context) {
      boolean above = meter.getValue() >= _threshold;
      if (_init) {
        _above = !above; // we force the call
        _init = false;
      }
      try {
        if (above) {
          if (!_above)
            return _trigger.above(_threshold, meter, context);
          else
            return context;
        } else {
          if (_above)
            return _trigger.below(_threshold, meter, context);
          else
            return context;
        }
      } finally {
        _above = above;
      }
    }
  }
  
  /**
   * Creates a Meter listener that is called when a given existing meter value is exceeding a given threshold.
   * @param threshold the threshold value
   * @param thlistener the listener to invoke when a meter value exceeds the specified threshold
   * @return the new meter listener
   */
  public static <C> MeterListener<C> newThresholdListener(long threshold, ThresholdListener<C> thlistener) {
    return new ThresholdListenerWrapper(threshold, thlistener);
  }
  
  private static class DelayedAboveThresholdListener<C> implements ThresholdListener<C> {
    private long _delay;
    private ThresholdListener<C> _thListener;
    private ScheduledExecutorService _exec;
    private Future _future;
    private C _ctx;
    private boolean _above, _init = true;
    
    private DelayedAboveThresholdListener(long delay, ThresholdListener<C> thlistener, ScheduledExecutorService exec) {
      _delay = delay;
      _thListener = thlistener;
      _exec = exec;
    }
    
    public C above(final long threshold, final Meter meter, C ctx) {
      if (_init){
	_ctx = ctx;
	_init = false;
      }
      Runnable r = new Runnable() {
        public void run() {
          _future = null;
          _above = true;
          _ctx = _thListener.above(threshold, meter, _ctx);
        }
      };
      _future = _exec.schedule(r, _delay, TimeUnit.MILLISECONDS);
      return null;
    }
    
    public C below(long threshold, Meter meter, C ctx) {
      if (_init){
	_ctx = ctx;
	_init = false;
      }
      if (_above) {
        _ctx = _thListener.below(threshold, meter, _ctx);
        _above = false;
      } else {
        if (_future != null) {
          _future.cancel(true);
          _future = null;
        }
      }
      return null;
    }
  }
  
  /**
   * Creates a MeterListener used to trigger a callback when a given meter is above a given treshold for at least a given delay
   * @param threshold the meter value above which the callback is invoked
   * @param delay the delay after which the callback is invoked
   * @param exec the executor used to scheduler the timer
   * @param thlistener the listener callback which can implement a "above" and "below" callbacks.
   * @return the MeterListener that wraps the DelayedAboveThresholdListener
   */
  public static <C> MeterListener<C> newDelayedAboveThresholdListener(long threshold, long delay,
		  															  ScheduledExecutorService exec,
                                                                      ThresholdListener<C> thlistener) {
    DelayedAboveThresholdListener delayedListener = new DelayedAboveThresholdListener(delay, thlistener, exec);
    return new ThresholdListenerWrapper(threshold, delayedListener);
  }
  
  private static class HighLowWatermarksState<C> {
    C aboveHigh(Meter meter, C x) {
      return x;
    }
    
    C belowLow(Meter meter, C x) {
      return x;
    }
    
    C betweenLowHigh(Meter meter, C x) {
      return x;
    }
    
    C run(Meter meter, long low, long high, C x) {
      // note that low==high works
      long value = meter.getValue();
      if (value >= high)
        return aboveHigh(meter, x);
      if (value < low)
        return belowLow(meter, x);
      return betweenLowHigh(meter, x);
    }
  }
  
  /**
   * Meter Listener used to call a "above" listener callback when a given meter value exceeds a "high" treshold.
   * And when the meter value comes back below a "low" watermark, then call the "below" listener callback
   *
   * @param <C> the context listener
   */
  private static class HighLowWatermarksListener<C> implements MeterListener<C> {
    private ThresholdListener<C> _trigger;
    private long _high, _low;
    private HighLowWatermarksState<C> _state;
    private HighLowWatermarksState<C> state_neutral = new HighLowWatermarksState<C>() {
      @Override
      C aboveHigh(Meter meter, C x) {
        _state = state_above;
        return _trigger.above(_high, meter, x);
      }
    };
    private HighLowWatermarksState<C> state_above = new HighLowWatermarksState<C>() {
      @Override
      C belowLow(Meter meter, C x) {
        _state = state_neutral;
        return _trigger.below(_low, meter, x);
      }
    };
    
    private HighLowWatermarksListener(long high, long low, ThresholdListener<C> trigger) {
      _high = high;
      _low = low;
      _trigger = trigger;
      _state = state_neutral;
    };
    
    public C updated(Meter meter, C context) {
      return _state.run(meter, _low, _high, context);
    }
  }
  
  /**
   * Creates a Meter Listener used to call a "above" listener callback when a given meter value exceeds a "high" treshold.
   * And when the meter value comes back below a "low" watermark, then call the "below" listener callback
   *
   * @param high when the meter value exceeds the "high" treshold, then the "above" listener callback is invoked
   * @param low when the meter value comes back below the "low" treshold, then the "below" listener callback is invoked
   * @param <C> the context listener
   */
  public static <C> MeterListener<C> newHighLowWatermarksListener(long high, long low, ThresholdListener<C> thlistener) {
    if (low > high)
      throw new IllegalArgumentException("Low threshold greater than high threshold : low=" + low + "/high=" + high);
    return new HighLowWatermarksListener(high, low, thlistener);
  }
  
  /**
   * Class implementing the {@link Meters#newDelayedHighLowWatermarksListener(long, long, long, long, PlatformExecutor, ThresholdListener)} method.
   *
   * @param <C> the type of the listener context
   */
  private static class DelayedHighLowWatermarksListener<C> implements MeterListener<C> {
    private long _aboveDelay, _belowDelay, _highWM, _lowWM;
    private ThresholdListener<C> _wrappedListener;
    private HighLowWatermarksState<HighLowWatermarksState> _state;
    private ScheduledExecutorService _exec;
    private Future _future;
    private C _ctx;
    private boolean _init = true;
    
    private void cleanFuture(boolean cancel) {
      if (cancel)
        _future.cancel(true);
      _future = null;
    }
    
    private HighLowWatermarksState<HighLowWatermarksState> state_neutral = new HighLowWatermarksState<HighLowWatermarksState>() {
      @Override
      HighLowWatermarksState aboveHigh(final Meter meter, HighLowWatermarksState x) {
        Runnable r = new Runnable() {
          public void run() {
            cleanFuture(false);
            _ctx = _wrappedListener.above(_highWM, meter, _ctx);
            _state = state_above_fired;
          }
        };
        _future = _exec.schedule(r, _aboveDelay, TimeUnit.MILLISECONDS);
        return state_above_pending;
      }
    };
    private HighLowWatermarksState<HighLowWatermarksState> state_above_pending = new HighLowWatermarksState<HighLowWatermarksState>() {
      @Override
      HighLowWatermarksState belowLow(Meter meter, HighLowWatermarksState x) {
        return betweenLowHigh(meter, x);
      }
      
      @Override
      HighLowWatermarksState betweenLowHigh(Meter meter, HighLowWatermarksState x) {
        cleanFuture(true);
        return state_neutral;
      }
    };
    private HighLowWatermarksState<HighLowWatermarksState> state_above_fired = new HighLowWatermarksState<HighLowWatermarksState>() {
      @Override
      HighLowWatermarksState belowLow(final Meter meter, HighLowWatermarksState x) {
        Runnable r = new Runnable() {
          public void run() {
            cleanFuture(false);
            _ctx = _wrappedListener.below(_lowWM, meter, _ctx);
            _state = state_neutral;
          }
        };
        _future = _exec.schedule(r, _belowDelay, TimeUnit.MILLISECONDS);
        return state_below_pending;
      }
    };
    private HighLowWatermarksState<HighLowWatermarksState> state_below_pending = new HighLowWatermarksState<HighLowWatermarksState>() {
      @Override
      HighLowWatermarksState aboveHigh(Meter meter, HighLowWatermarksState x) {
        return betweenLowHigh(meter, x);
      }
      
      @Override
      HighLowWatermarksState betweenLowHigh(Meter meter, HighLowWatermarksState x) {
        cleanFuture(true);
        return state_above_fired;
      }
    };
    
    private DelayedHighLowWatermarksListener(long high, long low, long aboveDelay, long belowDelay,
                                             ThresholdListener<C> wrappedListener, ScheduledExecutorService exec) {
      _highWM = high;
      _lowWM = low;
      _aboveDelay = aboveDelay;
      _belowDelay = belowDelay;
      _wrappedListener = wrappedListener;
      _exec = exec;
      _state = state_neutral;
    }
    
    public C updated(Meter meter, C context) {
      if (_init){
	_ctx = context;
	_init = false;
      }
      _state = _state.run(meter, _lowWM, _highWM, _state);
      return null;
    }
  }
  
  /**
   * Creates a Meter Listener used to call a "above" listener callback when a given meter value exceeds a "high" treshold
   * for at least a given "highDelay" period.
   * And when the meter value comes back below a "low" watermark, then call the "below" listener callback.
   *
   * @param high when the meter value exceeds the "high" treshold, then the "above" listener callback is invoked
   * @param low when the meter value comes back below the "low" treshold, then the "below" listener callback is invoked
   * @param highDelay the "above" listener callback is invoked only if the meter value is above the "high" treshold" for at
   *        least the "highDelay" period in millis
   * @param exec the Executor for the timer used to track the meter value
   * @param <C> the context listener
   */
  public static <C> MeterListener<C> newDelayedHighLowWatermarksListener(long high, long low, long highDelay,
		  																 ScheduledExecutorService exec,
                                                                         ThresholdListener<C> thlistener) {
    if (low > high)
      throw new IllegalArgumentException("Low threshold greater than high threshold : low=" + low + "/high=" + high);
    return newHighLowWatermarksListener(high, low, new DelayedAboveThresholdListener(highDelay, thlistener, exec));
  }
  
  /**
   * Creates a Meter Listener used to call a "above" listener callback when a given meter value exceeds a "high" treshold
   * for at least a given "highDelay" period.
   * And when the meter value comes back below a "low" watermark for at least the "lowDelay" period, then call the "below" listener callback.
   *
   * @param high when the meter value exceeds the "high" treshold, then the "above" listener callback is invoked
   * @param low when the meter value comes back below the "low" treshold, then the "below" listener callback is invoked
   * @param highDelay the "above" listener callback is invoked only if the meter value is above the "high" treshold" for at
   *        least the "highDelay" period in millis
   * @param lowDelay the "below" listener callback is invoked only if the meter value gets back (and remains) under the "low" treshold for at
   *        least the "lowDelay" period in millis
   * @param exec the Executor for the timer used to track the meter value
   * @param <C> the context listener
   */
  public static <C> MeterListener<C> newDelayedHighLowWatermarksListener(long high, long low, long highDelay,
                                                                         long lowDelay,
                                                                         ScheduledExecutorService exec,
                                                                         ThresholdListener<C> thlistener) {
    return new DelayedHighLowWatermarksListener(high, low, highDelay, lowDelay, thlistener, exec);
  }
  
  private static class DiscreteEventListener<C> implements MeterListener<C> {
    private MeterListener<C> _trigger;
    private int _count = -1, _nb;
    
    private DiscreteEventListener(int nb, MeterListener<C> trigger) {
      _nb = nb;
      _trigger = trigger;
    }
    
    // note that the first call to this method is done when registering --> skip the first call
    public C updated(Meter meter, C context) {
      if (++_count == _nb) {
        _count = 0;
        return _trigger.updated(meter, context);
      }
      return context;
    }
  }
  
  /**
   * Creates a Meter Listener which invokes a listener only when a given meter is modified more than a given
   * number of time.
   * 
   * @param eventsNb the listener is invoked only if the meter is modified more than the "eventsNb" time.
   * @param listener the listener to invoke if the meter is modified more than the "eventsNb" time
   * @return the meter listener
   */
  public static <C> MeterListener<C> newDiscreteEventListener(int eventsNb, MeterListener<C> listener) {
    return new DiscreteEventListener(eventsNb, listener);
  }
  
  private static class MaxValueListener implements MeterListener<Meter> {
    private long _maxValue;
    
    private MaxValueListener() {
    }
    
    public Meter updated(Meter meter, Meter store) {
      long value = meter.getValue();
      if (value > _maxValue)
        store.set(_maxValue = value);
      return store;
    }
  }
  
  /**
   * Creates a Meter which represents the max value of an existing meter.
   * 
   * @param metering the Metering Service
   * @param target the existing meter.
   * @return a new Meter which will calculate the max value of the "target" meter. The new meter name will be set to the existing "target" meter, suffixed with
   * ".max"
   */
  public static Meter createMaxValueMeter(MeteringService metering, Meter target) {
    return createMaxValueMeter(metering, new StringBuilder().append(target.getName()).append(".max").toString(), target);
  }
  
  /**
   * Creates a Meter which represents the max value of an existing meter.
   * 
   * @param metering the Metering Service
   * @param maxValueMeterName the name of the new created meter
   * @param target the existing meter.
   * @return a new Meter which will calculate the max value of the "target" meter.
   */
  public static Meter createMaxValueMeter(MeteringService metering, String maxValueMeterName, Meter target) {
    Meter store = metering.createAbsoluteMeter(maxValueMeterName);
    store.attach (target.startJob(new MaxValueListener(), store, null));
    return store;
  }
  
  /**
   * Creates a Meter which represents the max value of an existing meter.
   * The Meter will periodically watch for the existing meter value and will record the max value.
   * <br/>Usage example : check the max value of the JVM memory.
   * 
   * @param metering the Metering Service used to create the new meter (which name will be the name of the "target" meter, suffixed with ".max"
   * @param target the existing meter
   * @param delay the period in millis used to periodically watch for the existing meter
   * @param reportCount The number of samples, 0 for unlimited reports.
   */
  public static Meter createScheduledMaxValueMeter(MeteringService metering, Meter target, long delay, int reportCount) {
    return createScheduledMaxValueMeter(metering, new StringBuilder().append(target.getName()).append(".max")
        .toString(), target, delay, reportCount);
  }
  
  /**
   * Creates a Meter which represents the max value of an existing meter.
   * The Meter will periodically watch for the existing meter value and will record the max value.
   * 
   * @param metering the Metering Service used to create the new meter
   * @param maxValueMeterName the name of the new meter to be created
   * @param target the existing meter
   * @param delay the period in millis used to periodically watch for the existing meter
   * @param reportCount The number of samples, 0 for unlimited reports.
   */
  public static Meter createScheduledMaxValueMeter(MeteringService metering, String maxValueMeterName, Meter target,
                                                   long delay, int reportCount) {
    Meter store = metering.createAbsoluteMeter(maxValueMeterName);
    store.attach (target.startScheduledJob(new MaxValueListener(), store, null, delay, reportCount));
    return store;
  }
  
  private static class MovingMaxValueListener implements MeterListener<Meter> {
    private long[] _samples;
    private int _index = 0, _maxIndex = -1;
    private long _maxValue = 0L;
    
    private MovingMaxValueListener(int samples) {
      _samples = new long[samples];
      for (int i = 0; i < samples; i++)
        _samples[i] = 0L;
    }
    
    public Meter updated(Meter meter, Meter store) {
      long value = meter.getAndReset();
      _samples[_index] = value;
      if (_index == _maxIndex) { // we need to re-calculate the max sample
        _maxValue = value;
        _maxIndex = _index;
        for (int k = 0; k < _samples.length; k++) {
          if (k == _index)
            continue;
          long tmp = _samples[k];
          if (tmp > _maxValue) {
            _maxValue = tmp;
            _maxIndex = k;
          }
        }
        store.set(_maxValue);
      } else {
        if (value > _maxValue) {
          _maxIndex = _index;
          store.set(_maxValue = value);
        }
      }
      if (++_index == _samples.length)
        _index = 0;
      return store;
    }
  }
    
  /**
   * Creates a Meter which represents the moving max value of an existing meter.
   * The Meter will periodically watch the existing max value meter and will record the moving value.
   * <br/>Example of usage : track the max value of a meter on the last 5 second interval.
   * <br/>The interval is represented as a number of samples of a given delay : for ex, 5 seconds can be set as 5 intervals of 1 seconds.
   * The reason is that the accuracy of the result is equivalent to the duration of a sample : basically, samples are seen as blocks, and a sample is cleared as a whole (when obsolete).
   * 
   * @param metering the Metering Service used to create the new meter
   * @param movingMaxValueMeter the name of the created meter storing the moving max value
   * @param maxValueMeter the existing meter storing the max value
   * @param sampling the period in millis of a sample
   * @param samples The number of samples. When multiplied by the sampling, it gives the whole moving interval.
   */
  public static Meter createMovingMaxValueMeter(MeteringService metering, String movingMaxValueMeter, Meter maxValueMeter,
                                                long sampling, int samples) {
    MovingMaxValueListener listener = new MovingMaxValueListener(samples);
    Meter store = metering.createAbsoluteMeter(movingMaxValueMeter);
    store.attach (maxValueMeter.startScheduledJob(listener, store, null, sampling, samples));
    return store;
  }
  
  private static class RateListener implements MeterListener<Meter> {
    private long _previous, _previousDate;
    private boolean _inited;
    
    private RateListener() {
    }
    
    public Meter updated(Meter meter, Meter store) {
      long value = meter.getValue();
      long now = System.currentTimeMillis();
      if (_inited) {
        long interval = now - _previousDate;
        if (interval < 100) // too short
          return store;
        long increase = value - _previous;
        increase *= 1000;
        increase /= interval;
        store.set(increase);
      } else {
        _inited = true;
      }
      _previous = value;
      _previousDate = now;
      return store;
    }
  }
  
  /**
   * Creates a Meter Listener on an existing meter that calculates the rate of events over time (i.e: requests per seconds)
   * @param metering the Metering Service used to create the new meter (which name is the "target" meter suffixed with ".rate")
   * @param target the existing meter
   * @param interval the interval in millis used to calculate the rates
   * @return the new Rate Meter
   */
  public static Meter createRateMeter(MeteringService metering, Meter target, long interval) {
    return createRateMeter(metering, new StringBuilder().append(target.getName()).append(".rate").toString(), target,
                           interval);
  }
  
  /**
   * Creates a Meter Listener on an existing meter that calculates the rate of events over time (i.e: requests per seconds)
   * @param metering the Metering Service used to create the new meter
   * @param name the name of the new meter to create
   * @param target the existing meter
   * @param interval the period used to periodically watch to the existing meter value
   * @return the new Rate Meter
   */
  public static Meter createRateMeter(MeteringService metering, String name, Meter target, long interval) {
    RateListener listener = new RateListener();
    Meter store = metering.createAbsoluteMeter(name);
    store.attach (target.startScheduledJob(listener, store, null, interval, 0));
    return store;
  }

  public static boolean stopRateMeter (Meter rateMeter){
    Object o = rateMeter.attachment ();
    if (o == null || !(o instanceof MonitoringJob)) return false;
    MonitoringJob job = (MonitoringJob) o;
    job.stop ();
    return true;
  }
  public static boolean stopMaxValueMeter (Meter maxValueMeter){
    Object o = maxValueMeter.attachment ();
    if (o == null || !(o instanceof MonitoringJob)) return false;
    MonitoringJob job = (MonitoringJob) o;
    job.stop ();
    return true;
  }

  private static boolean[] separators = new boolean[128];
  static {
    for (int i=0; i<separators.length; i++) separators[i] = true;
    for (int i='0'; i<='9'; i++) separators[i] = false;
    for (int i='a'; i<='z'; i++) separators[i] = false;
    for (int i='A'; i<='Z'; i++) separators[i] = false;
    separators[(int)'_'] = false;
  }
  /**
   * Indicates if the name (monitorable name or meter name) matches the provided pattern.
   * Two patterns are supported:<ul>
   * <li>token : the specified token must be part of the meter name, but not as a substring
   * <li>endsWith
   * </ul>
   * <p/>
   * Examples:<ul>
   * <li>matches ("foo.bar", "foo") returns true
   * <li>matches ("foo.bar", "oo") returns false
   * <li>matches ("foo.bar", "foo.bar*") returns true
   * <li>matches ("foo.bar.test", "foo.bar*") returns true
   * <li>matches ("foo.bart", "foo.bar*") returns false
   * <li>matches ("foo.bar", "=foo.bar") returns true
   * <li>matches ("foo.bart", "=foo.bar") returns false
   * <li>matches ("foo.test", "!foo.bar") returns true
   * </ul>
   * Note that separators can be any char except [0-9, a-z, A-Z]
   * @param name the name to check
   * @param pattern the pattern
   * @return true or false
   */
  public static boolean matches (String name, String pattern){
    if (pattern.startsWith ("!")){
      if (pattern.length () == 1) return false;
      return !matches (name, pattern.substring (1), 0);
    }
    return matches (name, pattern, 0);
  }
  /**
   * Indicates if the monitorable name matches the provided pattern.
   * See doc on matches() method for explanations on pattern matching.
   * @param monitorable the monitorable to check
   * @param pattern the pattern
   * @return true or false
   */
  public static boolean matches (Monitorable monitorable, String pattern){ return matches (monitorable.getName (), pattern);}
  /**
   * Indicates if the meter name matches the provided pattern.
   * See doc on matches() method for explanations on pattern matching.
   * @param meter the meter to check
   * @param pattern the pattern
   * @return true or false
   */
  public static boolean matches (Meter meter, String pattern){ return matches (meter.getName (), pattern);}
  private static boolean matches (String name, String pattern, int from){ // from is not used for regexp
    if (pattern.startsWith ("=")){
      return name.length () == (pattern.length () - 1) &&
	name.regionMatches (false, 0, pattern, 1, name.length ());
    }
    if (pattern.endsWith ("*")){
      int patternPrefixLen = pattern.length () - 1;
      if (patternPrefixLen == 0) return true;
      if (separators[(int)pattern.charAt (pattern.length () - 2)]){
	return name.regionMatches (false, 0, pattern, 0, patternPrefixLen);
      } else {
	if (name.regionMatches (false, 0, pattern, 0, patternPrefixLen)){
	  if (name.length () > patternPrefixLen){
	    return separators[(int) name.charAt (patternPrefixLen)];
	  }
	  return true;
	} else
	  return false;
      }
    } else {
      if (from >= name.length ()) return false;
      int index = name.indexOf (pattern, from);
      int end = index + pattern.length ();
      if (index == -1) return false;
      if (index > 0){
	if (separators[(int)name.charAt (index - 1)] == separators[(int)pattern.charAt (0)]) // the char before must be from a different type than the pattern start
	  return matches (name, pattern, end);
      }
      if (end < name.length ()){
	if (separators[(int)name.charAt (end)] == separators[(int)pattern.charAt (pattern.length () - 1)]) // the char after must be from a different type than the pattern end
	  return matches (name, pattern, end);
      }
      return true;
    }
  }

  private static final Comparator<Meter> comparator = new Comparator<Meter> (){
    public int compare (Meter a, Meter b){ return a.getName ().compareTo (b.getName ());}
    public boolean equals (Object o){ return this == o;}
  };

  /**
   * Retrives the meters matching a given pattern.
   * @param monitorables a list of monitorables to search
   * @param metersPattern the pattern
   * @param sort if the returned list must be sorted
   * @return the list of matching Meters
   */
  public static List<Meter> getMeters (List<Monitorable> monitorables, String metersPattern, boolean sort){
    List<Meter> list = new ArrayList<> ();
    for (Monitorable monitorable : monitorables)
      getMeters (monitorable, metersPattern, list);
    if (sort) Collections.sort(list, comparator);
    return list;
  }
  /**
   * Retrives the meters matching a given pattern.
   * @param monitorable the monitorable to search
   * @param metersPattern the pattern
   * @param sort if the returned list must be sorted
   * @return the list of matching Meters
   */
  public static List<Meter> getMeters (Monitorable monitorable, String metersPattern, boolean sort){
    List<Meter> list = getMeters (monitorable, metersPattern, null);    
    if (sort) Collections.sort(list, comparator);
    return list;
  }
  /**
   * Retrives the meters matching a given pattern.
   * @param monitorable the monitorable to search
   * @param metersPattern the pattern
   * @param list the destination where to put matching meters (may be null)
   * @return the list of matching Meters, same as param, unless the param was null
   */
  public static List<Meter> getMeters (Monitorable monitorable, String metersPattern, List<Meter> list){
    if (list == null) list = new ArrayList<> ();    
    if (monitorable != null){ // make it possible to chain getMeters (getMonitorable (..), ...) without breaking
      for (Meter meter : monitorable.getMeters().values()) {
	if (matches (meter, metersPattern))
	  list.add(meter);
      }
    }
    return list;
  }
  /**
   * Iterates Meters.
   * @param monitorable the monitorable to search
   * @param it the iterator to call
   * @param metersPattern the meters pattern
   * @param T the context to pass along the iteration
   * @return the final context
   */
  public static <T> T iterateMeters (Monitorable monitorable, MeterIterator<T> it, String metersPattern, T ctx){
    for (Meter meter : monitorable.getMeters().values()) {
      if (matches (meter, metersPattern))
	ctx = it.next (monitorable, meter, ctx);
    }
    return ctx;
  }
  /**
   * Iterates Meters.
   * @param monitorables the monitorables to search
   * @param it the iterator to call
   * @param metersPattern the meters pattern
   * @param T the context to pass along the iteration
   * @return the final context
   */
  public static <T> T iterateMeters (List<Monitorable> monitorables, MeterIterator<T> it, String metersPattern, T ctx){
    for (Monitorable monitorable : monitorables){
      ctx = iterateMeters (monitorable, it, metersPattern, ctx);
    }
    return ctx;
  }
  /**
   * Instanciates a new MonitorableIterator.
   * The new MonitorableIterator will be filtered with a pattern.
   * @param it the iterator to call when a Monitorable matches the pattern
   * @param monitorablePattern the monitorable pattern
   * @return the new MonitorableIterator
   */
  public static <T> MonitorableIterator<T> newMonitorableIterator (final MonitorableIterator<T> it, final String monitorablePattern){
    return new MonitorableIterator<T> (){
      public T next (Monitorable monitorable, T ctx){
	if (matches (monitorable, monitorablePattern))
	  return it.next (monitorable, ctx);
	return ctx;
      }
    };
  }
  /**
   * Instanciates a new MeterIterator.
   * The new MeterIterator will be filtered with a pattern.
   * @param it the iterator to call when a Meter matches the pattern
   * @param meterPattern the meter pattern
   * @return the new MeterIterator
   */
  public static <T> MeterIterator<T> newMeterIterator (final MeterIterator<T> it, final String meterPattern){
    return new MeterIterator<T> (){
      public T next (Monitorable monitorable, Meter meter, T ctx){
	if (matches (meter, meterPattern))
	  return it.next (monitorable, meter, ctx);
	return ctx;
      }
    };
  }
  /**
   * Instanciates a new MonitorableIterator.
   * The new MonitorableIterator will call the provided MeterIterator when the provided pattern matches the meter.
   * @param it the iterator to call when a Meter matches the pattern
   * @param meterPattern the meter pattern
   * @return the new MonitorableIterator
   */
  public static <T> MonitorableIterator<T> newMonitorableIterator (final MeterIterator<T> it, final String meterPattern){
    return new MonitorableIterator<T> (){
      public T next (Monitorable monitorable, T ctx){
	return iterateMeters (monitorable, it, meterPattern, ctx);
      }
    };
  }
  /**
   * Instanciates a new MonitorableIterator.
   * The new MonitorableIterator will call the provided MeterIterator when the provided pattern matches the meter.
   * @param monitorablePattern the monitorable pattern
   * @param it the iterator to call when a Meter matches the pattern
   * @param meterPattern the meter pattern
   * @return the new MonitorableIterator
   */
  public static <T> MonitorableIterator<T> newMonitorableIterator (final String monitorablePattern, final MeterIterator<T> it, final String meterPattern){
    return new MonitorableIterator<T> (){
      public T next (Monitorable monitorable, T ctx){
	if (matches (monitorable, monitorablePattern))
	    return iterateMeters (monitorable, it, meterPattern, ctx);
	return ctx;
      }
    };
  }
  /**
   * Generates a pattern.
   * The pattern will be a best pick between an exact match and a regexp.
   * The goal is to easily generate a pattern from arguments mixing exact match and regexp.
   * @param exact a potential exact match
   * @param regexp a potential regexp
   * @return the matching pattern
   */
  public static String toPattern (String exact, String regexp){
    if (exact != null){
      if (exact.length () > 0) return new StringBuilder (exact.length () + 1).append ('=').append (exact).toString ();
    }
    if (regexp == null) return "*";
    if (regexp.length () == 0) return "*";
    return regexp;
  }

  /**
   * Creates an uptime meter (in seconds) named from MeteringConstants.SYSTEM_UPTIME.
   * The start time is when this method is called.
   * @param metering the MeteringService
   */
  public static Meter createUptimeMeter (MeteringService metering){
    return createUptimeMeter (metering, MeteringConstants.SYSTEM_UPTIME);
  }
  /**
   * Creates an uptime meter (in seconds).
   * The start time is when this method is called.
   * @param metering the MeteringService
   * @param meterName the meter name
   */
  public static Meter createUptimeMeter (MeteringService metering, String meterName){
    final long since = System.currentTimeMillis () / 1000L;
    return metering.createValueSuppliedMeter (meterName, new ValueSupplier() {
	public long getValue() {
	  return (System.currentTimeMillis () / 1000L) - since;
	}
      });
  }
  
  public static void main (String[] s){
    System.out.println  (matches ("foo", "foo"));
    System.out.println  (matches ("foo.bar", "foo"));
    System.out.println  (matches ("foo.bar", "bar"));
    System.out.println  (matches ("foo.bar:kiki", "bar"));
    System.out.println  (!matches ("foo.bar", "oo"));
    System.out.println  (!matches ("foo.bar", "fo"));
    System.out.println  (!matches ("foo.bar", "ar"));
    System.out.println  (!matches ("foo.bar", "ba"));
    System.out.println  (matches ("foo.bar.ba", "ba"));

    System.out.println  (matches ("foo", "*"));
    System.out.println  (matches ("foo", "foo*"));
    System.out.println  (!matches ("fooo", "foo*"));
    System.out.println  (matches ("foo.bar", "foo*"));
    System.out.println  (matches ("foo.bar", "foo.*"));
    System.out.println  (!matches ("foo", "foo.*"));
    System.out.println  (matches ("foo.bar.kiki", "foo.bar*"));
    System.out.println  (matches ("foo.bar.kiki", "foo.bar.*"));
    System.out.println  (!matches ("foo.bar", "foo.bar.*"));
    System.out.println  (!matches ("foo.barbar", "foo.bar*"));
    System.out.println  (!matches ("foo.barbar", "foo.bar.*"));

    System.out.println  (matches ("foo.bar", "=foo.bar"));
    System.out.println  (!matches ("foo.bart", "=foo.bar"));

    System.out.println  (matches ("foo.kiki", "!foo.bar.*"));
    System.out.println  (!matches ("foo.bar.kiki", "!foo.bar.*"));
    
    System.out.println  (matches ("foo.test", "!=foo.bar"));

    System.out.println  (matches ("foo@test", "@"));
    System.out.println  (matches ("@test", "@"));
    System.out.println  (matches ("test@", "@"));
    System.out.println  (!matches ("test.@foo", "@"));
    System.out.println  (!matches ("test@.foo", "@"));
  }
}
