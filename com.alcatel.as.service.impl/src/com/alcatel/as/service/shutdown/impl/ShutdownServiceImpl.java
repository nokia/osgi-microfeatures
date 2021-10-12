// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.shutdown.impl;

import org.osgi.service.component.annotations.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import com.alcatel.as.service.shutdown.*;
import com.alcatel.as.service.concurrent.*;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.osgi.service.event.*;
import org.osgi.framework.BundleContext;

import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.MSelectProperty;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

@Component(property = { EventConstants.EVENT_TOPIC + "=" + ShutdownService.SHUTDOWN_TOPIC }, configurationPolicy=ConfigurationPolicy.REQUIRE)
@Config(rootSnmpName = "alcatel.srd.a5350.CalloutAgent", rootOid = { 637, 71, 6, 110 },
        section = "Shutdown Service Parameters")
public class ShutdownServiceImpl implements ShutdownService, EventHandler {
  
  public static int DEF_EXIT_STATUS = 0;
  
  private final static Logger logger = Logger.getLogger("as.service.shutdown");
  
  private boolean _shuttingDown;
  private List<Shutdownable> _shutdownables = new ArrayList<Shutdownable>();
  private PlatformExecutor _exec;
  private PlatformExecutors _execs;
  private EventAdmin _eventAdmin;
  private Dictionary _system;
  private Future _shutdownTask;
  private long _minDelay = 0L;
  private long _maxDelay = 10000L;
  
