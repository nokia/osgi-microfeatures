// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.http;

// Jdk
import java.io.OutputStream;

// HttpAPI
import com.nextenso.proxylet.ProxyletException;

/**
 * A pushlet is a kind of proxylet which has the ability to retain the http client socket and push
 * some chunks of data at any time, from any threads.
 * A BufferedHttpRequestPushlet is a puhslet that needs a full request to perform its job.
 * <p/>It implies buffering the whole body prior to calling the proxylet.
 */
public interface BufferedHttpRequestPushlet extends HttpRequestProxylet {
  /**
   * The proxy will call the same pushlet again.
   */
  public static final int SAME_PROXYLET = 9999;
  /**
   * @deprecated Use SAME_PROXYLET instead of this variable.
   * @see SAME_PUSHLET
   */
  public static final int SAME_PUSHLET = SAME_PROXYLET;
  /**
   * The Engine will return to the first proxylet in the request chain.
   */
  public static final int FIRST_PROXYLET = 10000;
  /**
   * The Engine will send response headers to the client and headers will go through the response proxylets chain.
   */
  public static final int RESPOND_FIRST_PROXYLET = 1001;
  /**
   * The Engine will send response headers to the client and the response will NOT go through the response-chain 
   * proxylets.
   */
  public static final int RESPOND_LAST_PROXYLET = 1002;  
  
  /**
   * Processes the buffered request and send response chunks asynchronously. 
   * The engine calls this method after having received the whole http request.
   * The pushlet is able to send at any time some chunks using request.getResponse().getOutputStream(). The socket 
   * may be closed at any time by closing the http response output stream.
   * <br/>Returns one of the predefined codes to specify what the Engine should do next with the request.
   * @param request the buffered request to process
   * @return SAME_PROXYLET, FIRST_PROXYLET, RESPOND_FIRST_PROXYLET or RESPOND_LAST_PROXYLET. If this method returns
   *	     either RESPOND_FIRST_PROXYLET, or RESPOND_LAST_PROXYLET, then you must setup the response headers 
   *	     using the request.getResponse().getProlog/getHeaders() methods.
   * @throws ProxyletException if any problem occurs while processing the request
   */
  public int doRequest(HttpRequest request)
    throws ProxyletException;
}
