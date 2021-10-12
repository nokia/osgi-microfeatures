// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.agent.http.stest.proxy;

import java.net.InetSocketAddress;

import org.apache.felix.dm.annotation.api.Component;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.HttpBody;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpRequestProlog;
import com.nextenso.proxylet.http.StreamedHttpRequestProxylet;

@Component
public class MyProxylet implements StreamedHttpRequestProxylet  {

	@Override
	public int accept(HttpRequestProlog arg0, HttpHeaders arg1) {
		return ACCEPT;
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
    String nextPort = headers.getHeader("X-Next-Port");
    String nextHost = headers.getHeader("X-Next-Host");
    if(nextPort != null) {
      prolog.getRequest().setNextServer(new InetSocketAddress(
          nextHost == null ? "127.0.0.1" : nextHost,
          Integer.parseInt(nextPort)));
      System.out.println("next Server " + prolog.getRequest().getNextServer());
    }
    return NEXT_PROXYLET;
  }

  @Override
  public void doRequestBody(HttpBody body, boolean isLastChunk) throws ProxyletException {
    // TODO Auto-generated method stub
    
  }

}
