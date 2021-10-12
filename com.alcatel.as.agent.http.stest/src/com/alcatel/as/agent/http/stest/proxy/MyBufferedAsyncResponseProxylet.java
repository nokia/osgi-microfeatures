// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.agent.http.stest.proxy;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.BufferedHttpResponseProxylet;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpResponse;
import com.nextenso.proxylet.http.HttpResponseProlog;

@Component
public class MyBufferedAsyncResponseProxylet implements BufferedHttpResponseProxylet {

  @ServiceDependency
  PlatformExecutors execs;
  
  PlatformExecutor exec;
  
  @Start
  public void start() {
    exec = execs.createQueueExecutor(execs.getProcessingThreadPoolExecutor());
  }

  @Override
  public int accept(HttpResponseProlog prolog, HttpHeaders headers) {
    return ACCEPT;
  }

  @Override
  public void destroy() {
  }

  @Override
  public String getProxyletInfo() {
    return "Test Buffered HTTP Response Proxylet - Suspend";
  }

  @Override
  public void init(ProxyletConfig arg0) throws ProxyletException {
    
  }

  @Override
  public int doResponse(HttpResponse response) throws ProxyletException {
    response.getHeaders().addHeader("X-Hello", "World");
      
      exec.submit(() -> {
        try {
          Thread.sleep(1_000L);
          response.getHeaders().addHeader("x-resumed-by", Thread.currentThread().getName());
          System.out.println("resumed");
          response.resume(NEXT_PROXYLET);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      });
      
      return SUSPEND;
  }

}
