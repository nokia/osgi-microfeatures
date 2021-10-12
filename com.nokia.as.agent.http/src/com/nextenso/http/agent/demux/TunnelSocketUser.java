// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.demux;



public class TunnelSocketUser implements SocketUser {

  private volatile HttpPipeline pipeline;

  public TunnelSocketUser(HttpPipeline pipeline) {
    this.pipeline = pipeline;
  }
  
  @Override
  public void connected(int sockId) {
    pipeline.tunnelSocketConnected();
  }

  @Override
  public void closed(DemuxClientSocket socket, boolean aborted) {
    pipeline.tunnelSocketClosed();
  }

  @Override
  public void error(int errno) {
    pipeline.tunnelSocketError(errno);
  }

  @Override
  public void timeout(DemuxClientSocket socket) {
  }
  
  @Override
  public void dataReceived(byte[] data, int off, int len) {
    pipeline.tunnelData(data, off, len);
  }
  
  @Override
  public boolean isTunneling() {
    return true;
  }

}
