package com.nextenso.http.agent.demux;

import com.nextenso.mux.socket.Socket;

public interface DemuxSocket extends Socket {

  public final static int SWITCHING_PROTOCOL = 101;
  public final static int BAD_REQUEST = 400;
  public final static int FORBIDDEN = 403;
  public final static int NOT_FOUND = 404;
  public final static int INTERNAL_SERVER_ERROR = 500;
  public final static int BAD_GW = 502;

  public void socketData(byte[] data, int off, int len);
  
  public void socketClosed();
  
}
