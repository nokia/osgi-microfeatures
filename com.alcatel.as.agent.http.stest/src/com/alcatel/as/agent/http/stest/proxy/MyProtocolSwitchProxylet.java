// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.agent.http.stest.proxy;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;

import org.apache.felix.dm.annotation.api.Component;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.HttpBody;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpRequestProlog;
import com.nextenso.proxylet.http.HttpUtils;
import com.nextenso.proxylet.http.StreamedHttpRequestProxylet;

@Component
public class MyProtocolSwitchProxylet implements StreamedHttpRequestProxylet  {
  
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
    if(headers.getHeader("X-Server-Hop") != null) {
      prolog.getRequest().setNextServer(new InetSocketAddress("127.0.0.1", 8089));
    } else {
      try {
        prolog.getURL().setPort(8080);
      } catch (MalformedURLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      if(prolog.getProtocol().endsWith("2.0")) {
        prolog.setProtocol(HttpUtils.HTTP_11);
      } else {
        prolog.setProtocol(HttpUtils.HTTP_20);
      }
    }
    return NEXT_PROXYLET;
  }

  @Override
  public void doRequestBody(HttpBody body, boolean isLastChunk) throws ProxyletException {
    // TODO Auto-generated method stub
    
  }

}
