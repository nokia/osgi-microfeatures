package com.nextenso.http.agent.parser;


/**
 * Interface used to notify about http response parsing.
 */
public interface HttpResponseHandler extends HttpHandler {
  // Response prolog
  public void setHttpResponseStatus(int status);
  
  public void setHttpResponseReason(String reason);
}
