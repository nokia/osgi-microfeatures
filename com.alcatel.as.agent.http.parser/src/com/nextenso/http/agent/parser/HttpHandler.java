package com.nextenso.http.agent.parser;

import java.io.IOException;
import java.io.InputStream;

import com.nextenso.proxylet.http.HttpCookie;

/**
 * Interface used to notify about http message parsing.
 */
public interface HttpHandler {
  // method common to request/response
  public void setHttpProtocol(String protocol);
  
  public void addHttpCookie(HttpCookie cookie);
  
  // Request headers
  public void addHttpHeader(String name, String val);
  
  public void addHttpHeader(HttpHeaderDescriptor hdrDesc, String val);
  
  // Request body
  public void addHttpBody(InputStream in, int size) throws IOException;
}
