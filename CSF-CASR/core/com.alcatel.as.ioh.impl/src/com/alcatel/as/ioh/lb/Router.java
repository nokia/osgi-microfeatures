package com.alcatel.as.ioh.lb;

import com.alcatel.as.ioh.client.TcpClient;

public interface Router {

    public default void init (Client client){}
    public default void init (Client client, TcpClient.Destination server){}

    // indicates the number of bytes needed for routing
    // return -1 : all message
    // return 0 : no byte needed (quite equivalent to return 1)
    // return N : at least N bytes
    public int neededBuffer ();

    public default boolean needServerData (){ return false;}

    // for TCP
    public void route (Client client, Chunk chunk);
    public default void route (Client client, TcpClient.Destination server, Chunk chunk){
	client.sendToClient (server, chunk);
    }

    // for UDP
    public void route (UdpClientContext client, Chunk chunk);
}
