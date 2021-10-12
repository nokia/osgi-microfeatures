// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.client.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import com.nextenso.http.agent.Agent;
import com.nextenso.http.agent.Utils;
import com.nextenso.http.agent.client.HttpConnection;
import com.nextenso.http.agent.client.HttpSocket;
import com.nextenso.http.agent.client.HttpSocketHandler;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxHeaderV0;

/**
 * This class Manages async http stack sockets.
 */
public class HttpConnectionImpl extends HttpConnection {
  
  private static int seed = 0;
  private final static Logger _logger = Logger.getLogger("agent.http.client." + HttpConnectionImpl.class.getSimpleName());
  
  /**
   * Class constructor.
   * @param agent 
   */
  public HttpConnectionImpl(MuxConnection muxCnx, Agent agent) {
    this.sockets = new Hashtable<Integer, HttpSocketImpl>();
    this.muxCnx = muxCnx;
    this.sender = agent;
  }
  
  @Override
  public String toString() {
    return muxCnx != null ? muxCnx.getStackInstance() : "??" + ")";
  }
  
  /************************* Mux callbacks ****************************/
  
  @Override
  public void disconnected() {
    ArrayList<HttpSocketImpl> list = new ArrayList<HttpSocketImpl>();
    
    synchronized (this) {
      // Abort any pending requests.
      
      Enumeration<HttpSocketImpl> e = sockets.elements();
      while (e.hasMoreElements()) {
        HttpSocketImpl s = e.nextElement();
        list.add(s);
      }
      
      sockets.clear();
    }
    
    for (int i = 0; i < list.size(); i++) {
      HttpSocketImpl s = list.get(i);
      s.disconnected();
    }
  }
  
  public void dataReceived(MuxHeaderV0 header, byte[] buf, int off, int len) {
    HttpSocketImpl s = null;
    int channelId = header.getChannelId();
    
    synchronized (this) {
      s = sockets.get(channelId);
    }
    
    if (s != null) {
      if ((header.getFlags() & Utils.NO_FILTER_MASK) != Utils.CLOSED) {
    	if (_logger.isDebugEnabled()) _logger.debug("data received");
        // Proceed with http response data: lookup the socket and callback its handler.
        s.dataReceived(header, buf, off, len);
      } else {
        // Its either a close or close ack (close ack occurs if socket was previously aborted).
    	if (_logger.isDebugEnabled()) _logger.debug("close received");
      	s.closeReceived();
      }
    } else {
    	if (_logger.isDebugEnabled()) _logger.debug("ignoring data (no client socket found for sockid " + channelId + ")");
    }
  }
  
  /**************************************************************************/
    
  public HttpSocket open(HttpSocketHandler handler) {
    long id = 0;
    synchronized (HttpConnectionImpl.class) {
      if (seed < 0)
        seed = 1;
      else
        ++seed;
      id = seed & 0xFFFFFFFF;
    }
    return open(handler, id);
  }
  
  public HttpSocket open(HttpSocketHandler handler, long sessionId) {
    HttpSocketImpl s = new HttpSocketImpl(this, handler);
    handler.getRequest().setProxyMode(true);
    // Generate a socket id and register the socket.    
    MuxHeaderV0 hdr = s.getMuxHeader();
    
    int id = 0;
    synchronized (HttpConnectionImpl.class) {
      if (seed < 0)
        seed = 0;
      id = ++seed;
    }
    synchronized (this) {
      hdr.set(sessionId, id, Utils.CLIENT_FLAGS | Utils.DATA);
      sockets.put(id, s);
    }
    
    return (s);
  }
  
  @Override
  public void close(HttpSocket s) {
    close(s.getSocketId());
  }
  
  @Override
  public Agent getAgent() {
	  return sender;
  }
  
  @Override
  public MuxConnection getMuxCnx() {
	  return muxCnx;
  }
  
  protected void close(int httpSocketId) {
    synchronized (this) {
      sockets.remove(httpSocketId);
    }
    if (_logger.isDebugEnabled()) {
      _logger.debug("Removed socket " + httpSocketId + " (remaining=" + sockets.size() + ")");
    }
  }
  
  protected boolean sendData(MuxHeaderV0 hdr, byte[] buf, int off, int len) {
    // no need to duplicate the http client data
    return sender.sendMuxData(muxCnx, hdr, false, ByteBuffer.wrap(buf, off, len));
  }
  
  /** Sockets waiting for a response */
  private Hashtable<Integer, HttpSocketImpl> sockets;
  
  /** mux connection used */
  private MuxConnection muxCnx;
  /** message sender */
  private Agent sender;
  
}
