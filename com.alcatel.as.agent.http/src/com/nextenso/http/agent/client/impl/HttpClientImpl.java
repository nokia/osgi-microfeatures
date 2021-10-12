// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.http.HttpAuthenticator;
import alcatel.tess.hometop.gateways.utils.ByteBuffer;
import alcatel.tess.hometop.gateways.utils.Utils;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.nextenso.http.agent.client.HttpConnection;
import com.nextenso.http.agent.client.HttpSocket;
import com.nextenso.http.agent.client.HttpSocketHandler;
import com.nextenso.http.agent.impl.HttpRequestFacade;
import com.nextenso.http.agent.parser.CookieParser;
import com.nextenso.http.agent.parser.HttpHeaderDescriptor;
import com.nextenso.http.agent.parser.HttpParser;
import com.nextenso.http.agent.parser.HttpResponseHandler;
import com.nextenso.mux.MuxHeaderV0;
import com.nextenso.proxylet.http.HttpClient;
import com.nextenso.proxylet.http.HttpCookie;
import com.nextenso.proxylet.http.HttpURL;
import com.nextenso.proxylet.http.HttpUtils;

public class HttpClientImpl implements HttpClient, HttpSocketHandler, HttpResponseHandler {
  
  public static final int REDIRECT_MAX = 5;
  public static final int AUTHENT_MAX = 3; // may go through proxies before reaching the server...
  private HttpSocket _pendingRequestSocket = null;
  private State _state = State.REQUEST_NOT_SENT;
  
  private enum State {
	  REQUEST_NOT_SENT,
	  WAITING_FOR_RESPONSE,
	  RESPONSE_RECEIVED,
	  CANCELED
  };

  public HttpClientImpl(HttpClientFactoryImpl hcf, String url) throws MalformedURLException {
    this.hcf = hcf;
    this.url = new HttpURL(url);
  }
  
  public HttpClientImpl(HttpClientFactoryImpl hcf, String url, String componentName)
                                                                                    throws MalformedURLException {
    this(hcf, url);
    checkLicense(componentName);
  }
  
  public HttpClientImpl(HttpClientFactoryImpl hcf, String url, HttpClient client)
                                                                                 throws MalformedURLException {
    this(hcf, url);
    if (client instanceof HttpClientImpl) {
      HttpClientImpl hci = (HttpClientImpl) client;
      if (hci.componentId != null)
        checkLicense(hci.componentId);
      credentials = hci.credentials;
    }
  }
  
  private void checkLicense(String componentName) {

  }
  
//  public void setSessionId(int id) {
//    sessionId = id & 0xFFFFFFFF;
//  }
  
  /**
   * Sets request header
   */
  public void setHeader(String name, String value) {
    initRequest();
    req.getHeaders().setHeader(name, value);
  }
  
  public void addCredentials(String realm, String user, String pswd) {
    credentials.put(realm, new Credentials(realm, user, pswd));
  }
  
  /**
   * Returns the HttpURL involved.
   */
  public HttpURL getURL() {
    return url;
  }
  
  /**
   * Performs a GET and returns the response code. The response can be obtained by calling
   * getResponse().
   */
  public int doGET() throws IOException {
    return run(HttpUtils.METHOD_GET, null, null);
  }
  
  public void doGET(HttpClient.Listener listener) {
    this.listener = listener;
    initRequest();
    req.getProlog().setMethod(HttpUtils.METHOD_GET);
    runAsync();
  }
  
  /**
   * Performs an OPTIONS and returns the response code. The response can be obtained by calling
   * getHeaders() and getResponse().
   */
  public int doOPTIONS() throws IOException {
    return run(HttpUtils.METHOD_OPT, null, null);
  }
  
  public void doOPTIONS(HttpClient.Listener listener) {
    this.listener = listener;
    initRequest();
    req.getProlog().setMethod(HttpUtils.METHOD_OPT);
    runAsync();
  }
  
  /**
   * Performs a POST and returns the response code. The response can be obtained by calling
   * getResponse().
   */
  public int doPOST(String content_type, byte[] body) throws IOException {
    return run(HttpUtils.METHOD_POST, content_type, body);
  }
  
