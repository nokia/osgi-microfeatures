package com.alcatel.as.http2.client;

import org.osgi.annotation.versioning.ProviderType;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;


@ProviderType
public interface Http2Connection {

    public static enum Status {
	AVAILABLE (true, true),
	UNAVAILABLE_MAX_CONCURRENT (false, true),
	UNAVAILABLE_FULL_WINDOW (false, true),
	UNAVAILABLE_EXHAUSTED (false, false),
	UNAVAILABLE_NOT_CONNECTED (false, false);

	private boolean _available, _retriable;
	private Status (boolean available, boolean retriable){
	    _available = available;
	    _retriable = retriable;
	}
	public boolean available (){ return _available;}
	public boolean retriable (){ return _retriable;}
    }

    public <T> T attachment ();

    public void attach (Object o);

    public java.net.InetSocketAddress remoteAddress ();

    public java.net.InetSocketAddress proxyAddress ();

    public java.net.InetSocketAddress localAddress ();

    public Map<String, Object> exportTlsKey ();

    // must be called in writeExecutor
    public Http2Request newRequest (Http2ResponseListener listener);
    // must be called in writeExecutor
    public Http2Connection onAvailable (Runnable onSuccess, Runnable onFailure, long delay);

    // must be called in writeExecutor
    // weight between 1 and 256 (inclusive)
    public void sendPriority (int streamId, boolean exclusive, int streamDepId, int weight);

    // must be called in writeExecutor
    // delay :
    // 0 : now ! (idleTimeout is ignored)
    // -1 : wait until all reqs are done, possibly forever
    // N : max N millis to wait for all reqs to be done, then close
    // if idleTimeout>0 it is set and may generate a close of its own
    public void close (int code, String msg, long delay, long idleTimeout);
    // old method : idleTimeout set to 0
    public void close (int code, String msg, long delay);
    
    public void clone (java.util.function.Consumer<Http2Connection> onSuccess,
		       Runnable onFailure,
		       Runnable onClose);

    public Executor writeExecutor ();
	
    public Executor readExecutor ();

    // must be called in writeExecutor
    public Status status ();

    // must be called in writeExecutor - returns the nb of requests that can still be created
    public int remainingRequests ();
}

