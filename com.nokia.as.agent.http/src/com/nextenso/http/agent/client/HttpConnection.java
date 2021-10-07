package com.nextenso.http.agent.client;

import com.nextenso.http.agent.Agent;
import com.nextenso.http.agent.Utils;
import com.nextenso.mux.MuxConnection;

/**
 * This class manages HttpSocket objects which are used to perform http requests.
 */
public abstract class HttpConnection {
  
  /**
   * Get an available http stack connection.
   *
   * @return an available http stack connection, or null if no connection are
   * currently available.
   */
  public static HttpConnection getConnection(Utils utils) {
    return utils.getHttpConnection();
  }
  
  /**
   * Acquire an http stack socket.
   *
   * @param handler the callback object which is used to notify about
   *		    http response events.
   */
  public abstract HttpSocket open(HttpSocketHandler handler, long sessionId);
  
  public abstract HttpSocket open(HttpSocketHandler handler);

  public abstract void close(HttpSocket s);
  
  public abstract void disconnected();
  
  public abstract Agent getAgent();
  
  public abstract MuxConnection getMuxCnx();
}
