package com.alcatel.as.agent.http.stest.proxy;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.HttpBody;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpRequestProlog;
import com.nextenso.proxylet.http.StreamedHttpRequestProxylet;
@Component
public class MyAsyncProxylet implements StreamedHttpRequestProxylet {


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
    return "Test HTTP Proxylet - Suspend";
  }

  @Override
  public void init(ProxyletConfig arg0) throws ProxyletException {
    
  }


  @Override
  public int doRequestHeaders(HttpRequestProlog prolog, HttpHeaders headers) throws ProxyletException {
    headers.addHeader("X-alexa", "play-despacito");
    
    exec.submit(() -> {
      try {
        Thread.sleep(1_000L);
        headers.addHeader("x-resumed-by", Thread.currentThread().getName());
        System.out.println("resumed");
        headers.getRequest().resume(NEXT_PROXYLET);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    
    return SUSPEND;
  }

  @Override
  public void doRequestBody(HttpBody body, boolean isLastChunk) throws ProxyletException {
    // TODO Auto-generated method stub
    
  }

}
