// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.client.impl;

import java.io.IOException;

import com.nextenso.http.agent.Utils;
import com.nextenso.http.agent.client.HttpConnection;
import com.nextenso.http.agent.client.HttpSocket;
import com.nextenso.http.agent.client.HttpSocketHandler;
import com.nextenso.http.agent.impl.HttpRequestFacade;
import com.nextenso.http.agent.impl.HttpResponseFacade;
import com.nextenso.http.agent.parser.HttpParser;

/**
 * This client uses the stack to perform the fectch.
 */

public class RedirectClient implements HttpSocketHandler {
  
  private HttpRequestFacade req;
  private final Utils utils;
  private boolean notified;
  
  public RedirectClient(HttpRequestFacade req, Utils utils) {
    this.req = req;
    this.utils = utils;
  }
  
  public void redirect() throws Throwable {
    HttpConnection connection = HttpConnection.getConnection(utils);
    
    if (connection == null)
      throw new IOException("No Http Stack Available");
    
    HttpSocket socket = connection.open(this);
    
    synchronized (this) {
      notified = false;
      req.writeTo(socket.getOutputStream(), req.getProxyMode());
      try {
    	  while (! notified) {
    		  wait();
    	  }
      } catch (InterruptedException e) {
          throw new IOException(e.toString());
      }
    }
  }
  
  /****************** HttpSocketHandler **********/
  
  public boolean handleHttpSocket(HttpSocket s) {
    try {
    	//TODO handle socket exceptions here!
      if (s.getHttpParser().parseResponse(req.getProlog().getMethod(), s.getInputStream(),
                                          (HttpResponseFacade) req.getResponse()) == HttpParser.PARSED) {
        
        synchronized (this) {
          notified = true;
          notify();
        }
        return false;
      }
      
      // Keep handling the socket
      return true;
    } catch (Exception e) {
      
      synchronized (this) {
        notified = true;
        notify();
      }
      return false;
    }
  }

  @Override
  public HttpRequestFacade getRequest() {
    return req;
  }
  
}
