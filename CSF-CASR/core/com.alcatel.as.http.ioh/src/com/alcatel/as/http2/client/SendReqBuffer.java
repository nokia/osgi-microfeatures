package com.alcatel.as.http2.client;

import java.nio.ByteBuffer;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface SendReqBuffer {
    
    // throws an IndexOutOfBoundsException if the maxSize is exceeded
    // returns true if all was sent
    public boolean send (ByteBuffer data, boolean copy, boolean done);

    public int size ();
    public int maxSize ();
    // clears the buffers to help GC in case of abort for ex
    public void clear ();    
}
