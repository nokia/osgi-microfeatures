package com.nextenso.mux.socket;

public interface UdpSocket extends Socket
{

    public long getBindId();

    public boolean isShared();
}
