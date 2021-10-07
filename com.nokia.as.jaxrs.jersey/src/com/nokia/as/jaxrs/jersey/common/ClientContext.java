package com.nokia.as.jaxrs.jersey.common;

import java.nio.ByteBuffer;

public interface ClientContext {

    public ServerContext getServerContext ();
	
    public void send (ByteBuffer data, boolean copy);
    
    public void close();

    public void setSuspendTimeout (long timeOut);
    
}
