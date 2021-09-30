package com.alcatel.as.service.metering.impl;

import com.alcatel.as.service.metering.Counter;
import com.alcatel.as.service.metering.Meter;
import com.alcatel.as.service.metering.Sampler;
import com.alcatel.as.service.metering.Stat;
import com.alcatel.as.service.metering.StopWatch;

class CounterImpl extends MeterImpl implements Counter {
  public static final String COUNTER_NAME = "counter.name";
  private final StopWatch _nullStopWatch = new NullStopWatch();
  private static Time _time;
  
  public static void setTime(Time time) {
    _time = time;
  }
  
  public CounterImpl(String name) {
    super(name);
  }
  
  @Override
  public String toString() {
    return new StringBuilder("Counter(").append(getName()).append(")").toString();
  }
  
  public void add(long value) {
    doSet(value, true);
  }
  
  public StopWatch start() {
    return _listeners.length > 0 ? new StopWatchImpl() : _nullStopWatch;
  }
  
  @Override
  public String getDisplayName() {
    return "Counter";
  }
  
  public Sampler createSampler() {
    Sampler sampler = new CounterSampler();
    addMeterListener(sampler);
    return sampler;
  }
  
  class CounterSampler implements Sampler {
    protected CounterMath _counter = new CounterMath();
    protected CounterMath _counterAcc = new CounterMath();
    
    public synchronized void meterChanged(Meter meter, long count, boolean add) {
      _counter.set(count);
      _counterAcc.set(count);
    }
    
    public synchronized Stat computeStat() {
      Stat stat = new StatImpl(CounterImpl.this, _counter.getSum(), _counterAcc.getSum(), _counter.getMean(),
          _counterAcc.getMean(), _counter.getStandardDeviation(), _counterAcc.getStandardDeviation(),
          _counter.getMin(), _counterAcc.getMin(), _counter.getMax(), _counterAcc.getMax());
      _counter.reset();
      return stat;
    }
    
    public Meter getMeter() {
      return CounterImpl.this;
    }
    
    public void remove() {
      removeMeterListener(this);
    }
  }
  
  class StopWatchImpl implements StopWatch {
    private long _startedAt = _time.currentTimeMillis(); // -1 if paused
    private long _elapsed;
    private boolean _stopped;
    
    public synchronized long pause() {
      if (!_stopped && _startedAt != -1) { // if not stopped and not paused
        _elapsed += _time.currentTimeMillis() - _startedAt;
        _startedAt = -1; // paused
      }
      return _elapsed;
    }
    
    public synchronized void resume() {
      if (!_stopped && _startedAt == -1) { // if not stopped and paused
        _startedAt = _time.currentTimeMillis();
      }
    }
    
    public synchronized long stop() {
      if (_stopped == false) {
        if (_startedAt != -1) // if not paused
        {
          _elapsed += (_time.currentTimeMillis() - _startedAt);
        }
        add(_elapsed);
        _stopped = true;
      }
      return _elapsed;
    }
    
    public synchronized long getElapsedTime() {
      // If stopped or paused, return _elapsed.
      // if running, return current elapsed time.
      return (_stopped || _startedAt == -1) ? _elapsed : _elapsed + (_time.currentTimeMillis() - _startedAt);
    }
    
    public Counter getCounter() {
      return CounterImpl.this;
    }

	@Override
	public void close() {
		stop();		
	}
  }
  
  public class NullStopWatch implements StopWatch {
    public long pause() {
      return 0L;
    }
    
    public void resume() {
    }
    
    public long stop() {
      return 0L;
    }
    
    public long getElapsedTime() {
      return 0;
    }
    
    public Counter getCounter() {
      return CounterImpl.this;
    }

	@Override
	public void close() {
		stop();		
	}
  }
}
