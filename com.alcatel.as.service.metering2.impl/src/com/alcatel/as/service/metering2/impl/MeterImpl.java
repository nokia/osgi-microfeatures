package com.alcatel.as.service.metering2.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.SerialExecutor;
import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeterListener;
import com.alcatel.as.service.metering2.MonitoringJob;
import com.alcatel.as.service.metering2.StopWatch;

public abstract class MeterImpl implements Meter {
  protected final String _name;
  protected final ScheduledExecutorService _timer;
  protected volatile MonitoringJob[] _jobs = new MonitoringJob[0];
  protected Object _attachment;
  private final static Logger _logger = Logger.getLogger("as.service.metering2.Meter");
  
  public MeterImpl(String name, ScheduledExecutorService timer) {
    _name = name;
    _timer = timer;
  }
  
  public <T> T attach (T x){
    _attachment = x;
    return x;
  }
  
  public <T> T attachment (){
    return (T) _attachment;
  }
  
  @Override
  public abstract long getValue();
  
  @Override
  public String getName() {
    return _name;
  }

  @Override
  public boolean hasJobs() {
    return _jobs.length > 0;
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public Collection<MonitoringJob> getJobs() {
    if (_jobs.length > 0) { // also makes a memory barriers to get a "safe" view of
      MonitoringJob[] jobs = _jobs;
      return Arrays.asList(jobs);
    }
    return Collections.EMPTY_LIST;
  }
  
  @Override
  public void stopAllJobs() {
    for (MonitoringJob job : getJobs()) {
      job.stop();
    }
  }
  
  @Override
  public void updated() {
    MonitoringJob[] jobs = _jobs; // This also makes a memory barrier to get a "safe" view of registered listeners.
    if (jobs.length > 0) { 
      for (MonitoringJob job : jobs) {
        ((MonitoringJobImpl) job).updated(this);
      }
    }
  }
  
  @Override
  public MonitoringJob startJob(MeterListener<?> listener, Object context, Executor exec) {
    MonitoringJobImpl job = new MonitoringJobImpl(listener, context, exec, -1, 0);
    return job.start();
  }
  
  @Override
  public MonitoringJob startScheduledJob(MeterListener<?> listener, Object context, Executor exec, long schedule,
                                         int count) {
    MonitoringJobImpl job = new MonitoringJobImpl(listener, context, exec, schedule, count);
    return job.start();
  }
  
  @Override
  public String toString() {
    return new StringBuilder(_name).append("[").append(getValue()).append("]").toString();
  }  
  
  @Override
  public StopWatch startWatch(boolean force) {
    if (_jobs.length == 0 && !force) {
      return NullStopWatch.instance();
    }
    
    final long startTime = System.nanoTime();
    
    return new StopWatch() {
      public void close() {
        switch (getType()) {
        case INCREMENTAL:        
          inc(System.nanoTime() - startTime);
          break;
        case ABSOLUTE:
          set(System.nanoTime() - startTime);
          break;
          default:
            // noop
        }
      }
    };
  }
  
  private synchronized void addJob(MonitoringJob job) {
    int length = _jobs.length;
    MonitoringJob jobs[] = new MonitoringJob[length + 1];
    System.arraycopy(_jobs, 0, jobs, 0, length);
    jobs[length] = job;
    _jobs = jobs; // make a fence memory barrier to publish a safe view of the listeners array
  }
  
  public synchronized void removeJob(MonitoringJob job) {
    int found = -1;
    int length = _jobs.length;
    for (int i = 0; i < length; i++) {
      if (job.equals(_jobs[i])) {
        found = i;
        break;
      }
    }
    if (found != -1) {
      MonitoringJob jobs[] = new MonitoringJob[length - 1];
      System.arraycopy(_jobs, 0, jobs, 0, found);
      System.arraycopy(_jobs, found + 1, jobs, found, jobs.length - found);
      _jobs = jobs; // make a fence memory barrier to publish a safe view of the listeners array
    }
  }
  
  @SuppressWarnings("rawtypes")
  class MonitoringJobImpl implements MonitoringJob, Runnable {
    private volatile Object _context;
    private final MeterListener _listener;
    private final Executor _executor;
    private final int _count;
    private final long _schedule;
    private volatile ScheduledFuture<?> _future;
    private volatile boolean _started;
    private volatile long _counted;
    
    public MonitoringJobImpl(MeterListener listener, Object context, Executor executor, long schedule, int count) {
      _listener = listener;
      _context = context;
      _schedule = schedule;
      _count = count;
      _executor = (executor != null) ? executor : ((schedule == -1) ? new SerialExecutor(_logger) : null);
    }
    
    @Override
    public boolean isRunning() {
      return _started;
    }
    
    public void updated(final Meter meter) {
      if (_schedule == -1) {
        _executor.execute(new Runnable() {
          @SuppressWarnings("unchecked")
          public void run() {
            if (_started)
              _context = _listener.updated(meter, _context);
          }
        });
      }
    }
    
    public MonitoringJob start() {
      Runnable task = null;
      
      synchronized (this) {
        if (_started) {
          throw new IllegalStateException("job already started");
        }
        
        if (_schedule != -1) {
          Runnable redirect = _executor == null ? this : new Runnable() {
            @Override
            public void run() {
              _executor.execute(MonitoringJobImpl.this);
            }};
          _future = _timer.scheduleAtFixedRate(redirect, 0L, _schedule, TimeUnit.MILLISECONDS);
        } else {
          task = new Runnable() {
            @SuppressWarnings("unchecked")
            public void run() {
              if (_started)
                _context = _listener.updated((Meter) MeterImpl.this, _context);
            }
          };
        }
        _started = true;
      }
      
      if (task != null) {
        _executor.execute(task);
      }
      addJob(this);
      return this;
    }
    
    @Override
    public synchronized void stop() {
      if (_started) {
        try {
          if (_future != null) {
            _future.cancel(false);
          }
          removeJob(this);
        } finally {
          _started = false;
        }
      }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getContext() {
      return (T) _context;
    }
    
    @Override
    public Meter getMeter() {
      return MeterImpl.this;
    }
    
    @Override
    public Executor getExecutor() {
      return _executor;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
      if (_started == false) return;
      _context = _listener.updated((Meter) MeterImpl.this, _context);
      if (_count > 0 && (++_counted) >= _count) {
        stop();
      }
    }
  }
}
