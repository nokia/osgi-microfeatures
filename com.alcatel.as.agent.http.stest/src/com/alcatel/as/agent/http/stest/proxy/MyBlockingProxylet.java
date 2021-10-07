package com.alcatel.as.agent.http.stest.proxy;

import org.apache.felix.dm.annotation.api.Component;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.HttpBody;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpRequestProlog;
import com.nextenso.proxylet.http.StreamedHttpRequestProxylet;

@Component
public class MyBlockingProxylet implements StreamedHttpRequestProxylet {

  @Override
  public int accept(HttpRequestProlog arg0, HttpHeaders arg1) {
    return ACCEPT_MAY_BLOCK;
  }

  @Override
  public void destroy() {   
  }

  @Override
  public String getProxyletInfo() {
    return "Test Http Proxylet";
  }

  @Override
  public void init(ProxyletConfig arg0) throws ProxyletException {    
  }


  @Override
  public int doRequestHeaders(HttpRequestProlog prolog, HttpHeaders headers) throws ProxyletException {
    headers.addHeader("X-alexa", "play-despacito");
    return NEXT_PROXYLET;
  }

  @Override
  public void doRequestBody(HttpBody body, boolean isLastChunk) throws ProxyletException {
    // TODO Auto-generated method stub
  }
}
