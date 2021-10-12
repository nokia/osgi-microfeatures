// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.agent.http.stest.proxy;

import org.apache.felix.dm.annotation.api.Component;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.BufferedHttpResponseProxylet;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpResponse;
import com.nextenso.proxylet.http.HttpResponseProlog;
import com.nextenso.proxylet.http.HttpSession;

@Component
public class MyBufferedStatefulResponseProxylet implements BufferedHttpResponseProxylet  {
  
  @Override
  public int accept(HttpResponseProlog arg0, HttpHeaders arg1) {
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
  public int doResponse(HttpResponse response) throws ProxyletException {
    HttpSession session = response.getSession();
    
    System.out.println("session.MaxInactiveInterval : " + session.getMaxInactiveInterval());
    Integer counter = (Integer) session.getAttribute("counter");
    if (counter == null) {
      counter = new Integer(1);
    } else {
      counter = new Integer(counter.intValue() + 1);
    }
    session.setAttribute("counter", counter);
    
    response.getHeaders().setIntHeader("X-Stateful-Counter", counter);
    response.getBody().appendContent("\nCounter: " + counter + "\n");

    return NEXT_PROXYLET;
  }


}
