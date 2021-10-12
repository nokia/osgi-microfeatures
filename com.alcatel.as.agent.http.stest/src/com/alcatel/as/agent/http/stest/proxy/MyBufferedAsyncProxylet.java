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
import com.nextenso.proxylet.http.BufferedHttpRequestProxylet;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpRequestProlog;
@Component
public class MyBufferedAsyncProxylet implements BufferedHttpRequestProxylet {


  @ServiceDependency
  PlatformExecutors execs;
  
  PlatformExecutor exec;
  
  @Start
  public void start() {
    exec = execs.createQueueExecutor(execs.getProcessingThreadPoolExecutor());
  }
  
  @Override
  public int accept(HttpRequestProlog prolog, HttpHeaders headers) {
    return ACCEPT;
  }

  @Override
  public void destroy() {
    
  }

  @Override
  public String getProxyletInfo() {
    return "Test Buffered HTTP Proxylet - Suspend";
  }

  @Override
  public void init(ProxyletConfig arg0) throws ProxyletException {
    
  }

  @Override
  public int doRequest(HttpRequest request) throws ProxyletException {
    request.getHeaders().addHeader("X-alexa", "play-despacito");
    
    exec.submit(() -> {
      try {
        Thread.sleep(1_000L);
        request.getHeaders().addHeader("x-resumed-by", Thread.currentThread().getName());
        System.out.println("resumed");
        request.resume(NEXT_PROXYLET);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    
    return SUSPEND;
  }

}
