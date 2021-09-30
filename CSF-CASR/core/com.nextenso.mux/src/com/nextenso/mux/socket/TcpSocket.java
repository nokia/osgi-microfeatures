package com.nextenso.mux.socket;

public interface TcpSocket extends Socket
{

    public long getConnectionId();

    public int getRemoteIP();

    public int getRemotePort();

    public int getVirtualIP();

    public String getVirtualIPString();

    public int getVirtualPort();

    public boolean isSecure();

    public boolean isClientSocket();

}
