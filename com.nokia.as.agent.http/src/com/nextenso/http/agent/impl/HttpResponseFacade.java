// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.impl;

import java.io.IOException;
import java.io.OutputStream;

import alcatel.tess.hometop.gateways.utils.ByteBuffer;
import alcatel.tess.hometop.gateways.utils.Constants;
import alcatel.tess.hometop.gateways.utils.Recyclable;

import com.nextenso.http.agent.engine.PushletOutputStream;
import com.nextenso.http.agent.parser.HttpResponseHandler;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpResponse;
import com.nextenso.proxylet.http.HttpResponseProlog;
import com.nextenso.proxylet.http.HttpSession;
import com.nextenso.proxylet.http.HttpURL;
import com.nextenso.proxylet.http.HttpUtils;

public class HttpResponseFacade extends HttpMessageFacade implements HttpResponse, HttpResponseProlog,
    HttpResponseHandler, Recyclable {
  // the initial size for content
  public static int CONTENT_INIT_SIZE = 0; // 1K for response
  // the max size for content when reseting
  public static int CONTENT_MAX_SIZE = 4096; // 4K
  
  public static final Object ATTR_PUSHLET_OS = new Object();
  
  public HttpResponseFacade() {
    super(new ByteBuffer(CONTENT_INIT_SIZE));
  }
  
  /**************************************************************
   * Extension of HttpMessageFacade
   **************************************************************/
  
  public final boolean isRequest() {
    return false;
  }
  
  public HttpRequest getRequest() {
    return this.request;
  }
  
  public HttpResponse getResponse() {
    return this;
  }
  
  public int getId() {
    return request.getId();
  }
  
  public void firstLineToString(StringBuilder buf, boolean usingProxy) {
    buf.append(protocol);
    buf.append(Constants.SPACE);
    buf.append(status);
    buf.append(Constants.SPACE);
    if (reason == null) {
      buf.append(HttpUtils.getHttpReason(status));
    } else {
      buf.append(reason);
    }
    buf.append(Constants.CRLF);
  }
  
  public OutputStream getOutputStream() {
    OutputStream pushletOS = (OutputStream) getAttribute(ATTR_PUSHLET_OS);
    if (pushletOS == null) {
      return super.getOutputStream();
    }
    return pushletOS;
  }
  
  public void appendContent(java.nio.ByteBuffer ... bufs) throws IOException {
    PushletOutputStream pushletOS = (PushletOutputStream) getAttribute(ATTR_PUSHLET_OS);
    if (pushletOS == null) {
      super.appendContent(bufs);
      return;
    }
    pushletOS.write(bufs);
  }
  
  /**************************************************************
   * Implementation of com.nextenso.proxylet.http.HttpResponseProlog
   **************************************************************/
  
  public int getStatus() {
    return (this.status);
  }
  
  public void setStatus(int status) {
    this.status = status;
  }
  
  public String getReason() {
    return (this.reason);
  }
  
  public void setReason(String reason) {
    this.reason = reason;
  }
  
  public HttpURL getURL() {
    return getRequest().getProlog().getURL();
  }
  
  /**************************************************************
   * Implementation of com.nextenso.proxylet.http.HttpResponse
   **************************************************************/
  
  public HttpResponseProlog getProlog() {
    return this;
  }
  
  /*************************************************************
   * Implementation of com.nextenso.http.agent.parser.HttpResponseHandler
   *************************************************************/
  
  public void setHttpResponseStatus(int status) {
    setStatus(status);
  }
  
  public void setHttpResponseReason(String reason) {
    setReason(reason);
  }
  
  /*******************************************************
   * Implementation of com.nextenso.proxylet.http.HttpObject
   *******************************************************/

  @Override
  public String getRemoteAddr() {
	  return getRequest().getRemoteAddr();
  }
	  
  @Override
  public String getRemoteHost() {
	  return getRequest().getRemoteHost();
  }

  /***********************************************
   * Misc. methods
   ***********************************************/
  
  public void setRequest(HttpRequestFacade request) {
    this.request = request;
  }
  
  private void setResponseKeepAlive() {
    // we make sure we keep the client connection open, unless a proxylet has set a "connection" header.
    if (!connectionHeaderModified()) {
      setHeader(HttpUtils.CONNECTION, HttpUtils.KEEP_ALIVE);
    }
  }
  
  public void writeTo(OutputStream out, boolean usingProxy) throws IOException {
    // we make sure we keep the client connection open, unless a proxylet has set a "connection: close" header.
    setResponseKeepAlive();
    
    String method = request.getMethod();
    if (!method.equalsIgnoreCase(HttpUtils.METHOD_HEAD))
      setHeader(HttpUtils.CONTENT_LENGTH, String.valueOf(content.size()));
    else if (content.size() > 0)
      throw new HttpMessageException("A Response to a Method " + method + " must have an empty body");
    removeChunkedHeader();
    super.writeTo(out, usingProxy);
  }
  
  public void writeHeadersTo(OutputStream out, boolean usingProxy) throws IOException {
    // we make sure we keep the client connection open, unless a proxylet has set a "connection: close" header.
    setResponseKeepAlive();
    
    String method = request.getMethod();
    // Support https
    //if (chunked) {
    if (chunked && !method.equalsIgnoreCase(HttpUtils.METHOD_CONNECT)) {
      addChunkedHeader();
      removeHeader(HttpUtils.CONTENT_LENGTH);
    } else {
      chunked = hasChunkedHeader();
    }
    super.writeHeadersTo(out, usingProxy);
  }
  
  public String toString(boolean withBody, boolean usingProxy) {
    return (super.toString(withBody, usingProxy));
  }
  
  public boolean keepAlive(boolean isUsingProxy) {
    //
    // Keep-Alive http handling.
    //
    // In HTTP 1.0, the connection remains alive only if we have
    // Connection:keep-alive and/or proxy-connection:keep-alive
    //
    // In HTTP 1.1, All connections are kept alive, unless stated 
    // otherwise with the header "Connection: close".
    //
    String keep = null;
    boolean keepAlive = false;
    
    if (isUsingProxy) {
      keep = getHeader(HttpUtils.PROXY_CONNECTION);
    }
    
    if (keep == null) {
      keep = getHeader(HttpUtils.CONNECTION);
    }
    
    if (keep != null && keep.equalsIgnoreCase(Constants.KEEP_ALIVE)) {
      keepAlive = true;
    } else if (getProtocol() != null && getProtocol().equalsIgnoreCase(HttpUtils.HTTP_11) && keep == null) {
      //
      // in http 1.1: The connection is not persistent only if we have
      // "connection:close" and/or "proxy-connection:close".
      //
      keepAlive = true;
    }
    
    return (keepAlive);
  }
  
//  public void reset() {
//    super.reset();
//    status = -1;
//    reason = null;
//    request = null;
//    if (this.content.bufferSize() <= CONTENT_MAX_SIZE)
//      this.content.init();
//    else
//      this.content.init(CONTENT_INIT_SIZE);
//  }
  
  
  @Override
  public void recycled() {
	  status = -1;
	  reason = null;
	  super.recycled();
  }
  
  @Override
  public boolean isValid() {
  	return true;
  }

  /**********************************************
   * Class fields
   **********************************************/
  
  private int status = -1;
  private String reason;
  private HttpRequestFacade request;

}
