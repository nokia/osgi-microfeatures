// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.agent.http.stest.proxy;

import org.apache.felix.dm.annotation.api.Component;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.HttpBody;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpResponseProlog;
import com.nextenso.proxylet.http.StreamedHttpResponseProxylet;

@Component
public class MyResponseBlockingProxylet implements StreamedHttpResponseProxylet {

  @Override
  public void destroy() {   
  }

  @Override
  public String getProxyletInfo() {
    return "Test Http Response Proxylet";
  }

  @Override
  public void init(ProxyletConfig arg0) throws ProxyletException {    
  }


  @Override
  public int accept(HttpResponseProlog prolog, HttpHeaders headers) {
    return ACCEPT_MAY_BLOCK;
  }

  @Override
  public int doResponseHeaders(HttpResponseProlog prolog, HttpHeaders headers) throws ProxyletException {
    headers.addHeader("X-Hello", "World");
    headers.addHeader("X-Processed-By", Thread.currentThread().getName());

    return NEXT_PROXYLET;
  }

  @Override
  public void doResponseBody(HttpBody body, boolean isLastChunk) throws ProxyletException {
    
  }

}
