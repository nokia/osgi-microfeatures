// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.demux;

public interface SocketUser {

  void dataReceived(byte[] data, int off, int len);

  void connected(int sockId);

  void closed(DemuxClientSocket socket, boolean aborted);

  void error(int errno);

  void timeout(DemuxClientSocket socket);

  boolean isTunneling();

}
