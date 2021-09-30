package com.nextenso.mux.socket;

public interface SctpServerSocket extends Socket
{
    public long getListenId();
    public boolean isSecure();
    public String[] getLocalIPs();
}
