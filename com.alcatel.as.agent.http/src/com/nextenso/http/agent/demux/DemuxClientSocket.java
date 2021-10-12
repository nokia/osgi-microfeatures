// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.demux;

import java.time.Duration;
import java.util.concurrent.Future;

import com.nextenso.http.agent.Utils;
import com.nextenso.mux.socket.Socket;

public class DemuxClientSocket implements DemuxSocket
{

  private volatile int sockId;
  private String destination;
  private ConnectionPool connectionPool;
  private long lastAccessTime;
  private Future<?> timeoutTask;
  private volatile SocketUser user;
  private volatile boolean reused;
  private volatile boolean ready = false;
  
  private long customTimeout = 0;

  public DemuxClientSocket(String destination, ConnectionPool pool) {
    this.destination = destination;
    this.connectionPool = pool;
  }

  public void setUser(SocketUser user, boolean reused) {
    this.user = user;
    this.reused = reused;
  }
  
  public void setTimeout(long timeout) {
	  this.customTimeout = timeout;
      if (customTimeout > 0) {
    	  timeoutTask = connectionPool.scheduleTimeout(new SocketTimeout(), customTimeout + 10);      
      }
  }
  
  /**
   * 
   * @param timeout Duration, long or null is supported.
   */
  public void setTimeout(Object timeout) {
	  if(timeout == null) {
		  customTimeout = 0;
	  } else if(timeout instanceof Long) {
		  long to = (Long) timeout;
		  setTimeout(to);
	  } else if(timeout instanceof Duration){
		  setTimeout(((Duration)timeout).toMillis());
	  }
  }
  
  public String getDestination() {
    return destination;
  }

  @Override
  public void socketData(byte[] data, int off, int len) {
    access();
    if (user != null) user.dataReceived(data, off, len);
  }
  
  public void socketConnected(int sockId) {
    this.sockId = sockId;
    access();
    if (user != null) user.connected(sockId);
    ready = true;
  }

  @Override
  public void socketClosed() {
    if (timeoutTask != null) timeoutTask.cancel(false);
    boolean aborted = reused & ((lastAccessTime+50)>System.currentTimeMillis());
    if (user != null) user.closed(this, aborted);
    ready = false;
  }
  
  public void socketError(int errno) {
    if (timeoutTask != null) timeoutTask.cancel(false);
    if (user != null) user.error(errno);
    ready = false;
  }

  @Override
  public boolean close() {
    return true;
  }

  @Override
  public int getLocalIP() {
    return 0;
  }

  @Override
  public String getLocalIPString() {
    return "";
  }

  @Override
  public int getLocalPort() {
    return 0;
  }

  @Override
  public int getSockId() {
    return sockId;
  }

  @Override
  public int getType() {
    return Socket.TYPE_TCP;
  }
  
  public void access() {
    this.lastAccessTime = System.currentTimeMillis();
  }
  public boolean isTunneling() {
    if (user != null) return user.isTunneling();
    return false;
  }
  
  public SocketUser getUser() {
    return user;
  }
  
  public boolean isReady() {
    return ready;
  }

  @Override
  public String toString() {
    return "DemuxClientSocket [sockId=" + sockId + ", destination=" + destination + "]";
  }

  /*****************************************
   * Inner class that checks Socket Timeout
   *****************************************/
  private class SocketTimeout implements Runnable {

    @Override
    public void run() {
      if (timeoutTask.isCancelled()) return;
      
      long elapsed = (System.currentTimeMillis() - lastAccessTime);
      long remaining = customTimeout - elapsed + 1000;
      if (elapsed > customTimeout || remaining <= 1000) {
        if (Utils.logger.isDebugEnabled()) Utils.logger.debug(DemuxClientSocket.this + ":timeout");
        if (user != null) user.timeout(DemuxClientSocket.this);
      }
      else {
        if (Utils.logger.isDebugEnabled()) Utils.logger.debug(DemuxClientSocket.this + ": Re-arming socket timer for "+ remaining + " sec");
        timeoutTask = connectionPool.scheduleTimeout(new SocketTimeout(), remaining);
      }
    }
    
  }

}
