// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.impl;

// Jdk
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.impl.Meters.ReactorMeters;
import alcatel.tess.hometop.gateways.reactor.util.SynchronousTimerTask;

/**
 * This is the reactor io event dispatcher, which demultiplex io events
 * to channel listeners.
 */
@SuppressWarnings("javadoc")
public class ReactorImpl implements Reactor {
  // Default logger used when no logger is provided when creating a Reactor.
  private final static Logger _defLogger = Logger.getLogger("as.service.reactor.Reactor");
  
  // The Reactor logger
  private final Logger _logger;
  
  // The Reactor timer manager.
  private final ReactorTimer _timer;
  
  // Tells if our reactor is started.
  private final AtomicBoolean _started = new AtomicBoolean();
  
  // Our Reactor name (may be auto-generated if use does not provide one).
  private volatile String _name;
  
  // Counter used to generate reactor names.
  private static final AtomicLong _idGenerator = new AtomicLong(0);
  
  // Flag telling if a name was given to our Reactor, during object creation.
  private final boolean _hasName;
  
  // Our Reactor provider impl 
  private final ReactorProviderImpl _provider;
  
  // Executor used to dispatch input events to listener reactor thread.
  private final PlatformExecutor _queue;
  
  // Our worker thread queue
  private volatile Thread _threadQueue;
  
  // Latch used to shutdown the legacy run method, when the reactor is stopped.
  private final CountDownLatch _stopLatch = new CountDownLatch(1);

  // Our Reactor Meters
  private final ReactorMeters _meters;

  /**
   * Executor used to schedule timers using high priority.
   */
  Executor _timerExecutor = new Executor() {
    @Override
    public void execute(Runnable command) {
      schedule(command, TaskPriority.HIGH);
    }
  };

