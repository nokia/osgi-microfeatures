package alcatel.tess.hometop.gateways.reactor.impl;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.TimerService;

import alcatel.tess.hometop.gateways.reactor.AsyncChannel;
import alcatel.tess.hometop.gateways.reactor.Channel;

/**
 * NIO Events dispatcher.
 */
public class NioSelector implements Runnable {
  // Our logger
  private final static Logger _logger = Logger.getLogger("as.service.reactor.NioSelector");
  
  // Our selector name
  private final String _name;
  
  // Our NIO selector thread
  private final Selector _selector;
    
  // List of tasks scheduled within the selector thread.
  private final ConcurrentLinkedQueue<Runnable> _selectorTasks = new ConcurrentLinkedQueue();
  
  // The Reactor timer manager.
  private final TimerService _timer;
  
  // Flag telling if we are stopped;
  private boolean _stopped;
  
  // Flag telling if we are shutting down
  private final AtomicBoolean _shuttingDown = new AtomicBoolean();
  
  // The Worker thread which is currently running our selector
  private volatile Thread _thread;
  
  // Latch used to check if our Selector thread is fully started
  private final CountDownLatch _latch = new CountDownLatch(1);
  
  // Meters
  private final Meters.SelectorMeters _meters;
  
  NioSelector(String name, TimerService timer, Meters.SelectorMeters meters) throws IOException {
    _name = name;
    _selector = Selector.open();
    _timer = timer;
    _meters = meters;
  }
  
