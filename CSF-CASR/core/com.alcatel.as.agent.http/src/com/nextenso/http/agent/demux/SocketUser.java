package com.nextenso.http.agent.demux;

public interface SocketUser {

  void dataReceived(byte[] data, int off, int len);

  void connected(int sockId);

  void closed(DemuxClientSocket socket, boolean aborted);

  void error(int errno);

  void timeout(DemuxClientSocket socket);

  boolean isTunneling();

}
