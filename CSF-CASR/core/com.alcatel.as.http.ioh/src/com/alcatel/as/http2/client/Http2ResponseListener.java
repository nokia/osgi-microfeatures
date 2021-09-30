package com.alcatel.as.http2.client;

import org.osgi.annotation.versioning.ProviderType;
import java.nio.ByteBuffer;

public interface Http2ResponseListener {
    
    public default void recvRespStatus (Http2Request req, int status){}

    public default void recvRespHeader (Http2Request req, String name, String value){}

    public default void recvRespHeaders (Http2Request req, boolean done){}

    public default void recvRespData (Http2Request req, ByteBuffer data, boolean done){}

    public default void recvRespTrailer (Http2Request req, String name, String value){}
    
    public default void endResponse (Http2Request req){}
    
    public default void abortRequest (Http2Request req){};

}

