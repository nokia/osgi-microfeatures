package alcatel.tess.hometop.gateways.reactor.impl;

// Jdk
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.util.SynchronousTimer;
import alcatel.tess.hometop.gateways.reactor.util.SynchronousTimerTask;

import com.alcatel.as.service.concurrent.TimerService;

/**
 * Class used to schedule legacy timers in the reactor thread. 
 */
@SuppressWarnings("deprecation")
public class ReactorTimer {
  private final static Logger _logger = Logger.getLogger("as.service.reactor.timer");
  private TimerService _timerService;
  
  ReactorTimer(TimerService timerService) {
    _timerService = timerService;
  }
  
  /**
   * A wrapper around a ScheduledFuture that blocks until the
   * Scheduled Future is available.
   */
  static class FutureWrapper implements Future<Object> {
    private Future<?> _future;
    
    Future<?> ensureFuture() {
      synchronized (this) {
        while (_future == null) {
          try {
            wait();
          } catch (InterruptedException ie) {
            // we were interrupted so hopefully we will now have a
            // service registration ready; if not we wait again
          }
        }
        // check if we're in an illegal state and throw an exception
        return _future;
      }
    }
    
    void setFuture(Future<?> future) {
      synchronized (this) {
        _future = future;
        notifyAll();
      }
    }
    
    @Override
    public int hashCode() {
      return ensureFuture().hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof FutureWrapper) {
        return ensureFuture().equals(((FutureWrapper) obj).ensureFuture());
      }
      return ensureFuture().equals(obj);
    }
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return ensureFuture().cancel(mayInterruptIfRunning);
    }
    
    @Override
    public boolean isCancelled() {
      return ensureFuture().isCancelled();
    }
    
    @Override
    public boolean isDone() {
      return ensureFuture().isDone();
    }
    
    @Override
    public Object get() throws InterruptedException, ExecutionException {
      return ensureFuture().get();
    }
    
    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
        TimeoutException {
      return ensureFuture().get(timeout, unit);
    }
  }
  
  public ScheduledFuture<?> schedule(Executor executor, Runnable task, long delay, TimeUnit unit) {
    return _timerService.schedule(executor, task, delay, unit);
  }
        
  // deprecated
  public void schedule(Executor executor, SynchronousTimerTask task, long delay) {
    if (delay < 0)
      throw new IllegalArgumentException("delay can't be negative");
    SynchronousTimer.setScheduledExecutionTime(task, System.currentTimeMillis() + delay);
    FutureWrapper fw = new FutureWrapper();
    SynchronousTimer.setFuture(task, fw);
    Future<?> f = schedule(executor, task, delay, TimeUnit.MILLISECONDS);
    fw.setFuture(f);
  }
  
  // deprecated; similar to java.util.Timer.scheduledAtFixedRate
  public void schedule(Executor executor, final SynchronousTimerTask task, long initDelay, final long delay) {
    if (initDelay < 0)
      throw new IllegalArgumentException("delay can't be negative");
    if (delay < 0)
      throw new IllegalArgumentException("perod can't be negative");
    final long firstExecutionTime = System.currentTimeMillis() + initDelay;
    SynchronousTimer.setScheduledExecutionTime(task, firstExecutionTime);
    Runnable wrap = new Runnable() {
      int _executionNumber;
      
      @Override
      public void run() {
        try {
          task.run();
          SynchronousTimer.setScheduledExecutionTime(task, firstExecutionTime
              + ((++_executionNumber) * delay));
        } catch (Throwable t) {
          _logger.warn("got unexpected exception on synchronous timer task: " + task, t);
        }
      }
    };
    FutureWrapper fw = new FutureWrapper();
    SynchronousTimer.setFuture(task, fw);
    Future<?> sf = _timerService.scheduleAtFixedRate(executor, wrap, initDelay, delay, TimeUnit.MILLISECONDS);
    fw.setFuture(sf);
  }
}
