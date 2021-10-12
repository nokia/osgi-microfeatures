// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.client.impl;

import static com.nextenso.http.agent.Utils.logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import alcatel.tess.hometop.gateways.utils.ByteInputStream;

import com.nextenso.http.agent.Utils;
import com.nextenso.http.agent.client.HttpSocket;
import com.nextenso.http.agent.client.HttpSocketHandler;
import com.nextenso.http.agent.parser.HttpParser;
import com.nextenso.mux.MuxHeaderV0;

public class HttpSocketImpl extends OutputStream implements HttpSocket {
  
  // ---------------- HttpSocket interface -------------------------------------
  
  public HttpSocketImpl(HttpConnectionImpl httpCnx, HttpSocketHandler handler) {
    this.hdr = new MuxHeaderV0();
    this.input = new ByteInputStream();
    this.parser = new HttpParser();
    this.httpCnx = httpCnx;
    this.handler = handler;
  }
  
  @Override
  public OutputStream getOutputStream() {
    return (this);
  }
  
  @Override
  public InputStream getInputStream() {
    return (this.input);
  }
  
  @Override
  public HttpParser getHttpParser() {
    return (this.parser);
  }
  
  public int getSocketId() {
    return (hdr.getChannelId());
  }
  
  @Override
  public long getSessionId() {
    return (hdr.getSessionId());
  }
  
  // ---------------- package methods ------------------------------------------
  
  MuxHeaderV0 getMuxHeader() {
    return (this.hdr);
  }
  
  void dataReceived(MuxHeaderV0 header, byte[] buf, int off, int len) {
    try {
      hdr.set(header.getSessionId(), header.getChannelId(), header.getFlags()); // ???
      input.init(buf, off, len, false);
      
      if (!handler.handleHttpSocket(this)) {
        httpCnx.close(hdr.getChannelId());
      }
    }
    
    catch (Throwable t) {
      logger.warn("Got exception when calling http handler", t);
      httpCnx.close(hdr.getChannelId());
    }
  }
  
  void closeReceived() {
    try {
      input.close();
      handler.handleHttpSocket(this);
    }
    
    catch (Throwable t) {
      logger.warn("Got exception when calling http handler", t);
    }
    
    finally {
      httpCnx.close(hdr.getChannelId());
    }
  }
  
  void disconnected() {
    closeReceived();
  }
  
  // ------------------------------------------------------------------------
  // OutputStream implementation.
  // ------------------------------------------------------------------------
  
  public void write(int b) throws IOException {
    write(new byte[] { (byte) b }, 0, 1);
  }
  
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }
  
  public void write(byte[] b, int off, int len) throws IOException {
    int flags = Utils.DATA | Utils.CLIENT_FLAGS;
    hdr.set(hdr.getSessionId(), hdr.getChannelId(), flags);
    if (!httpCnx.sendData(hdr, b, off, len)) {
      throw new IOException("no mux connection available");
    }
  }
  
  // ------------------------------------------------------------------------
  // Internal attributes.
  // ------------------------------------------------------------------------
  
  private HttpConnectionImpl httpCnx;
  private HttpSocketHandler handler;
  private MuxHeaderV0 hdr;
  private ByteInputStream input;
  private HttpParser parser;
}
