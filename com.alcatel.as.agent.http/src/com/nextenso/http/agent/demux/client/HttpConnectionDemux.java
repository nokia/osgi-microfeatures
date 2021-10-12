// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.demux.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;

import com.alcatel_lucent.as.service.dns.DNSHelper;
import com.alcatel_lucent.as.service.dns.RecordAddress;
import com.nextenso.http.agent.Agent;
import com.nextenso.http.agent.HttpCnxMeters;
import com.nextenso.http.agent.Utils;
import com.nextenso.http.agent.client.HttpConnection;
import com.nextenso.http.agent.client.HttpSocket;
import com.nextenso.http.agent.client.HttpSocketHandler;
import com.nextenso.http.agent.demux.ConnectionPool;
import com.nextenso.http.agent.demux.DemuxClientSocket;
import com.nextenso.http.agent.impl.HttpRequestFacade;
import com.nextenso.http.agent.parser.HttpParser;
import com.nextenso.mux.MuxConnection;

public class HttpConnectionDemux extends HttpConnection {

  private final MuxConnection mux;
  private final ConnectionPool pool;
  private final Agent agent;
  private final HttpCnxMeters meters;
  
  private enum ERROR_CAUSE {
    CONNECTION_REFUSED,
    SELF_CONNECTION_PROHIBITED, 
    HOSTNAME_CANNOT_BE_RESOLVED,
    PROXY_CANNOT_BE_RESOLVED
  }
  
  public HttpConnectionDemux(MuxConnection connection, ConnectionPool pool, Agent agent, HttpCnxMeters meters) {
    this.mux = connection;
    this.pool = pool;
    this.agent = agent;
    this.meters = meters;
  }

  @Override
  public HttpSocket open(HttpSocketHandler handler, long sessionId) {
    return open(handler, sessionId, false);
  }

  private HttpSocket open(HttpSocketHandler handler, long sessionId, boolean retry) {
    if (Utils.logger.isDebugEnabled()) Utils.logger.debug(this + " open");
    HttpRequestFacade request = handler.getRequest();
    
    String host = request.getProlog().getURL().getHost();
    int port = request.getProlog().getURL().getPort();
    String nextProxy = pool.getNextProxy(host);
	request.setProxyMode((nextProxy != null ? true : false));
    if (mux.isOpened()) {
      InetSocketAddress destination = new InetSocketAddress(host, port);
      if (pool.isSelfConnection(destination)) {
        return new SocketError(ERROR_CAUSE.SELF_CONNECTION_PROHIBITED); 
      }
      String to = host;
      if (pool.isHostName(host)) {
        List<RecordAddress> list = DNSHelper.getHostByName(host);
        if (list.isEmpty()) {
          return new SocketError(ERROR_CAUSE.HOSTNAME_CANNOT_BE_RESOLVED);
        }
        to = list.get(0).getAddress();
      }
            
      if (request.getProxyMode()) {
    	  String[] parts = nextProxy.split(":");
    	  if (parts.length < 2) return new SocketError(ERROR_CAUSE.PROXY_CANNOT_BE_RESOLVED);
    	  to = parts[0];
    	  port = Integer.valueOf(parts[1]);
      }
      
      HttpClientSocketUser user = new HttpClientSocketUser(this, handler);
      DemuxClientSocket socket = pool.getHttpClientSocket(mux, host, to, port, request, sessionId, user, retry);
      if (socket != null) {
        user.setSocket(socket);
        return user;
      }
    }
    return new SocketError(ERROR_CAUSE.CONNECTION_REFUSED); 
  }
  
  public HttpCnxMeters getMeters() {
	  return meters;
  }
  
  @Override
  public HttpSocket open(HttpSocketHandler handler) {
    return open(handler, -1);
  }

  @Override
  public void close(HttpSocket s) {
    // Exception while writing the request: Nothing to do
  }

  @Override
  public void disconnected() {
  }
  
  void closeSocket(DemuxClientSocket socket) {
    mux.sendTcpSocketClose(socket.getSockId());
  }
  
  void removeSocket(DemuxClientSocket socket) {
    pool.clientSocketClosed(mux, 0, socket);
  }
  
  void releaseSocket(DemuxClientSocket socket) {
    pool.releaseClientSocket(mux, 0, socket);
  }
  
  boolean sendData(DemuxClientSocket socket, ByteBuffer data) {
    socket.access();
    return mux.sendTcpSocketData(socket.getSockId(), false, data);
  }
  
  boolean retry(HttpSocketHandler handler) {
    HttpSocket socket = open(handler, -1, true);
    if (socket instanceof SocketError) {
      return false;
    }
    else {
      try {
        handler.getRequest().writeTo(socket.getOutputStream(), handler.getRequest().getProxyMode());
        return true;      
      }
      catch (IOException e) {
        return false;
      }      
    }
  }
  
  @Override
  public String toString() {
    return "HttpConnectionDemux [mux=" + mux + "]";
  }


  private static class SocketError extends OutputStream implements HttpSocket {

    private ERROR_CAUSE cause;
    
    public SocketError(ERROR_CAUSE cause) {
      this.cause = cause;
    }

    @Override
    public OutputStream getOutputStream() {
      return this;
    }

    @Override
    public InputStream getInputStream() {
      return null;
    }

    @Override
    public int getSocketId() {
      return 0;
    }

    @Override
    public long getSessionId() {
      return 0;
    }

    @Override
    public HttpParser getHttpParser() {
      return null;
    }

    @Override
    public void write(int b) throws IOException {
      flush();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      flush();
    }

    @Override
    public void write(byte[] b) throws IOException {
      flush();
    }
    
    @Override
    public void flush() throws IOException {
      throw new IOException(cause.toString());
    }

  }

	@Override
	public Agent getAgent() {
		return agent;
	}

	@Override
	public MuxConnection getMuxCnx() {
		return mux;
	}

}
