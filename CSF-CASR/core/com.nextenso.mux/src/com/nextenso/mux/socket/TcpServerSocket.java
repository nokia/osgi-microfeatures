package com.nextenso.mux.socket;

public interface TcpServerSocket extends Socket
{

    public long getListenId();

    public boolean isSecure();
}