  public void doPOST(String content_type, byte[] body, HttpClient.Listener listener) {
    this.listener = listener;
    initRequest();
    req.getProlog().setMethod(HttpUtils.METHOD_POST);
    if (body == null) {
      body = com.nextenso.http.agent.Utils.EMPTY;
    }
    if (content_type == null) {
      content_type = "application/x-www-form-urlencoded";
    }
    req.getHeaders().setHeader(HttpUtils.CONTENT_TYPE, content_type);
    req.getHeaders().setHeader(HttpUtils.CONTENT_LENGTH, String.valueOf(body.length));
    req.getBody().setContent(body, true);
    runAsync();
  }
  
  /**
   * Performs a PUT and returns the response code. The response can be obtained by calling
   * getResponse().
   */
  public int doPUT(String content_type, byte[] body) throws IOException {
    return run(HttpUtils.METHOD_PUT, content_type, body);
  }
  
  public void doPUT(String content_type, byte[] body, HttpClient.Listener listener) {
    this.listener = listener;
    initRequest();
    if (body == null)
      body = com.nextenso.http.agent.Utils.EMPTY;
    req.getProlog().setMethod(HttpUtils.METHOD_PUT);
    req.getHeaders().setHeader(HttpUtils.CONTENT_TYPE, content_type);
    req.getHeaders().setHeader(HttpUtils.CONTENT_LENGTH, String.valueOf(body.length));
    req.getBody().setContent(body, true);
    runAsync();
  }
  
  /**
   * Performs a DELETE and returns the response code. The response can be obtained by calling
   * getResponse().
   */
  public int doDELETE() throws IOException {
    return run(HttpUtils.METHOD_DELETE, null, null);
  }
  
  public void doDELETE(HttpClient.Listener listener) {
    this.listener = listener;
    initRequest();
    req.getProlog().setMethod(HttpUtils.METHOD_DELETE);
    runAsync();
  }
  
  /**
   * Returns the reponse from the server
   */
  public byte[] getResponse() {
    return body;
  }
  
  /**
   * Returns the headers from the response
   */
  @SuppressWarnings("rawtypes")
  public Hashtable getHeaders() {
    return headers;
  }
  
  /**
   * Returns the reason from the response (if any).
   */
  public String getReason() {
    return reason;
  }
  
  /**
   * Returns the status of the response.
   */
  public int getStatus() {
    return status;
  }
  
  public void attach(Object attachment) {
    this.attachment = attachment;
  }
  
  public Object attachment() {
    return this.attachment;
  }
  
  /****************** HttpSocketHandler **********/
  
  // curr thread = Http Reactor thread
  @Override
  public boolean handleHttpSocket(HttpSocket s) {
    try {
      synchronized (this) {
    	  if (_state == State.CANCELED) {
    		  logger.debug("response ignored (request canceled)");
    		  return false; // we are done with this socket.
    	  }
    	  _state = State.RESPONSE_RECEIVED;
      }
    	
      if (s.getHttpParser().parseResponse(req.getProlog().getMethod(), s.getInputStream(), this) == HttpParser.PARSED) {
        if (logger.isDebugEnabled()) {
          logger.debug("Response fully parsed");
        }
        
        if (buffer != null) {
          body = buffer.toByteArray(true);
          buffer.init();
        }
        
        if (status == 301 || status == 302 || status == 303) {
          if (redirects >= REDIRECT_MAX)
            throw new IOException("Maximum number of redirections reached. Abort.");
          
          try {
            String location = (String) headers.get(HttpUtils.LOCATION);
            if (location != null) {
              req.getProlog().getURL().setValue(location);
              redirects++;
              runAsync();
              return false;
            }
          }
          
          catch (MalformedURLException ex) {
            // ignore redirection
          }
        } else if (status == 401 || status == 407) {
          if (authentications >= AUTHENT_MAX)
            throw new IOException("Maximum number of authentications reached. Abort.");
          
          Credentials cred = handleStaleAuthenticate();
          if (cred != null) {
            req.getHeaders().setHeader(cred.proxy ? "Proxy-Authorization" : "Authorization",
                                       cred.authorize(req.getProlog().getMethod(), req.getProlog().getURL()
                                           .toString()));
            authentications++;
            runAsync();
            return false;
          } else {
            reason = "Invalid credentials";
          }
        }
        
        // Callback using the caller executor.
        requestCompleted();
        
        // We are done with this socket.
        return false;
      }
      
      // Keep handling the socket because the response is not fully parsed.
      return true;
    }
    
    catch (Throwable e) {
      if (buffer != null) {
        buffer.init();
      }
      requestFailed(e);
      return false;
    }
    
    finally {
    	// help garbage collector
    	_pendingRequestSocket = null;
    }
  }
  
