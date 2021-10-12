// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.demux;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.nextenso.http.agent.Utils;
import com.nextenso.http.agent.parser.HttpHeaderDescriptor;
import com.nextenso.http.agent.parser.HttpParser;
import com.nextenso.http.agent.parser.HttpParserException;
import com.nextenso.http.agent.parser.HttpResponseHandler;
import com.nextenso.proxylet.http.HttpCookie;


public class ProxySocketUser implements HttpResponseHandler, SocketUser {

  private volatile List<ByteBuffer> data;
  private volatile HttpPipeline pipeline;
  private volatile String requestMethod;
  private HttpParser responseParser;

  public ProxySocketUser(HttpPipeline pipeline, String requestMethod) {
    this.responseParser = new HttpParser();
    this.pipeline = pipeline;
    this.requestMethod = requestMethod;
    this.data = new ArrayList<>();
  }
  
  public void putData(ByteBuffer... buffers) {
    for(ByteBuffer b : buffers) {
      data.add(b);
    }
  }

  @Override
  public void connected(int sockId) {
    pipeline.getLock().lock();
    try {
      pipeline.sendProxyRequestData(sockId, data.toArray(new ByteBuffer[0])); // TODO false = error while writing data
      data.clear();
    } finally {
      pipeline.getLock().unlock();
    }
  }

  @Override
  public void closed(DemuxClientSocket socket, boolean aborted) {
    if (aborted)
      pipeline.proxySocketAborted(socket);   
    else
      pipeline.proxySocketClosed(socket, responseParser.getState()==0);   
  }

  @Override
  public void error(int errno) {
    pipeline.proxySocketError(errno);
  }

  @Override
  public void timeout(DemuxClientSocket socket) {
    pipeline.proxySocketTimeout(socket);
  }
  
  @Override
  public void dataReceived(byte[] data, int off, int len) {
    boolean last = false;
    int oldState = 0;
    try {
      oldState = responseParser.getState();
      if (responseParser.parseResponse(requestMethod, new ByteArrayInputStream(data, off, len), this)==HttpParser.PARSED)
        last = true;
      pipeline.proxyResponseData(data, off, len, last);
    }
    catch (HttpParserException e) {
      if (Utils.logger.isDebugEnabled()) Utils.logger.debug("socketData catches parser error " + e.getMessage(), e);
      if (oldState == 0) {
        pipeline.proxySocketError(DemuxSocket.INTERNAL_SERVER_ERROR, "Parsing Error");
      }
      else {
        pipeline.proxyResponseData(data, off, len, last);
      }
    }
  }
  
  @Override
  public boolean isTunneling() {
    return false;
  }

  //---------- HttpResponseHandler

  @Override
  public void setHttpProtocol(String protocol) {}

  @Override
  public void addHttpCookie(HttpCookie cookie) {}

  @Override
  public void addHttpHeader(String name, String val) {}

  @Override
  public void addHttpHeader(HttpHeaderDescriptor hdrDesc, String val) {}

  @Override
  public void addHttpBody(InputStream in, int size) throws IOException { }

  @Override
  public void setHttpResponseStatus(int status) {}

  @Override
  public void setHttpResponseReason(String reason) {}

}