  void start(Executor exec) {
    exec.execute(this);
    try {
      if (!_latch.await(30000, TimeUnit.MILLISECONDS)) {
        throw new RuntimeException("Could not initialize selector " + _name);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("Could not initialize selector " + _name, e);
    }
  }
  
  void shutdown() {
    if (!_shuttingDown.compareAndSet(false, true)) {
      return;
    }
    
    Callable<Throwable> callable = new Callable() {
      @Override
      public Throwable call() {
        try {
          _shutdown();
          return null;
        }
        
        catch (Throwable t) {
          return t;
        }
      }
    };
    
    FutureTask<Throwable> ft = new FutureTask(callable);
    scheduleNow(ft);
    
    Throwable err = null;
    try {
      err = ft.get();
    } catch (Throwable e) {
      err = e;
    }
    
    if (err != null) {
      _logger.warn("Could not close selector " + _name, err);
    } else if (_logger.isInfoEnabled()) {
      _logger.info("Selector " + _name + " closed");
    }
  }
  
  public int getCount() {
	  return _selector.keys().size();
  }
  
  public void run() {
    _thread = Thread.currentThread();
    int selected = 0;
    ArrayList<SelectionKey> lowPriList = new ArrayList();
    _logger.info("Starting selector thread: " + _name);
    
    _latch.countDown();
    
    try {
      while (!_stopped) {
        // Wait for IO event readiness
        selected = 0;
        selected = _selector.select();
        
        // Run tasks scheduled in the selector thread (via the scheduledInSelector method).
        runSelectorTasks();
        
        // Loop on selected keys.
        if (selected > 0) {
          loopOnSelectedKeys(lowPriList);
        }
      }
    }
    
    catch (Throwable t) {
      if (!_stopped) {
        _logger.error("Reactor: got unexpected exception in selector thread: " + _name, t);
        shutdown();
      }
    }
    
    finally {
      _logger.info("Selector " + _name + " stopped");
    }
  }
  
  /**
   * Wakeup the selector thread, if it is blocked in a select operation.
   */
  void wakeup() {
    _selector.wakeup();
  }
  
  /**
   * Schedules a task in the selector thread. This method is typically used when 
   * the selection keys must be changed outside the selector thread.
   * 
   * @param task the task to be scheduled in the selector thread.
   */
  void schedule(Runnable task) {
    _selectorTasks.offer(task);
    wakeup();
  }
  
  /**
   * Returns selector meters
   */
  Meters.SelectorMeters getMeters() {
	  return _meters;
  }
  
  /**
   * Schedules a task in the future, in the selector thread.
   * 
   * @param task the task to be scheduled in the future, in the selector thread.
   */
  ScheduledFuture<?> schedule(final Runnable task, long delay, TimeUnit unit) {
    return _timer.schedule(new Executor() {
      @Override
      public void execute(Runnable command) {
        schedule(command);
      }
    }, task, delay, unit);
  }
  
  /**
   * Schedules a task in the selector thread, or invoke the runnable if 
   * the current thread is the selector thread.
   * @param task the task to be scheduled in the selector thread.
   */
  void scheduleNow(Runnable task) {
    if (Thread.currentThread() == _thread) {
      task.run();
      return;
    }
    schedule(task);
  }
  
  /**
   * Register a handler listening to socket events (used by clients/servers).
   */
  SelectionKey registerSelectHandler(final AbstractSelectableChannel channel, final int ops,
                                     final SelectHandler l) throws IOException {
    Callable<Object> callable = new Callable() {
      @Override
      public Object call() {
        try {
          return channel.register(_selector, ops, l);
        }
        
        catch (Throwable t) {
          return t;
        }
      }
    };
    
    FutureTask<Throwable> ft = new FutureTask(callable);
    scheduleNow(ft);
    Object result = null;
    
    try {
      result = ft.get();
    } catch (Throwable t) {
      result = t;
    }
    if (result instanceof IOException) {
      throw (IOException) result;
    } else if (result instanceof RuntimeException) {
      throw (RuntimeException) result;
    } else if (result instanceof Throwable) {
      throw new IOException("Could not register channel in selector", (Throwable) result);
    }
    return (SelectionKey) result;
  }
  
  /**
   * Closes all channels managed by this selector.
   * @param reactor null of all channels must be closed, or a given reactor whose channels have to be closed.
   * @param abort true if the channel must be aborted, false if the channel must be gracefully closed.
   */
  void closeReactorChannels(final ReactorImpl reactor, final boolean abort) {
    Callable<Throwable> callable = new Callable() {
      public Throwable call() {
        try {
          closeReactorChannelsInSelector(reactor, abort);
          return null;
        }
        
        catch (Throwable t) {
          return t;
        }
      }
    };
    
    FutureTask<Throwable> ft = new FutureTask(callable);
    scheduleNow(ft);
    
    Throwable err = null;
    try {
      err = ft.get();
    } catch (Throwable e) {
      err = e;
    }
    
    if (err != null) {
      _logger
          .warn("Could not close reactor channels (" + reactor.getName() + ") from selector " + _name, err);
    } else if (_logger.isInfoEnabled()) {
      _logger.info("Closed reactor channels (" + reactor.getName() + ") from selector " + _name);
    }
  }
  
  private void closeReactorChannelsInSelector(final ReactorImpl reactor, final boolean abort) {
    for (SelectionKey key : _selector.keys()) {
      SelectableChannel channel = key.channel();
      try {
        if (channel != null) {
          Object attachment = key.attachment();
          if (attachment instanceof Channel) {
            Channel c = (Channel) attachment;
            if (reactor != null && c.getReactor() != reactor) {
              continue;
            }
            if (_logger.isInfoEnabled()) {
              _logger.info("Closing Channel: " + channel);
            }
            if (abort) {
              ((Channel) attachment).shutdown();
            } else {
              ((Channel) attachment).close();
            }
          } else {
            if (_logger.isInfoEnabled()) {
              _logger.info("Closing Channel: " + channel);
            }
            channel.close();
          }
        }
      } catch (IOException e) {
      }
    }
  }
  
  private void _shutdown() { // called from selector thread
    if (_stopped) {
      return;
    }
    try {
      _stopped = true;
      
      if (_logger.isInfoEnabled()) {
        _logger.info("Stopping selector " + _name);
      }
      
      // Close sockets    
      for (SelectionKey key : _selector.keys()) {
        key.cancel();
        
        SelectableChannel channel = key.channel();
        if (_logger.isDebugEnabled()) {
          _logger.debug("Closing Channel: " + channel);
        }
        try {
          if (channel != null) {
            channel.close();
          }
        } catch (Throwable e) {
        }
      }
    } finally {
      try {
        if (_selector != null) {
          _selector.close();
        }
      } catch (Throwable t) {
      }
    }
  }
  
  private void runSelectorTasks() {
    ConcurrentLinkedQueue<Runnable> tasks = _selectorTasks;
    Runnable task;
    while ((task = tasks.poll()) != null) {
      try {
        task.run();
      }
      
      catch (Throwable err) {
        _logger.error("Caught unexpected exception while running scheduled reactor task", err);
      }
    }
  }
  
  private void loopOnSelectedKeys(ArrayList<SelectionKey> lowPriList) {
    // Loop on ready operation's list. First we run high priority events.
    lowPriList.clear();
    Set<SelectionKey> selected = _selector.selectedKeys();
    Iterator<SelectionKey> it = selected.iterator();
    
    while (it.hasNext()) {
      SelectionKey key = it.next();
      SelectHandler handler = (SelectHandler) key.attachment();
      
      if (handler != null) {
        if (handler.getPriority() == AsyncChannel.MAX_PRIORITY) {
          handler.selected(key);
        } else {
          lowPriList.add(key);
        }
      }
      
      it.remove();
    }
    
    // Run low priority events
    if (lowPriList.size() > 0) {
      handleLowPriEvents(lowPriList);
    }
  }
  
  private void handleLowPriEvents(ArrayList<SelectionKey> list) {
    for (int i = list.size(); --i >= 0;) {
      SelectionKey key = list.get(i);
      SelectHandler handler = (SelectHandler) key.attachment();
      handler.selected(key);
    }
  }
  
  public void dump(final StringBuilder sb) {
    final CountDownLatch latch = new CountDownLatch(1);
    scheduleNow(new Runnable() {
      public void run() {
        for (SelectionKey key : _selector.keys()) {
          SelectableChannel channel = key.channel();
          if (channel != null) {
            Object attachment = key.attachment();
            if (attachment instanceof Channel) {
              Channel c = (Channel) attachment;
              sb.append("\n").append(c.toString());
            }
          }
        }
        latch.countDown();
      }
    });
    
    try {
      latch.await();
    } catch (InterruptedException e) {
    }
  }
}
