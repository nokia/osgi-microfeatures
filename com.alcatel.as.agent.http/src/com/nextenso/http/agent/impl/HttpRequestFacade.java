// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Optional;

import alcatel.tess.hometop.gateways.utils.ByteBuffer;
import alcatel.tess.hometop.gateways.utils.Constants;
import alcatel.tess.hometop.gateways.utils.Utils;

import com.nextenso.agent.event.AsynchronousEvent;
import com.nextenso.agent.event.AsynchronousEventScheduler;
import com.nextenso.http.agent.parser.HttpRequestHandler;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpRequestProlog;
import com.nextenso.proxylet.http.HttpResponse;
import com.nextenso.proxylet.http.HttpURL;
import com.nextenso.proxylet.http.HttpUtils;

public class HttpRequestFacade extends HttpMessageFacade implements HttpRequest, HttpRequestProlog,
    HttpRequestHandler {
  // the initial size for content
  public static int CONTENT_INIT_SIZE = 128; // 128 for request
  // the max size for content when reseting
  public static int CONTENT_MAX_SIZE = 4096; // 4K
  
  public HttpRequestFacade() {
    super();
    content = new ByteBuffer(CONTENT_INIT_SIZE);
  }
  
  /**************************************************************
   * Extension of HttpMessageFacade
   **************************************************************/
  
  public final boolean isRequest() {
    return true;
  }
  
  public HttpRequest getRequest() {
    return this;
  }
  
  public int getId() {
    return (this.id);
  }
  
  public HttpResponse getResponse() {
    return this.response;
  }
  
  protected void firstLineToString(StringBuilder buf, boolean usingProxy) {
    buf.append(method);
    buf.append(Constants.SPACE);
    if (url != null) {
      // Support https
      if (method.equals(HttpUtils.METHOD_CONNECT)) {
        buf.append(url.getHost()).append(':').append(url.getPort());
      } else {
        if (usingProxy) {
          buf.append(url.toString());
        } else {
          buf.append(url.getFile());
        }
      }
    }
    
    buf.append(Constants.SPACE);
    buf.append(protocol);
    buf.append(Constants.CRLF);
  }
  
  /**************************************************************
   * Implementation of com.nextenso.proxylet.http.HttpRequestProlog
   **************************************************************/
  
  public String getMethod() {
    return (this.method);
  }
  
  public void setMethod(String method) {
    this.method = Utils.toASCIIUpperCase(method);
  }
  
  public HttpURL getURL() {
    return (this.url);
  }
  
  public void setURL(HttpURL url) {
    this.url = url;
  }
  
  public String getParameter(String name) {
    HttpURL url = getURL();
    
    if (url.getParameterSize(name) > 1) {
      ArrayList<?> arr = url.getParameterValues(name);
      return ((String) arr.get(0));
    } else {
      return (url.getParameterValue(name, false));
    }
  }
  
  public Enumeration<?> getParameterNames() {
    return (getURL().getParameterNames());
  }
  
  public ArrayList<?> getParameterValues(String name) {
    return (getURL().getParameterValues(name));
  }
  
  /**************************************************************
   * Implementation of com.nextenso.proxylet.http.HttpRequest
   **************************************************************/
  
  public HttpRequestProlog getProlog() {
    return this;
  }
  
  public void setNextHop(String value) {
    this.nextHop = (value != null) ? value : NEXT_HOP_DEFAULT;
    this.nextHopModified = true;
  }
  
  public String getNextHop() {
    return this.nextHop;
  }
  
  /*************************************************************
   * Implementation of com.nextenso.http.agent.parser.HttpRequestHandler
   *************************************************************/
  
  @Override
  public void addHttpHeader(String name, String val) {
    if (name.equalsIgnoreCase("x-casr-scheme_auth")) {
      try {
    	// parse scheme
    	int colon = val.indexOf(':');
    	if (colon != -1) {
    		String scheme = val.substring(0, colon);
    		String auth = val.substring(colon+1);
    		StringBuilder sb = new StringBuilder();
    		sb.append(scheme).append("://").append(auth).append(url.getFile());
    		url = new HttpURL(sb.toString());
    	}
      } catch (Exception e) {
        // unlikely
        // url remains the same
      }
    } else {
    	super.addHttpHeader(name, val);
    }
  }
  
  public void setHttpRequestMethod(String method) {
    setMethod(method);
  }
  
  public void setHttpRequestUri(String uri, boolean relativeUrl) throws java.net.MalformedURLException {
    setProxyMode(!relativeUrl); // We are in proxy mode If the url does not start with /
    
    // Support https
    if (method.equals(HttpUtils.METHOD_CONNECT)) {
      url = new HttpURL("https://" + uri);
    } else {
      if (relativeUrl) {
    	// this is the old code, but the addHttpHeader will override the url using proper scheme/auth.
    	// TODO: rework the code below (possibly remove it).
        if (uri.startsWith("/"))
          url = new HttpURL("http://localhost" + uri);
        else
          url = new HttpURL("http://localhost/" + uri);
      } else {
        url = new HttpURL(uri);
      }
    }
  }
  
  public void setHttpRequestUrlAuthority(String host) throws java.net.MalformedURLException {
    url.setAuthority(host);
  }
  
  /***********************************************
   * Misc. methods
   ***********************************************/
  
  public void setProxyMode(boolean mode) {
    this.proxyMode = mode;
  }
  
  public boolean getProxyMode() {
    return this.proxyMode;
  }
  
  public void setId(int id) {
    this.id = id;
  }
  
  public void setResponse(HttpResponseFacade response) {
    this.response = response;
  }
  
  public boolean nextHopModified() {
    return this.nextHopModified;
  }
  
  public void writeTo(OutputStream out, boolean usingProxy) throws IOException {
    boolean hasContent = (content.size() > 0);
    if (isContentCompliant(hasContent))
      setHeader(HttpUtils.CONTENT_LENGTH, String.valueOf(content.size()));
    else {
      if (hasContent)
        throw new HttpMessageException("Method " + method + " must have an empty body (url=" + url + ")");
      removeHeader(HttpUtils.CONTENT_LENGTH);
    }
    removeChunkedHeader();
    super.writeTo(out, usingProxy);
  }
  
  public void writeHeadersTo(OutputStream out, boolean usingProxy) throws IOException {
    // if the method does not support content
    // then we are in an IllegalState
    if (!isContentCompliant(true))
      throw new HttpMessageException("Method " + method + " must have an empty body (url=" + url + ")");
    // Support https
    //if (chunked){
    if (chunked && !method.equals(HttpUtils.METHOD_CONNECT)) {
      addChunkedHeader();
      removeHeader(HttpUtils.CONTENT_LENGTH);
    } else
      chunked = hasChunkedHeader();
    super.writeHeadersTo(out, usingProxy);
  }
  
  public String toString(boolean withBody, boolean usingProxy) {
    if (getHeader(HttpUtils.USER_AGENT) == null) {
      setHeader(HttpUtils.USER_AGENT, Constants.DEF_UA);
    }
    
    setHeader(HttpUtils.HOST, getURL().getAuthority());
    
    return (super.toString(withBody, usingProxy));
  }
  
  public void clientDisconnected() {
    if (getListenersSize() == 0 && response.getListenersSize() == 0)
      return;
    AsynchronousEventScheduler.schedule(new AsynchronousEvent(this, null, EVENT_CLIENT_DISC));
  }
  
  public void serverDisconnected() {
    if (getListenersSize() == 0 && response.getListenersSize() == 0)
      return;
    AsynchronousEventScheduler.schedule(new AsynchronousEvent(this, null, EVENT_SERVER_DISC));
  }
  
  public void serviceTimeout() {
    if (getListenersSize() == 0 && response.getListenersSize() == 0)
      return;
    AsynchronousEventScheduler.schedule(new AsynchronousEvent(this, null, EVENT_SERVICE_TO));
  }
  
  public void aborted() {
    if (getListenersSize() == 0 && response.getListenersSize() == 0)
      return;
    AsynchronousEventScheduler.schedule(new AsynchronousEvent(this, null, EVENT_ABORT));
  }
  
  public boolean isContentCompliant(boolean hasContent) {
    if (method.equalsIgnoreCase(HttpUtils.METHOD_GET))
      return false;
    if (method.equalsIgnoreCase(HttpUtils.METHOD_POST))
      return true;
    if (method.equalsIgnoreCase(HttpUtils.METHOD_OPT))
      return hasContent;
    if (method.equalsIgnoreCase(HttpUtils.METHOD_HEAD))
      return false;
    if (method.equalsIgnoreCase(HttpUtils.METHOD_TRACE))
      return true;
    if (method.equalsIgnoreCase(HttpUtils.METHOD_PUT))
      return true;
    if (method.equalsIgnoreCase(HttpUtils.METHOD_DELETE))
      return false;
    // custom method
    return true;
  }
  @Override
  public void setNextProxy(InetSocketAddress addr) {
  	this.nextProxy = addr;
  }

  @Override
  public void setNextServer(InetSocketAddress addr) {
	this.nextServer = addr;
  }

  @Override
  public Optional<InetSocketAddress> getNextProxy() {
  	return Optional.ofNullable(nextProxy);
  }

  @Override
  public Optional<InetSocketAddress> getNextServer() {
    return Optional.ofNullable(nextServer);
  }
    
  
  /**********************************************
   * Class fields
   **********************************************/
  
  private String method = HttpUtils.METHOD_GET;
  private int id = -1;
  private HttpResponseFacade response;
  private HttpURL url;
  private String nextHop = NEXT_HOP_DEFAULT;
  private boolean nextHopModified = false;
  private InetSocketAddress nextProxy;
  private InetSocketAddress nextServer;
  
  // the proxyMode is a flag that is used by the http agent to remember if
  // the request was received in proxy mode or reverse proxy mode
  protected boolean proxyMode = true;


}
