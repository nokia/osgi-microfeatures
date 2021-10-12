// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.demux.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import alcatel.tess.hometop.gateways.utils.ByteInputStream;

import com.nextenso.http.agent.Utils;
import com.nextenso.http.agent.client.HttpSocket;
import com.nextenso.http.agent.client.HttpSocketHandler;
import com.nextenso.http.agent.demux.DemuxClientSocket;
import com.nextenso.http.agent.demux.SocketUser;
import com.nextenso.http.agent.parser.HttpParser;

public class HttpClientSocketUser implements HttpSocket, SocketUser {

  private enum STATUS {
    OPENING,
    OPENED,
    DATA_SENT,
    CLOSED,
    HANDLED,
  }
  
  private HttpConnectionDemux connection;
  private ByteInputStream input;
  private HttpClientOutputStream output;
  private HttpParser parser;
  private volatile DemuxClientSocket socket;
  private HttpSocketHandler handler;
  private final ReentrantLock lock = new ReentrantLock();
  private volatile STATUS status;
  private LinkedList<ByteBuffer> queue;
 
  public HttpClientSocketUser(HttpConnectionDemux connection, HttpSocketHandler handler) {
    this.input = new ByteInputStream();
    this.output = new HttpClientOutputStream();
    this.queue = new LinkedList<ByteBuffer>();
    this.parser = new HttpParser();
    this.connection = connection;
    this.handler = handler;
    status = STATUS.OPENING;
  }
  
  public void setSocket(DemuxClientSocket socket) {    
    this.socket = socket;
    if (socket.getSockId() != 0 && (status == STATUS.OPENING)) {
      status = STATUS.OPENED;
    }
  }
  
  //--- SocketUser
  
  @Override
  public void dataReceived(byte[] data, int off, int len) {
    if (Utils.logger.isDebugEnabled()) Utils.logger.debug(HttpClientSocketUser.this + " dataReceived");
    input.init(data, off, len, false);
    if (!handler.handleHttpSocket(this)) {
      connection.releaseSocket(socket);
    }
  }

  @Override
  public void connected(int sockId) {
    lock.lock();
    try {
      status = STATUS.OPENED;
      ByteBuffer data = queue.poll();
      while(data != null) {
        status = STATUS.DATA_SENT;
        if (connection.sendData(socket, data)) {
          data = queue.poll();          
        }
        else {
          error(2);
          data = null;
        }
      }
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public void closed(DemuxClientSocket socket, boolean aborted) {
    if (aborted) { 
      if (Utils.logger.isInfoEnabled()) Utils.logger.info(this+".clientSocketAborted: retry");
      if (connection.retry(handler)) {
        status = STATUS.HANDLED;
        return;
      }
    }
    error(0);
    connection.removeSocket(socket);
  }

  @Override
  public void timeout(DemuxClientSocket socket) {
    error(1);
    connection.closeSocket(socket);
  }

  @Override
  public void error(int errno) {
    lock.lock();
    try {
      if (Utils.logger.isDebugEnabled()) Utils.logger.debug(this+" errno="+errno);
      boolean notify = false;
      STATUS old = status;
      status = STATUS.CLOSED;
      switch (old) {
      case OPENING:
      case OPENED:
        if (!queue.isEmpty()) {
          notify = true;
        }
        break;
        
      case DATA_SENT:
        notify = true;
        break;

      default:
        break;
      }

      if (notify) {
        input.close();
        queue.clear();
        handler.handleHttpSocket(this);
        status = STATUS.HANDLED;
      }
    }
    finally {
      lock.unlock();
    }
  }
  
  @Override
  public boolean isTunneling() {
    return false;
  }

  //--- HttpSocket

  @Override
  public OutputStream getOutputStream() {
    return output;
  }

  @Override
  public InputStream getInputStream() {
    return input;
  }

  @Override
  public int getSocketId() {
    return socket.getSockId();
  }

  @Override
  public long getSessionId() {
    return -1L;
  }

  @Override
  public HttpParser getHttpParser() {
    return parser;
  }
    
  @Override
  public String toString() {
    return "HttpClientSocketUser [status=" + status + ", " + socket + "]";
  }

  private final class HttpClientOutputStream extends OutputStream {

    @Override
    public void write(int b) throws IOException {
      write(new byte[] { (byte) b }, 0, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
      write(b, 0, b.length);
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      if (Utils.logger.isDebugEnabled()) Utils.logger.debug(HttpClientSocketUser.this + " write");
      lock.lock();
      try {
        switch (status) {
        case HANDLED:
          break;
          
        case CLOSED:
          status = STATUS.HANDLED;
          throw new IOException(STATUS.CLOSED.toString());
          
        case OPENING:
          queue.offer(ByteBuffer.wrap(b, off, len));
          break;
          
        case OPENED:
        case DATA_SENT:
          if (!connection.sendData(socket, ByteBuffer.wrap(b, off, len))) {
            status = STATUS.HANDLED;
            throw new IOException("MUX CONNECTION CLOSED");
          }
          status = STATUS.DATA_SENT;
          break;

        default:
          status = STATUS.HANDLED;
          throw new IOException("UNKNOWN SOCKET STATUS");
        }
      }
      finally {
        lock.unlock();
      }
    }

  }
  
}
