// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.agent.web.http.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel_lucent.as.agent.web.container.Container;
import com.alcatel_lucent.as.agent.web.muxhandler.WebAgentSocketInterface;
import com.alcatel_lucent.as.service.jetty.common.connector.AbstractBufferEndPoint;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.socket.Socket;

public class HttpClientSocket implements Socket, WebAgentSocketInterface {

  final static Logger LOGGER = Logger.getLogger("agent.web.http.client.socket");

  private int id;
  private MuxConnection mux;
  private EndPoint endPoint;
  private PlatformExecutor ioExecutor;

  public class EndPoint extends AbstractBufferEndPoint {
    private InetSocketAddress remote;
    public EndPoint(InetSocketAddress remote, boolean secure) {
      super(remote, secure, ioExecutor);
      this.remote = remote;
    }

    @Override
    public boolean flush(ByteBuffer... buffers) throws IOException {
      return mux.sendTcpSocketData(id, true, buffers);
    }

    public void serverData(byte[] data, int off, int len) {
      try {
        byte[] copy = new byte[len];
        System.arraycopy(data, off, copy, 0, len);
        input.write(copy, 0, len);
        input.flush();
      } catch (IOException e) {
        if (LOGGER.isDebugEnabled())
          LOGGER.debug("cannot handle server data", e);
      }
    }

    @Override
    public void close(boolean advertise) {
      super.close(false);
    }

    @Override
    public void prepareUpgrade() {
    } // unused

    @Override
    public InetSocketAddress getLocalAddress() {
      return mux.getLocalAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
      return remote;
    }

  }

  public HttpClientSocket(MuxConnection mux, int id, PlatformExecutor ioExecutor) {
    this.mux = mux;
    this.id = id;
    this.ioExecutor = ioExecutor;
  }

  EndPoint createEndPoint(InetSocketAddress remote, boolean secure) {
    endPoint = new EndPoint(remote, secure);
    return endPoint;
  }

  /*-- WebAgentSocketInterface ----------------------*/

  @Override
  public void received(byte[] data, int off, int len) {
    endPoint.serverData(data, off, len);
  }

  @Override
  public void closed(Container container) {
    endPoint.getConnection().close();
  }

  /*-- MUX Socket ----------------------*/

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
    return id;
  }

  @Override
  public int getType() {
    return Socket.TYPE_TCP;
  }

  @Override
  public String toString() {
    return "HttpClientSocket [id=" + id + "]";
  }

}