  /**
   * Creates a new Reactor.
   * 
   * @param name the reactor name. If null, then we'll create a unique name, but in this case,
   *            we won't use the meterng service (because we need a reactor name for the
   *            metering service.
   * @param logger The logger used by this reactor.
   * @param provider the provided used to create this reacor
   * @throws IOException if the reactor could not be properly initialized
   */
  public ReactorImpl(String name, Logger logger, final ReactorProviderImpl provider) throws IOException {
    logger = (logger == null) ? _defLogger : logger;
    _logger = logger;
    _provider = provider;
    _timer = new ReactorTimer(provider.getStrictTimerService());
    _meters = provider.getMeters().newReactorMeters(name);
    
    // We only use the metering service if our reactor has a name.
    _hasName = (name != null);
    _name = (name == null) ? "Reactor-" + _idGenerator.incrementAndGet() : name;
    
    // Initialize the default Reactor input handler thread
    _queue = _provider.getExecutors().createThreadQueueExecutor(name, new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, _name + "-reactor");
      }
    });
    
    final CountDownLatch latch = new CountDownLatch(1);
    _queue.execute(new Runnable() {
      @Override
      public void run() {
        _threadQueue = Thread.currentThread();
        provider.setReactorThreadLocal(ReactorImpl.this);
        latch.countDown();
      }
    });
    
    try {
      if (!latch.await(30000, TimeUnit.MILLISECONDS)) {
        throw new IOException("Could not initialize reactor executor thread");
      }
    } catch (InterruptedException e) {
      throw new IOException("could not initialize reactor executor", e);
    }
    
    // now schedule the initialization of our meters in our queue. we need to schedule because the meters initialization will register our 
    // meters into the registry, and the current thread is synchronized (see create method). So we definitely don't want to register a service
    // while holding a lock.
    schedule(() -> {
    	_meters.updated();
    });
  }
  
  @Override
  public String toString() {
    return _name;
  }
  
  // ---------------- Reactor interface ---------------------------------------------------
  
  /**
   * Returns the name given to our Reactor during object creation. If null was given, then the
   * name has been generated by our constructor and has the form "Reactor-Id". If no name was
   * given, then the hasName() method returns false.
   */
  public String getName() {
    return _name;
  }
  
  public boolean hasName() {
    return _hasName;
  }
  
  public void setName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("a Reactor name can't be null");
    }
    _provider.aliasReactor(_name, null);
    _provider.aliasReactor(name, this);
    _name = name;
  }
  
  public void start() {
    if (!_started.compareAndSet(false, true)) {
      throw new IllegalStateException("reactor is already started");
    }
  }
  
  public void stop() {
    if (_logger.isInfoEnabled()) {
      _logger.info("Closing reactor " + _name);
    }
    
    _stopLatch.countDown(); // wake up legacy run method.  
    _provider.aliasReactor(_name, null);
    _queue.shutdown();
    _provider.closeReactorChannels(this, false);     
    _logger.info("Reactor " + _name + " closed");
  }
  
  public Logger getLogger() {
    return _logger;
  }
  
  public <T> Future<T> schedule(Callable<T> c) {
    return schedule(c, TaskPriority.DEFAULT);
  }
  
  public <T> Future<T> schedule(Callable<T> c, TaskPriority pri) {
    RunnableFuture<T> ftask = new FutureTask<T>(c);
    schedule(ftask, pri);
    return ftask;
  }
  
  public ScheduledFuture<?> schedule(final Runnable task, long delay, TimeUnit unit) {
    return _queue.schedule(task, delay, unit);
  }
  
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initDelay, long delay, TimeUnit unit) {
    return _queue.scheduleAtFixedRate(task, initDelay, delay, unit);
  }
  
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initDelay, long delay, TimeUnit unit) {
    return _queue.scheduleWithFixedDelay(task, initDelay, delay, unit);
  }
  
  public <T> ScheduledFuture<T> schedule(Callable<T> callable, long delay, TimeUnit unit) {
    return _queue.schedule(callable, delay, unit);
  }
  
  @SuppressWarnings("deprecation")
  public void schedule(SynchronousTimerTask task, long delay) {
    _timer.schedule(_timerExecutor, task, delay);
  }
  
  @SuppressWarnings("deprecation")
  public void schedule(SynchronousTimerTask task, long delay, long period) {
    _timer.schedule(_timerExecutor, task, delay, period);
  }
  
  /**
   * Schedules a task in the reactor thread, where application listeners 
   * are executed.
   */
  public void execute(final Runnable task) {
    schedule(task, TaskPriority.DEFAULT);
  }
  
  /**
   * Schedules a task in the reactor thread, where application listeners are executed.
   */
  public void schedule(final Runnable task) {
    schedule(task, TaskPriority.DEFAULT);
  }
  
  public void schedule(final Runnable task, TaskPriority pri) {
    switch (pri) {
    case DEFAULT:
      _queue.execute(task, ExecutorPolicy.SCHEDULE);
      break;
    case HIGH:
      _queue.execute(task, ExecutorPolicy.SCHEDULE_HIGH);
      break;
    }
  }
  
  /**
   * Schedules a task in the reactor thread, where application listeners are executed.
   * If the current thread is the reactor thread, then the task is invoked in the context
   * of the caller thread.
   */
  public void scheduleNow(Runnable task) {
    scheduleNow(task, TaskPriority.DEFAULT);
  }
  
  public void scheduleNow(Runnable task, TaskPriority pri) {
    switch (pri) {
    case DEFAULT:
      _queue.execute(task, ExecutorPolicy.INLINE);
      break;
    case HIGH:
      _queue.execute(task, ExecutorPolicy.INLINE_HIGH);
      break;
    }
  }
  
  public Thread getReactorThread() {
    return _threadQueue;
  }
  
  public void close() {
    stop();
  }
  
  public void disconnect(final boolean abort) {
    _provider.closeReactorChannels(this, abort);
  }
  
  @SuppressWarnings("deprecation")
  public boolean cancel(SynchronousTimerTask task) {
    return task.cancel();
  }
  
  /****************** Package methods **************************************/
  
  ReactorProviderImpl getReactorProvider() {
    return _provider;
  }
  
  boolean isStarted() {
    return _started.get();
  }
  
  @Override
  public PlatformExecutor getPlatformExecutor() {
    return _queue;
  }
  
  /**
   * Legacy method, not used anymore (the start method should be used to start the reactor).
   */
  @Override
  public void run() {
    if (!_started.compareAndSet(false, true)) {
      throw new IllegalStateException("Reactor " + _name + " already started");
    }
    
    try {
      _stopLatch.await();
    } catch (InterruptedException e) {
    }
  }
  
  NioSelector getSelector(AtomicInteger counter) {
    return _provider.getSelector(counter);
  }
  
  ReactorMeters getMeters() {
    return _meters;
  }
}