  @Override
  public HttpRequestFacade getRequest() {
    return req;
  }

  /****************** HttpResponseHandler ********/
  
  public void setHttpProtocol(String protocol) {
    // ignore
  }
  
  @SuppressWarnings("unchecked")
  public void addHttpCookie(HttpCookie cookie) {
    String[] cookies = (String[]) headers.get("Set-Cookie");
    if (cookies == null)
      cookies = new String[0];
    String[] clone = new String[cookies.length + 1];
    System.arraycopy(cookies, 0, clone, 0, cookies.length);
    clone[cookies.length] = CookieParser.toString(cookie);
    headers.put("Set-Cookie", clone);
    if (logger.isDebugEnabled())
      logger.debug("HttpResponseHandler.addHttpCookie(" + Arrays.asList(clone) + ")");
  }
  
  @SuppressWarnings("unchecked")
  public void addHttpHeader(String name, String val) {
    name = Utils.capitalizeFirstLetter(name);
    String oldValue = (String) headers.get(name);
    if (oldValue != null)
      val = oldValue + ',' + val;
    
    headers.put(name, val);
    if (logger.isDebugEnabled())
      logger.debug("HttpResponseHandler.addHttpHeader(" + name + ":" + val + ")");
  }
  
  public void addHttpHeader(HttpHeaderDescriptor desc, String val) {
    addHttpHeader(desc.name(), val);
  }
  
  public void addHttpBody(InputStream in, int size) throws IOException {
    if (buffer == null)
      buffer = new ByteBuffer(size);
    buffer.append(in, size);
    if (logger.isDebugEnabled())
      logger.debug("HttpResponseHandler.addHttpBody(" + buffer.toString() + ")");
  }
  
  public void setHttpResponseStatus(int status) {
    this.status = status;
    if (logger.isDebugEnabled())
      logger.debug("HttpResponseHandler.setHttpResponseStatus(" + status + ")");
  }
  
  public void setHttpResponseReason(String reason) {
    this.reason = reason;
    if (logger.isDebugEnabled())
      logger.debug("HttpResponseHandler.setHttpResponseReason(" + reason + ")");
  }
  
  /****************** Private methods ********/
  
  private int run(String method, String content_type, byte[] body) throws IOException {
    checkSynchronousClient();
    SynchronousClientListener syncListener = new SynchronousClientListener();
    if (method == HttpUtils.METHOD_GET) {
      doGET(syncListener);
    } else if (method == HttpUtils.METHOD_OPT) {
      doOPTIONS(syncListener);
    } else if (method == HttpUtils.METHOD_DELETE) {
      doDELETE(syncListener);
    } else if (method == HttpUtils.METHOD_POST) {
      doPOST(content_type, body, syncListener);
    } else if (method == HttpUtils.METHOD_PUT) {
      doPUT(content_type, body, syncListener);
    } else {
      throw new IllegalArgumentException("Method not supported: " + method);
    }
    try {
      syncListener.waitForCompletion();
    } catch (IOException e) {
      status = -1;
      reason = null;
      headers.clear();
      body = com.nextenso.http.agent.Utils.EMPTY;
      throw e;
    }
    return status;
  }
  
  private void runAsync() {
    try {
      // First: create our executor used to callback the caller.
      if (_callbackExecutor == null) {
        _callbackExecutor = hcf.getPlatformExecutors().getCurrentThreadContext().getCallbackExecutor();
        _callbackCL = Thread.currentThread().getContextClassLoader();
      }
      
      resetResponse();
      
      if (componentId != null) {
        // Notify license manager that proxylet sent a request
        componentId = null;
      }
      
      if (!hcf.isActive(this)) {
        return;
      }
      
      setRandomConnection();
      resume();
    }
    
    catch (Throwable t) {
      requestFailed(t);
    }
  }
  
