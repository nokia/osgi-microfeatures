package com.alcatel.as.ioh.lb.mux;

import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;

import com.alcatel.as.ioh.lb.Chunk;

public interface IOHRouter {

    public default void init (IOHClient client){}
    public default void init (IOHClient client, MuxClient agent){}

    // indicates the number of bytes needed for routing
    // return -1 : all message
    // return 0 : no byte needed (quite equivalent to return 1)
    // return N : at least N bytes
    public int neededBuffer ();

    public default boolean needServerData (){ return false;}

    // for TCP
    // chunk from client
    public void route (IOHClient client, Chunk chunk);
    // chunk from server
    public default void route (IOHClient client, MuxClient agent, Chunk chunk){
	client.sendToClient (agent, chunk);
    }

    // called when a chunk followup cannot be forwarded (agent gone)
    // or when the new chunk cannot be sent (agent overload)
    public default void sendToDestinationFailed (IOHClient client, MuxClient agent, Chunk chunk){ client.sendToDestination (null, chunk, false);}
    
}