  @StringProperty(
               title = "Graceful Shutdown Timeout",
               help = "Defines the maximum delay in milliseconds to run graceful shutdown services " +
               "when the agent is stopped. Setting it to 0 will deactivate the graceful shutdown mechanism." +
               "Recommended value >= 3000. A minimum value can also be set : for ex : \"1000-5000\" enforces " +
               "a minimum delay of 1 second and a maximum delay of 5 seconds.",
               oid = 135, snmpName = "GracefulShutdownTimeout", 
               required = false, dynamic = true, defval = "5000")
  public static final String GS_TIMEOUT = "agent.gsTimeout";
    
  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind="unbindShutdownable")
  public void shutdownable(Shutdownable shutdownable) {
    register(shutdownable);
  }

  public void unbindShutdownable(Shutdownable shutdownable) {
  }    
  
  public boolean register(Shutdownable shutdownable) {
    synchronized (this) {
      if (_shuttingDown)
        return false;
      logger.info("ShutdownService : register : " + shutdownable);
      _shutdownables.add(shutdownable);
    }
    return true;
  }
  
  @Reference
  public void setEventAdmin(EventAdmin evt) {
    _eventAdmin = evt;
  }
  
  @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, target = "(service.pid=system)")
  public void setSystemConfig(Dictionary conf) {
    _system = conf;
  }

  public void unsetSystemConfig(Dictionary conf) {
  }
  
  @Activate
  @Modified
  private void start(Map<String, String> conf) {
    synchronized (this) {
      if (_shuttingDown)
        return; // to avoid an update while shutting down
    }
    long minDelay = _minDelay;
    long maxDelay = _maxDelay;
    String delays = ConfigHelper.getString(conf, GS_TIMEOUT, "0-" + _maxDelay);
    try {
      logger.debug("ShutdownService : parsing gsTimeout : " + delays);
      delays = delays.replace(" ", "");
      if (delays.startsWith("-"))
        delays = "0" + delays;
      int index = delays.indexOf('-');
      if (index == -1) {
        minDelay = 0L;
        maxDelay = Long.parseLong(delays);
      } else {
        minDelay = Long.parseLong(delays.substring(0, index));
        if (index == (delays.length() - 1))
          maxDelay = minDelay + 50; // give an extra 50 ms by precaution
        else
          maxDelay = Long.parseLong(delays.substring(index + 1));
      }
      if (minDelay < 0L)
        minDelay = 0L;
      if (maxDelay < minDelay)
        maxDelay = minDelay > 0 ? minDelay + 50L : 0L;
      // we set when no more exception to expect
      _minDelay = minDelay;
      _maxDelay = maxDelay;
      logger.info("ShutdownService : set min/max delays : " + _minDelay + "/" + _maxDelay);
    } catch (Exception e) {
      logger.error("ShutdownService : invalid gsTimeout : " + delays, e);
    }
  }
  
  @Reference
  public void setPlatformExecs(PlatformExecutors execs) {
    _execs = execs;
    _exec = execs.createQueueExecutor(execs.getProcessingThreadPoolExecutor());
  }
    
  public void sendShutdownEvent(java.util.Map<String, String> props) {
    _eventAdmin.postEvent(new Event(SHUTDOWN_TOPIC, props));
  }
  
  public void sendShutdownEvent(String instanceKey, String instanceValue) {
    Map<String, String> props = new HashMap<>();
    if (instanceKey != null && instanceValue != null)
      props.put(instanceKey, instanceValue);
    sendShutdownEvent(props);
  }
  
  public void handleEvent(Event event) {
    Object o = event.getProperty(SHUTDOWN_TARGET_INSTANCE_ID);
    boolean isLocal = true;
    if (o == null) {
      o = event.getProperty(SHUTDOWN_TARGET_INSTANCE_NAME);
      if (o != null)
        isLocal = isLocal(SHUTDOWN_TARGET_INSTANCE_NAME, o.toString());
    } else {
      isLocal = isLocal(SHUTDOWN_TARGET_INSTANCE_ID, o.toString());
    }
    if (isLocal) {
      long delay = 0L;
      o = event.getProperty(SHUTDOWN_DELAY);
      if (o != null) {
        try {
          delay = Long.parseLong(o.toString());
        } catch (Exception e) {
          logger.warn("ShutdownService : failed to parse requested delay : shutdown immediate");
        }
      }
      String msg = "ShutdownService : received local EventAdmin";
      synchronized (this) {
        if (_shutdownTask != null) {
          msg = msg + " : cancelling pending shutdown";
          _shutdownTask.cancel(true);
        }
        if (delay < 0L) {
          logger.warn(msg);
          return;
        }
        Runnable r = new Runnable() {
          public void run() {
            shutdown("EventAdmin");
          }
        };
        msg = msg + " : scheduling shutdown in " + delay + " seconds";
        _shutdownTask = _exec.schedule(r, delay, java.util.concurrent.TimeUnit.SECONDS);
        logger.warn(msg);
      }
    } else
      logger.info("ShutdownService : received EventAdmin for remote instance : " + o);
  }
  
  private boolean isLocal(String key, String value) {
    return value.equals(_system.get(key).toString());
  }
  
  public void halt(int exitStatus, boolean dumpThreads) {
    if (dumpThreads) {
      dumpThreads();
    }
    logger.warn("ShutdownService : halt : " + exitStatus);
    LogManager.shutdown();
    Runtime.getRuntime().halt(exitStatus);
  }
  
  private void dumpThreads() {
    StringBuilder sw = new StringBuilder();
    sw.append("ShutdownService Threads dump:\n");
    try {
      Map<Thread, StackTraceElement[]> mapStacks = Thread.getAllStackTraces();
      Iterator<Thread> threads = mapStacks.keySet().iterator();
      while (threads.hasNext()) {
        Thread thread = threads.next();
        StackTraceElement[] stes = mapStacks.get(thread);
        sw.append("\nThread [" + thread.getName() + " prio=" + thread.getPriority() + "] --> StackTrace elements ...\n");
        for (StackTraceElement ste : stes) {
          sw.append("\t" + ste.toString() + "\n");
        }
      }
      
      logger.warn(sw.toString());
    } catch (Throwable t) {
      logger.warn("Exception while dumping threads", t);
    }
  }
  
  public boolean shutdown(Object src) {
    return shutdown(src, DEF_EXIT_STATUS);
  }

  public boolean shutdown(Object src, final int exitStatus){
    return shutdown (src, new Runnable (){
	public void run (){
	  System.exit (exitStatus);
	}
	public String toString (){ return "System.exit("+exitStatus+")";}
      });
  }
  
  public boolean shutdown(Object src, Runnable onCompleted){
    synchronized (this) {
      if (_shuttingDown)
        return false;
      _shuttingDown = true;
    }
    if (_minDelay > 0L) {
      Shutdownable minDelay = new Shutdownable() {
        Shutdown _shutdown;
        
        public void shutdown(Shutdown shutdown) {
          this._shutdown = shutdown;
          Runnable r = new Runnable() {
            public void run() {
              done();
            }
          };
          _execs.getThreadPoolExecutor().schedule(r, _minDelay, TimeUnit.MILLISECONDS);
        }
        
        public void done() {
          _shutdown.done(this);
        }
        
        public String toString() {
          return "MinShutdown[" + _minDelay + "]";
        }
      };
      _shutdownables.add(minDelay);
    }
    logger.warn("ShutdownService : shutdown : onCompleted=" + onCompleted + ", minDelay=" + _minDelay + ", maxDelay="
        + _maxDelay + ", src=" + src + ", Shutdownables : " + _shutdownables);
    ShutdownImpl shutdown = new ShutdownImpl(src, onCompleted);
    _exec.execute(shutdown);
    return true;
  }
  
  private class ShutdownImpl implements Shutdown, Runnable {
    private Object _src;
    private AtomicLong _delay;
    private Runnable _onCompleted;
    private long _startDate;
    private java.util.concurrent.Future _exitFuture;
    private boolean _exited;
    
    private ShutdownImpl(Object src, Runnable onCompleted) {
      _src = src;
      _onCompleted = onCompleted;
      _delay = new AtomicLong(_maxDelay);
      _startDate = System.currentTimeMillis();
    }
    
    public Object getSource() {
      return _src;
    }
    
    public long getExitDelay() {
      return _delay.get();
    }
    
    public long increaseExitDelay(long value) { // this was removed from API by precaution
      logger.warn("ShutdownService : increaseExitDelay : " + value);
      return _delay.addAndGet(value);
    }
    
    public long getRemainingExitDelay() {
      return Math.max(_startDate + _delay.get() - System.currentTimeMillis(), 0);
    }
    
    private void exit() {
      logger.warn("ShutdownService : exiting with callback " + _onCompleted + " : pending Shutdownables : " + _shutdownables);
      _exited = true;
      _onCompleted.run ();
    }
    
    public Runnable _exitTask = new Runnable() {
      public void run() {
        long remaining = getRemainingExitDelay();
        if (remaining > 50) { // we dont care for less than 50 ms - and avoid edge effects of timers
          _exec.schedule(this, remaining, java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
          exit();
        }
      }
    };
    
    public void done(final Shutdownable shutdownable) {
      Runnable doneTask = new Runnable() {
        public void run() {
          _shutdownables.remove(shutdownable);
          logger.info("ShutdownService : " + shutdownable + " is done : remainings=" + _shutdownables.size());
          if (_shutdownables.size() == 0 &&
	      !_exited) {
            _exitFuture.cancel(true);
            exit();
          }
        }
      };
      _exec.execute(doneTask);
    }
    
    public void run() {
      for (Shutdownable shutdownable : _shutdownables) {
        try {
          logger.info("ShutdownService : calling : " + shutdownable);
          shutdownable.shutdown(this);
        } catch (Throwable t) {
          logger.warn("ShutdownService : exception while calling : " + shutdownable, t);
        }
      }
      if (_shutdownables.size() == 0 || _delay.get() == 0)
        exit();
      else
        _exitFuture = _exec.schedule(_exitTask, _delay.get(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }
  }
}
