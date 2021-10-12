// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.agent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nextenso.mux.MuxContext;
import com.nextenso.mux.MuxHandler;
import com.nextenso.proxylet.engine.Context;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.IntHashtable;
import alcatel.tess.hometop.gateways.utils.Log;

public class MuxHandlerEnv {
  private final String _scope;
  private final int _scopeIndex;
  private final String _protocol;
  private final MuxHandler _muxHandler;
  private final IntHashtable _flags;
  private final Reactor _reactor;
  private final PlatformExecutor _queue;
  private final Set<String> _stacks = new HashSet<String>();
  private final static Log _logger = Log.getLogger("callout");
  private volatile int _stackCount = -1;
  private final MuxHandlerDesc _muxHandlerDesc;
  private final PlatformExecutors _execs;
  private final AtomicInteger _queueCounter = new AtomicInteger(0);
  
  @SuppressWarnings("unchecked")
  public MuxHandlerEnv(PlatformExecutors execs, MuxHandler muxHandler, MuxHandlerDesc mhd, IntHashtable flags, String protocol,
                       Reactor reactor, String scope, PlatformExecutor queue) {
    _protocol = protocol;
    _muxHandler = muxHandler;
    _muxHandlerDesc = mhd;
    _flags = flags;
    _reactor = reactor;
    _scope = scope;
    _queue = queue;
    _execs = execs;
    
    if (_scope != null) {
      Pattern p = Pattern.compile("[a-zA-Z_]*([0-9]+)");
      Matcher m = p.matcher(_scope);
      if (m.matches()) {
        _scopeIndex = Integer.valueOf(m.group(1));
      } else {
        throw new IllegalStateException("invalid composite scope, which does not end with some digits: "
            + _scope);
      }
    } else {
      _scopeIndex = 1;
    }

    muxHandler.getMuxConfiguration().put("reactor", reactor);
  }
  
  public boolean isConnectedTo(String stackInstance, String stackAddress, int stackPort) {
    return _stacks.contains(getStackKey(stackInstance, stackAddress, stackPort));
  }
  
  public MuxHandler getMuxHandler() {
    return _muxHandler;
  }
  
  public MuxHandlerDesc getMuxHandlerDesc() {
    return _muxHandlerDesc;
  }
  
  public IntHashtable getFlags() {
    return _flags;
  }
  
  public String getProtocol() {
    return _protocol;
  }
  
  public Reactor getReactor() {
    return _reactor;
  }
  
  public Executor getExecutor() {
    return _queue == null ? _reactor : _queue;
  }
  
  /**
   * Returns the executor used to handle a given mux connection for a given mux handler.
   */
  public PlatformExecutor getQueue() {
	boolean threadSafe = (Boolean) _muxHandler.getMuxConfiguration().get(MuxHandler.CONF_THREAD_SAFE);
	return threadSafe ? _execs.createQueueExecutor(_execs.getProcessingThreadPoolExecutor(), String.valueOf(_queueCounter.incrementAndGet())) : _queue;
  }
  
  public String getScope() {
    return _scope;
  }
  
  public int getScopeIndex() {
    return _scopeIndex;
  }
  
  public void init(final Config config) {
    // Initialize the mux handler within its own reactor thread.   
    try {
      _muxHandler.init(config);
    }
    
    catch (Throwable t) {
      _logger.warn("Got exception while initializing mux hanlder " + _muxHandler, t);
      if ("true".equals(System.getProperty("system.failstop", "true"))) {
        System.exit(1);
      }
    }
  }
  
  public void init(final int appId, final String appName, final String instName, final MuxContext muxCtx) {
    try {
      Context.setMuxHandlerLocal(_muxHandler);
      _muxHandler.init(appId, appName, instName, muxCtx);
    }
    
    catch (Throwable t) {
      _logger.error("Can't initialize mux handler: " + _muxHandler, t);
      if ("true".equals(System.getProperty("system.failstop", "true"))) {
        System.exit(1);
      }
    }
  }
  
  public void initializeMaxStackCount() {
    _stackCount = ((Integer) _muxHandler.getMuxConfiguration().get(MuxHandler.CONF_CONNECTION_NUMBER)).intValue();
  }
  
  public boolean mayAddStack() {
      return _stackCount != 0;
  }
  
  public void addStack(String stackInstance, String stackAddress, int stackPort) {
    if (_stackCount == 0) {
      throw new IllegalStateException("Max number of mux handler connections reached");
    }
    String key = getStackKey(stackInstance, stackAddress, stackPort);
    if (_stacks.contains(key)) {
      throw new IllegalStateException("MuxHandler " + _muxHandler + " already connected to stack "
          + stackInstance);
    }
    _logger.debug("addStack: stackInstance=" + stackInstance + ", stackAddress=" + stackAddress + ", stackPort=" + stackPort);
    _stacks.add(key);
    _stackCount--;
  }
  
  public void removeStack(String stackInstance, String stackAddr, int stackPort) {
    boolean removed = _stacks.remove(getStackKey(stackInstance, stackAddr, stackPort));
    if (removed) {
        _logger.debug("Removed stack %s (%s:%d)", stackInstance, stackAddr, stackPort);
        _stackCount++;
    } else {
    	_logger.debug("removeStack: stack instance not found for stackInstance %s, stackAddr %s, stackPort %d", 
    			stackInstance, stackAddr, stackPort);
    }
  }
  
  public void destroy() {
    // Destroy the mux handler using its reactor
    Runnable task = new Runnable() {
      public void run() {
        try {
          MuxHandlerLogger.calloutLogger.info("Destroying MuxHandler: " + _muxHandler.getAppName());
          _muxHandler.destroy();
        }
        
        catch (Throwable t) {
          _logger.warn("Got exception while destroying mux handler " + _muxHandler, t);
        }
      }
    };
    getExecutor().execute(task);
  }
  
  public static String getStackKey(String stackInstance, String stackAddress, int stackPort) {
	  StringBuilder sb = new StringBuilder();
	  sb.append(stackInstance).append("/").append(stackAddress).append(":").append(stackPort);
	  return sb.toString();
  }
}