  public boolean setRandomConnection() {
    if (connection == null) {
      connection = hcf.getUtils().getHttpConnection();
    }
    return connection != null;
  }
  
  @Override
  public synchronized boolean cancel() {
	  logger.debug("cancel: state=" + _state + ", _pendingReq=" + _pendingRequestSocket + ", connection="  + connection);

	  // if request was not sent, return true (the request has been canceled).
	  // if request was sent and is pending, abort it and return true (request canceled)
	  // if request was sent and response was received, return false (too late, request could not be canceled)
	  switch (_state) {
	  case REQUEST_NOT_SENT:
		  _state = State.CANCELED;
		  break;
	  case WAITING_FOR_RESPONSE:
		  // abort pending request (if connection == null it means we have no stack currently available).
		  if (_pendingRequestSocket != null && connection != null) {
			  
			    MuxHeaderV0 header = new MuxHeaderV0();
			    int flags = com.nextenso.http.agent.Utils.CLIENT_FLAGS;
			    header.set(sessionId, _pendingRequestSocket.getSocketId(), com.nextenso.http.agent.Utils.CLOSED | flags);
			    connection.getAgent().sendClose(connection.getMuxCnx(), header);
			    _pendingRequestSocket = null;
		  }
		  _state = State.CANCELED;
		  break;

	  case RESPONSE_RECEIVED:
		  break;
	  }	  
	  return _state == State.CANCELED;
  }
  
  public synchronized void resume() {    
    if (_state == State.CANCELED) {
    	return;
    }
    
    try {
      if (connection == null) {
        throw new IOException("No http stack available");
      }
      
      if (logger.isInfoEnabled()) {
        logger.info("resuming http client: " + getURL() + " with cnx=" + connection);
      }
      
      if (sessionId == -1L) {
    	  _pendingRequestSocket = connection.open(this);
    	  sessionId = _pendingRequestSocket.getSessionId();
      } else {
    	  _pendingRequestSocket = connection.open(this, sessionId);
      }
      
      req.writeTo(_pendingRequestSocket.getOutputStream(), req.getProxyMode());
      _state = State.WAITING_FOR_RESPONSE;
    }
    
    catch (Throwable t) {
      _state = State.RESPONSE_RECEIVED;
      if (_pendingRequestSocket != null) {
        connection.close(_pendingRequestSocket); // connection can't be null. 
      }
      requestFailed(t);      
    }
  }
  
  private void resetResponse() {
    status = -1;
    reason = null;
    headers.clear();
    body = com.nextenso.http.agent.Utils.EMPTY;
  }
  
  private void checkSynchronousClient() {
    // Avoid deadlock if synchronous http request is performed within Http main thread.
	if (hcf.getPlatformExecutors().getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase("http")) {
      throw new IllegalStateException("Cannot use synchronous client in HTTP main thread. "
          + "(Your proxylet's accept method did not return ACCEPT_MAY_BLOCK)");
    }
  }
  
  private void initRequest() {
    if (req == null) {
      req = new HttpRequestFacade();
      req.getProlog().setURL(this.url);
      req.getProlog().setProtocol(HttpUtils.HTTP_11);
      req.getHeaders().setHeader(HttpUtils.CONNECTION, HttpUtils.KEEP_ALIVE);
    }
  }
  
  private Credentials handleStaleAuthenticate() throws IOException {
    String authenticate = null;
    boolean proxy = false;
    switch (status) {
    case 401:
      if ((authenticate = (String) headers.get("Www-Authenticate")) == null) {
        throw new IOException("Malformed 401 response: no WWW-Authenticate header");
      }
      break;
    
    case 407:
      if ((authenticate = (String) headers.get("Proxy-Authenticate")) == null) {
        throw new IOException("Malformed 407 response: no Proxy-Authenticate header");
      }
      proxy = true;
      break;
    }
    
    alcatel.tess.hometop.gateways.utils.Hashtable authParams = alcatel.tess.hometop.gateways.http.HttpUtils
        .extractAuthParams(authenticate);
    String realm = (String) authParams.get("realm");
    
    if (logger.isDebugEnabled())
      logger.debug("handleStaleAuthenticate: proxy=" + proxy + ", auth-header=" + authenticate + ", realm="
          + realm + ", authParams=" + authParams);
    
    if (realm == null) {
      throw new IOException("Malformed 401/407 response: no realm information");
    }
    
    Credentials c = credentials.get(realm);
    if (c == null) {
      return null;
      /*#IMSAS0CAG258547: do not throw new IOException("No credentials found for realm " + realm
      + ": Set credentials before calling GET/POST methods");*/
    }
    // create authenticator the first time only
    if (c.authenticator == null) {
      c.setAuthenticator(HttpAuthenticator.createInstance((String) authParams.get("scheme")), proxy);
    }
    // if true: it means that we may retry the request with the newly received nonce.
    if (c.authenticator.handleAuthenticate(authParams)) {
      return c;
    }
    // bad credentials, return null
    return null;
  }
  
