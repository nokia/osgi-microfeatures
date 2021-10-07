package com.nextenso.http.agent.client;

import com.nextenso.http.agent.impl.HttpRequestFacade;

/**
 * Callback interface used to send HTTP response events.
 */
public interface HttpSocketHandler {
  /**
   * Handle data received on the handled socket.
   * @return false if the  HTTP socket should not be handled any more (in this
   *	     case it means that either the HTTP response has been fully parsed,
   *	     or an IOException occurred while parsing). Return true if the socket
   *	     must still be handled (the HTTP response has not been fully parsed).
   */
  public boolean handleHttpSocket(HttpSocket s);
  
  public HttpRequestFacade getRequest();
  
}
