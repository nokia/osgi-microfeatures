// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.client;

import java.io.InputStream;
import java.io.OutputStream;

import com.nextenso.http.agent.parser.HttpParser;

/**
 * Interface for an asynchronous http stack socket.
 * An instance of this interface may be acquired using
 * the HttpConnection.getConnection() static method.
 */
public interface HttpSocket {
  /**
   * Returns a stream which is used to send an http request.
   * Possibly called multiple time when sending big http requests.
   */
  public OutputStream getOutputStream();
  
  /**
   * Return the socket input stream.
   */
  public InputStream getInputStream();
  
  /**
   * Return this socket id.
   */
  public int getSocketId();
  
  /**
   * Return this socket id.
   */
  public long getSessionId();
  
  /**
   * Return the http parser associated with this socket.
   * This parser may be used to parse http response using the 
   * getInputStream method.
   */
  public HttpParser getHttpParser();
  
}
