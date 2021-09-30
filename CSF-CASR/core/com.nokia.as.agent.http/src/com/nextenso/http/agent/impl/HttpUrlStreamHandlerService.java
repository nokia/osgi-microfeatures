package com.nextenso.http.agent.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.osgi.service.url.AbstractURLStreamHandlerService;

import com.nextenso.proxylet.http.HttpClient;
import com.nextenso.proxylet.http.HttpClientFactory;

import alcatel.tess.hometop.gateways.utils.ByteInputStream;
import alcatel.tess.hometop.gateways.utils.QuotedStringTokenizer;
import alcatel.tess.hometop.gateways.utils.StringCaseHashtable;
import alcatel.tess.hometop.gateways.utils.Utils;

/**
 * OSGi URL Handlers Service. This class registers a URL stream handler for the "http/https"
 * protocol in order to map standard HttpUrlConnection with our Proxylet Http Client.
 */
public class HttpUrlStreamHandlerService extends AbstractURLStreamHandlerService {
  private final static Logger logger = Logger.getLogger("agent.http.HttpUrlStreamHandlerService");
  private static HttpClientFactory _hfc;
  
  // We can only use the first agent, because we are a singleton service.
  public void bindHttpClientFactory(HttpClientFactory hfc) {
    _hfc = hfc;
  }
  
  void start() { // not used, we don't remove it, to avoid having to bump bundle version (yes: this package is unfortunately exported)
      logger.debug("activate"); 
  }
  
  public URLConnection openConnection(URL u) throws IOException {
    if (logger.isDebugEnabled()) {
      logger.debug("openConnection(url=" + u);
    }
    return new HttpClientConnection(u, false);
  }
  
  public URLConnection openConnection(URL u, Proxy p) throws IOException {
    if (logger.isDebugEnabled()) {
      logger.debug("openConnection(url=" + u + ", p=" + p);
    }
    return openConnection(u);
  }
  
  /**
   * We implements our own URLConnection in order to redirect jdk Http URL Connection to the
   * http mux.
   */
  static class HttpClientConnection extends HttpURLConnection {
    private boolean _usingProxy;
    private StringCaseHashtable _requestHeaders = new StringCaseHashtable();
    private ByteArrayOutputStream _post;
    private HttpClient _client;
    private byte[] _response;
    
    protected HttpClientConnection(URL u, boolean usingProxy) {
      super(u);
      _usingProxy = usingProxy;
    }
    
    public void connect() throws IOException {
    }
    
    public synchronized void addRequestProperty(String key, String value) {
      String old = _requestHeaders.get(key);
      if (old != null) {
        StringBuilder sb = new StringBuilder(value);
        sb.append(',').append(old);
        value = sb.toString();
      }
      _requestHeaders.put(key, value);
    }
    
    public synchronized void setRequestProperty(String key, String value) {
      _requestHeaders.put(key, value);
    }
    
    @Override
    public synchronized String getHeaderField(String s) {
      try {
        getInputStream();    
        return (String) _client.getHeaders().get(Utils.capitalizeFirstLetter(s));
      } catch (IOException e) {
        logger.warn("HttpClient IOException for url " + url, e);
        return null;
      }
    }
    
    @Override
    public synchronized Map<String, List<String>> getHeaderFields() {
    	if (_client.getHeaders().isEmpty()) {
    		return Collections.emptyMap();
    	}
    	
    	Map<String, List<String>> map = new HashMap<>();
    	for (Object key : _client.getHeaders().keySet()) {
    		String k = key.toString();
    		String v = _client.getHeaders().get(key).toString();

    		List<String> list = new ArrayList<String>(1);
    		QuotedStringTokenizer tok = new QuotedStringTokenizer(v, ",", false);
    		while (tok.hasMoreElements()) {
    			list.add(tok.nextToken());
    		}
    		map.put(k, list);
    	}

    	return map;
    }
    
    public synchronized OutputStream getOutputStream() throws IOException {
      if (_post == null) {
        _post = new ByteArrayOutputStream();
      }
      return _post;
    }
    
    public String getResponseMessage() throws IOException {
      getInputStream();
      return _client.getReason();
    }
    
    @Override
    public void disconnect() {
    }
    
    @Override
    public boolean usingProxy() {
      return _usingProxy;
    }
    
    public synchronized InputStream getInputStream() throws IOException {
      if (_response != null) {
        return new ByteInputStream(_response);
      }
      
      if (logger.isDebugEnabled()) {
        logger.debug("HttpURLConnection wrapped to mux: " + url);
      }
      
      String urls = url.toString();
      _client =  _hfc.create(urls);
      Enumeration<?> e = _requestHeaders.keys();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        _client.setHeader(key, (String) _requestHeaders.get(key));
      }
      
      if (method.equals("GET")) {
        _client.doGET();
      } else if (method.equals("POST")) {
        String ctype = (String) _requestHeaders.get("Content-Type");
        byte[] body = (_post != null) ? _post.toByteArray() : null;
        _client.doPOST(ctype, body);
      } else if (method.equals("OPTIONS")) {
        _client.doOPTIONS();
      } else if (method.equals("PUT")) {
        String ctype = (String) _requestHeaders.get("Content-Type");
        byte[] body = (_post != null) ? _post.toByteArray() : null;
        _client.doPUT(ctype, body);
      } else if (method.equals("DELETE")) {
        _client.doDELETE();
      } else {
        throw new IllegalArgumentException("Method " + method + " not supported");
      }
      
      _response = _client.getResponse();
      return new ByteInputStream(_response);
    }
    
    public int getResponseCode() throws IOException {
      getInputStream();
      return _client.getStatus();
    }
  }
}
