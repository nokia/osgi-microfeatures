// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.agent.event;

// Callout
import java.util.Dictionary;

import alcatel.tess.hometop.gateways.tracer.TracerManager;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.util.config.ConfigHelper;
import com.nextenso.agent.AgentProperties;

public class AsynchronousEventScheduler {
  private static PlatformExecutors _execs;
  
  // default number of worker threads used to handle asynchronous events
  private static final int DEF_WORKERS = 3;
  private static int _workers = DEF_WORKERS;
  
  @SuppressWarnings("rawtypes")
  public static void init(Dictionary cnf, PlatformExecutors execs) {
    _execs = execs;
    _workers = ConfigHelper.getInt(cnf, AgentProperties.ASYNCHRONOUS_READERS, DEF_WORKERS);
  }
  
  public static void schedule(final AsynchronousEvent event) {
    String queue = String.valueOf(System.identityHashCode(event) % _workers);
    _execs.getThreadPoolExecutor(queue).execute(new Runnable() {
      @Override
      public void run() {
        event.execute();
      }
    }, ExecutorPolicy.INLINE);
  }
}
