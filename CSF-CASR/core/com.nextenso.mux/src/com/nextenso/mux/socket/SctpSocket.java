package com.nextenso.mux.socket;

public interface SctpSocket extends Socket
{
    long getConnectionId();

    String[] getLocalIPs();

    String[] getRemoteIPs();

    int getRemotePort();

    boolean isSecure();

    boolean isClientSocket();

    int maxOutStreams();

    int maxInStreams();
}
