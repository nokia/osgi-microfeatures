package com.alcatel.as.http2;

import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import java.util.concurrent.Executor;
import org.osgi.annotation.versioning.ProviderType;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;

public interface Http2RequestListener {
    
    public default void newRequest (RequestContext cb){}

    public default void recvReqMethod (RequestContext cb, String method){}

    public default void recvReqPath (RequestContext cb, String path){}

    public default void recvReqScheme (RequestContext cb, String scheme){}

    public default void recvReqAuthority (RequestContext cb, String auth){}

    public default void recvReqHeader (RequestContext cb, String name, String value){}

    public default void recvReqHeaders (RequestContext cb, boolean done){}

    public default void recvReqData (RequestContext cb, ByteBuffer data, boolean done){}

    public default void recvReqTrailer (RequestContext cb, String name, String value){}
    
    public default void endRequest (RequestContext cb){}

    public default void abortRequest (RequestContext cb){};

    @ProviderType
    public interface RequestContext {

	public Logger logger ();
	
	public TcpChannel channel ();

	public int id ();

	public long channelId ();
	
	public <T> T attachment ();

	public void attach (Object o);

	public Executor requestExecutor ();
	
	public Executor responseExecutor ();
	
	public boolean isClosed ();

	public int sendWindow ();

	public void setRespStatus (int status);

	public void setRespHeader (String name, String value);

	public void sendRespHeaders (boolean done);
	
	public void sendRespData (ByteBuffer data, boolean copy, boolean done);

	public void abortStream (Http2Error.Code code, String msg);
	
	public void abortConnection (Http2Error.Code code, String msg);

	public void onWriteAvailable (Runnable success, Runnable failure, long delay);

	public SendBuffer newSendRespBuffer (int maxBufferSize);
    }
}
