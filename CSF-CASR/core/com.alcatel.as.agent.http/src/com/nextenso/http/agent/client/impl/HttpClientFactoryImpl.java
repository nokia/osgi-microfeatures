package com.nextenso.http.agent.client.impl;

import java.net.MalformedURLException;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nextenso.http.agent.Utils;
import com.nextenso.proxylet.http.HttpClient;
import com.nextenso.proxylet.http.HttpClientFactory;

@Component(service = { HttpClientFactory.class }, property = { "type=mux" })
public class HttpClientFactoryImpl extends HttpClientFactory {
  private LinkedList<HttpClientImpl> _pendingClients = new LinkedList<HttpClientImpl>();
  private volatile Utils _utils;
  private boolean _isActive;
  private volatile PlatformExecutors _execs;
  private final static Logger _logger = Logger.getLogger("agent.http.client."
      + HttpClientFactoryImpl.class.getSimpleName());
  
  
  @Reference
  void bindPlatformExecutors(PlatformExecutors execs) {
	  _execs = execs;
  }
  
  PlatformExecutors getPlatformExecutors() {
	  return _execs;
  }
  
  // The http agent calls this method when it is initialized and when at least one mux cnx is available
  // (curr thread = http agent).
  public void activate(Utils utils) {
    synchronized (this) {
     if (_isActive) {
        return;
      }
      _isActive = true;
      _utils = utils;
    }
    
    // At this point: we know that the pending list can't be modified anymore
    if (_logger.isInfoEnabled()) {
      _logger.info("activating http client factory: pending clients=" + _pendingClients.size());
    }
    HttpClientImpl client;
    while ((client = _pendingClients.poll()) != null) {
      client.setRandomConnection(); // choose one random mux cnx
      client.resume();
    }
    _pendingClients.clear();
  }
  
  //Called when an http client is used before the http agent is initialized and before we have any mux cnx
  // (current thread = ANY threads).
  public boolean isActive(final HttpClientImpl client) {
    synchronized (this) {
      if (!_isActive) {
        if (_logger.isInfoEnabled()) {
          _logger.info("Delaying http client (" + client.getURL() + "): http agent not yet initialized");
        }
        _pendingClients.add(client);
        return false;
      }
    }
    return true;
  }
  
  @Override
  public HttpClient create(String url) throws MalformedURLException {
    return new HttpClientImpl(this, url);
  }
  
  @Override
  public HttpClient create(String url, HttpClient client) throws MalformedURLException {
    return new HttpClientImpl(this, url, client);
  }
  
  @Override
  public HttpClient create(String url, String callerId) throws MalformedURLException {
    return new HttpClientImpl(this, url, callerId);
  }
  
  Utils getUtils() {
    return _utils;
  }
}
