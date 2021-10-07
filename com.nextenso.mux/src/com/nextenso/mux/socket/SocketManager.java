package com.nextenso.mux.socket;

import java.util.Enumeration;

/**
 * This class is used to manage the Sockets linked to a MuxConnection.
 * <p/>It is meant to keep track of the Sockets upon reception of an open or close notice.
 * <br/>Each MuxConnection has an instance of SocketManager.
 * <br/>The types of a Socket are defined in interface Socket.
 * <p/><b>This class is NOT synchronized.</b>
 */
public interface SocketManager
{

    public Socket getSocket(int type, int id);

    public void addSocket(Socket socket);

    public Socket removeSocket(int type, int id);

    public int getSocketsSize(int type);

    public int getSocketsSize();

    public Enumeration getSockets(int type);

    public Enumeration getSockets();

    public void removeSockets(int type);

    public void removeSockets();
}