  public static class Credentials {
    public final String realm, user, pswd;
    public HttpAuthenticator authenticator;
    public boolean proxy;
    
    public Credentials(String r, String u, String p) {
      realm = r;
      user = u;
      pswd = p;
    }
    
    public void setAuthenticator(HttpAuthenticator auth, boolean px) {
      authenticator = auth;
      proxy = px;
    }
    
    public String authorize(String method, String url) {
      return authenticator.authorize(user, pswd, realm, method, url);
    }
  }
  
  class SynchronousClientListener implements HttpClient.Listener {
    private Throwable _error;
    private boolean _completed;
    
    public synchronized void httpRequestCompleted(HttpClient client, int status) {
      _completed = true;
      notifyAll();
    }
    
    public synchronized void httpRequestFailed(HttpClient client, Throwable cause) {
      _error = cause;
      _completed = true;
      notifyAll();
    }
    
    public synchronized void waitForCompletion() throws IOException {
      try {
        while (!_completed) {
          wait();
        }
      } catch (InterruptedException e) {
        _error = e;
      }
      if (_error != null) {
        IOException ioe = new IOException("Unexpected exception in http client");
        ioe.initCause(_error);
        throw ioe;
      }
    }
  }
  
  private void requestCompleted() {
    if (logger.isDebugEnabled()) {
      logger.debug("http client (" + getURL() + ") completed");
    }
    Runnable task = new Runnable() {
      public void run() {
        Thread.currentThread().setContextClassLoader(_callbackCL);
        listener.httpRequestCompleted(HttpClientImpl.this, status);
      }
    };   
    callbackListener(task);
  }
  
  private void requestFailed(final Throwable t) {
    if (logger.isDebugEnabled()) {
      logger.debug("http client (" + getURL() + ") failed");
    }
    Runnable task = new Runnable() {
      public void run() {
        Thread.currentThread().setContextClassLoader(_callbackCL);
        listener.httpRequestFailed(HttpClientImpl.this, t);
      }
    };
    callbackListener(task);
  }
  
  private void callbackListener(Runnable task) {
    if (listener instanceof SynchronousClientListener) {
      // The listener thread is currently blocked in headers.wait and can't execute any
      // tasks.
      task.run();
    } else {
      // The client is using the async client (hence is not blocked).
      _callbackExecutor.execute(task, ExecutorPolicy.INLINE);
    }   
  }
  
  /****************** Class attributes ********/
  
  protected volatile HttpClientFactoryImpl hcf;
  protected volatile HttpConnection connection;
  protected volatile HttpRequestFacade req;
  protected volatile HttpURL url;
  protected volatile byte[] body;
  @SuppressWarnings("rawtypes")
  protected volatile Hashtable headers = new Hashtable();
  protected volatile Hashtable<String, Credentials> credentials = new Hashtable<String, Credentials>();
  protected volatile String reason;
  protected volatile int status;
  protected volatile String componentId = null;
  protected volatile long sessionId = -1L;
  protected volatile ByteBuffer buffer;
  private volatile int redirects;
  private volatile int authentications;
  private volatile HttpClient.Listener listener;
  private volatile Object attachment;
  private volatile PlatformExecutor _callbackExecutor; // executor used to call back
  private volatile ClassLoader _callbackCL; // caller class loader
  
  private static final Logger logger = Logger.getLogger("agent.http.client.HttpClientImpl");
}
