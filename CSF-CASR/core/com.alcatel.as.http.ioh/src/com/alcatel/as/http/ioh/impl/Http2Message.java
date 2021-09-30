package com.alcatel.as.http.ioh.impl;

import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import java.util.concurrent.Executor;
import org.osgi.annotation.versioning.ProviderType;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import java.util.function.*;
import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClientState;

import com.alcatel.as.http.parser.*;
import com.alcatel.as.http2.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.http.ioh.HttpIOHRouter;

// do not finish the response data before endRequest
// do not abort the request before recvReqHeaders

public class Http2Message extends IOHChannel implements HttpMessage {

    private static class HeaderImpl implements HttpMessage.Header {
	private String _name, _value;
	private HeaderImpl _next;
	private HeaderImpl (String name, String value){
	    _name = name;
	    _value = value;
	}
	private void next (HeaderImpl h){ _next = h;}
	public String getName (){ return _name;}
	public byte[] getRawValue (){ return HttpParser.getUTF8 (_value);}
	public String getValue (){ return _value;}
	public int getValueAsInt (int def){
	    try{
		return Integer.parseInt (_value);
	    }catch(Exception e){
		return def;
	    }
	}
	public Header getNext (){ return _next;}
    }

    private boolean _isRequest = true;
    private String _method;
    private String _path;
    private String _scheme;
    private String _auth;
    private Map<String, HeaderImpl> _headersMap = new HashMap<> ();
    private boolean _isFirst = true, _isLast = false, _h2Version = false;
    private byte[] _body;
    private int _status;
    private HttpIOHEngine.HttpIOHTcpChannel _httpChannel;
    private HttpParser _parser = new HttpParser ().skipChunkDelimiters ();
    private Http2RequestListener.RequestContext _h2Ctx;
    private long _sentRequestTimestamp = Long.MAX_VALUE;
    private long _reqContentLen = -1L;
    private boolean _reqChunked;

    // _closed flag is managed in agent exec
    
    public Http2Message (HttpIOHEngine.HttpIOHTcpChannel channel, Http2RequestListener.RequestContext ctx){
	super (channel.getIOHEngine (), false);
	_httpChannel = channel;
	_logger = _httpChannel.getLogger ();
	_h2Ctx = ctx;
	_toString = new StringBuilder().append ("Http2Message[").append (ctx.id ()).append (']').toString ();
    }

    // must be called in agent exec
    @Override
    protected void notifyOpenToAgent (MuxClient agent, long connectionId){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyOpenToAgent : "+agent+" : id="+_id);
	_exec = agent.getPlatformExecutor ();
	agent.getTcpChannels ().put (_id, Http2Message.this);  // <-- require to be in agent exec (it is a hashmap)
	TcpChannel tcp = (TcpChannel)_httpChannel.getChannel ();
	InetSocketAddress remote = tcp.getRemoteAddress();
	InetSocketAddress local =  tcp.getLocalAddress();
	agent.getMuxHandler ().tcpSocketConnected (agent, _id, remote.getAddress().getHostAddress(), remote.getPort(), local.getAddress().getHostAddress(), local.getPort (), null, 0, tcp.isSecure (), true, connectionId, 0);
	
    }
    // must be called in agent exec
    @Override
    protected void notifyCloseToAgent (MuxClient agent){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyCloseToAgent : "+agent);
	agent.getMuxHandler ().tcpSocketClosed (agent, _id);
	agent.getTcpChannels ().remove (_id);   // <-- require to be in agent exec (it is a hashmap)
    }
    
    protected boolean sendAgent (MuxClient agent){
	_agent = agent;
	if("http".equals(agent.getApplicationParam("agent.protocol", null))) {
		_h2Version = true; //set HTTP Version to 2 for the http proxylet agent
	}
	
	ByteBuffer[] buffs = toByteBuffers ();
	boolean isFirst = _isFirst;
	boolean isLast = _isLast;
	

	
	_agent.schedule (() -> {
		// move to agent exec
		if (_agent.isOpened () == false){
		    close (false, true);
		    return;
		}
		if (isLast) _sentRequestTimestamp = System.currentTimeMillis ();
		if (isFirst){
		    makeId ();
		    notifyOpenToAgent (_agent, 0L);
		}
		if (buffs != null) // an empty body may generate this
		    _agent.getExtendedMuxHandler ().tcpSocketData (_agent, _id, 0L, buffs);
	    });
	return true;
    }
    @Override
    public boolean sendOut (MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	// called in agent exec if agent != null
	// agent can be null if response is from router
	HttpMessage resp = null;
	boolean done = false;
	for (int i=0; i<buffs.length; i++){
	    whileloop : while ((resp = _parser.parseMessage (buffs[i])) != null){
		final HttpMessage fresp = resp;
		final byte[] fbody = resp.getBody ();
		final boolean flast = resp.isLast ();
		done = flast;
		boolean hasBody = fbody != null && fbody.length > 0;
		if (resp.isFirst ()){
		    final boolean dataComing = !flast || hasBody;
		    _h2Ctx.responseExecutor ().execute (() -> {
			    if (_h2Ctx.isClosed ()) return;
			    _h2Ctx.setRespStatus (fresp.getStatus ());
			    fresp.iterateHeaders ((n, v) -> {
				    switch (n){
				    case "connection":
				    case "transfer-encoding":
					return;
				    }
				    _h2Ctx.setRespHeader (n, v);
				});
			    _h2Ctx.sendRespHeaders (!dataComing);
			    if (hasBody)
				_h2Ctx.sendRespData (ByteBuffer.wrap (fbody), false, flast);
			    int status = fresp.getStatus ();
			    Meter meter = ((HttpIOHEngine)_engine)._writeByTypeMeters.get (status);
			    if (meter != null){
				meter.inc (1);
			    } else {
				if (_logger.isInfoEnabled ())
				    _logger.info (_channel+" : sent response with unknown Status : "+status);
				((HttpIOHEngine)_engine)._writeByTypeMeters.get (999).inc (1);
			    }

			});
		    if (agent != null){
			long now = System.currentTimeMillis ();
			long elapsed = now - _sentRequestTimestamp;
			if (elapsed >= 0) { // by precaution avoid negative value - in case of pipeline or early response before end of request body
			    HttpIOHRouter.AgentContext ctx = _agent.getContext ();
			    Meter latencyMeter = ctx._latencyMeter;
			    long value = latencyMeter.getValue ();
			    long newValue = value + (elapsed >> 3) - (value >> 3);
			    latencyMeter.set (newValue);
			}
		    }
		    ((HttpMessageImpl)resp).setHasMore ();
		    continue whileloop;
		}
		_h2Ctx.responseExecutor ().execute (() -> {
			if (_h2Ctx.isClosed ()) return;
			_h2Ctx.sendRespData (ByteBuffer.wrap (fbody), false, flast);
		    });
	    }
	}
	if (done){
	    close (agent != null, false);
	}
	return true;
    }
    public boolean sendAgent (MuxClient agent, InetSocketAddress from, boolean checkBuffer, long sessionId, boolean copy, ByteBuffer... buffs){
	// not used
	return true;
    }
    protected void aborted (){
	if (_agent != null){
	    _agent.schedule ( () -> {
		    close (_agent.isOpened (), false);
		});
	}
    }
    @Override
    public void close (){
	// called in connection exec when router error
	if (_agent == null) close (false, true);
	else {
	    _agent.schedule ( () -> {
		    close (true, true);
		});
	}
    }
    @Override
    public boolean close (MuxClient agent){
	// called in agent exec
	close (true, true);
	return true;
    }
    // must be called in agent exec
    protected void close (boolean notifyAgent, boolean sendAbort){
	if (_closed) return;
	_closed = true;
	if (_id != 0) {
		_engine.releaseSocketId(_id);
	}
	if (notifyAgent)
	    notifyCloseToAgent (_agent);
	
	if (sendAbort){
	    _h2Ctx.responseExecutor ().execute (() -> {
		    _h2Ctx.abortStream (null, null);
		});
	}
    }
    public boolean agentConnected (MuxClient agent, MuxClientState state){ return false;}
    public boolean agentJoined (MuxClient agent, MuxClientState state){ return false;}
    public boolean agentClosed (MuxClient agent){
	close (false, true);
	return true;
    }

    
    public void recvReqMethod (String method){ _method = method;}

    public void recvReqPath (String path){_path = path;}

    public void recvReqScheme (String scheme){_scheme = scheme;}

    public void recvReqAuthority (String auth){_auth = auth;}

    public void recvReqHeader (String name, String value){
	HeaderImpl existingHeader = _headersMap.get (name);
	HeaderImpl newHeader = new HeaderImpl (name, value);
	if (existingHeader == null){
	    _headersMap.put (name, newHeader);
	} else {
	    while (true){
		HeaderImpl next = existingHeader._next;
		if (next == null){
		    existingHeader._next = newHeader;
		    break;
		}
		existingHeader = next;
	    }
	}
    }

    public void recvReqData (ByteBuffer buffer){
	_body = new byte[buffer.remaining ()];
	buffer.get (_body);
    }

    public void setHasMore (){ _isFirst = false;}
    public void done (){ _isLast = true;}

    /***********************************/

    public boolean isRequest (){ return _isRequest;}
    public boolean isFull (){ return _isFirst & _isLast;}
    public boolean isFirst () { return _isFirst;}
    public boolean isLast () { return _isLast;}
    public boolean isChunked (){ return false;}

    public <T> T getAgent (){ return (T) _agent;}
    public void setAgent (Object agent){ _agent = (MuxClient) agent;}

    
    public String getMethod (){ return _method;}
    public String getURL (){ return _path;}
    public int getStatus (){ return _status;}
    public int getVersion (){ return 2;}

    public byte[] getBody (){ return _body;}
    
    public void setBody (byte[] body){ _body = body;}

    public Header getHeader (String name){
	return _headersMap.get (name);
    }
    public void iterateHeaders (BiConsumer<String, String> consumer){
	for (String name: _headersMap.keySet ()){
	    HeaderImpl h = _headersMap.get (name);
	    while (h != null){
		consumer.accept (name, h.getValue ());
		h = h._next;
	    }
	}
	if (_auth != null) consumer.accept ("host", _auth);
    }
    
    public String getHeaderValue (String name){
	Header h = getHeader (name);
	return h != null ? h.getValue () : null;
    }
    public int getHeaderValueAsInt (String name, int def){
	Header h = getHeader (name);
	return h != null ? h.getValueAsInt (def) : def;
    }

    public HttpMessage addHeader (byte[] name, String value){
	String nameS = HttpParser.getFromUTF8 (name, 0, name.length);
	_headersMap.put (nameS, new HeaderImpl (nameS, value));
	return this;
    }
    public HttpMessage addHeader (byte[] name, byte[] value){
	return addHeader (name, HttpParser.getFromUTF8 (value, 0, value.length));
    }
    public HttpMessage removeHeader (Header h){
	_headersMap.remove (h.getName ());
	return this;
    }
    public HttpMessage removeHeaders (){
	_headersMap.clear ();
	return this;
    }

    public ByteBuffer[] toByteBuffers (){
	return toByteBuffers (_isFirst, _isFirst, true);
    }
    public ByteBuffer[] toByteBuffers (boolean firstLine, boolean headers, boolean body){
	body = body && _body != null && _body.length > 0;
	StringBuilder sb = (firstLine || headers) ? new StringBuilder () : null;
	if (firstLine) {
		sb.append (_method).append (' ').append (_path);
		
		if(_h2Version) {
			sb.append (" HTTP/2.0\r\n");
		} else {
			sb.append (" HTTP/1.1\r\n");
		}
	}
	
	if (headers){
		if(_h2Version) {
			// only for proxylet engine
			sb.append ("x-casr-scheme_auth:").append (_scheme).append(":").append(_auth).append ("\r\n");
		}
	    iterateHeaders (new BiConsumer<String, String> (){
		    public void accept (String name, String value){
			if (_reqContentLen == -1L &&
			    name.equalsIgnoreCase ("content-length")){
			    _reqContentLen = Long.parseLong (value);
			}
			sb.append (name).append (':').append (value).append ("\r\n");
		    }
		});
	    if (_reqContentLen == -1L){
		if (_isLast){
		    if (_body != null){
			sb.append ("Content-Length: ").append (_body.length).append ("\r\n");
		    }else{ // full request with no body : we must set 0 for PUT/POST/PATCH
			switch (_method){
			case "POST":
			case "PUT":
			case "PATCH":
			    sb.append ("Content-Length: 0\r\n");
			}
		    }
		} else {
		    _reqChunked = true;
		    sb.append ("Transfer-Encoding: chunked").append ("\r\n");
		}
	    }
	    sb.append ("\r\n");
	}
	if (sb != null){
	    byte[] b = HttpParser.getUTF8 (sb.toString ());
	    if (_reqChunked){
		return body ? new ByteBuffer[]{ByteBuffer.wrap (b), chunkInfo (_body.length), ByteBuffer.wrap (_body)} : new ByteBuffer[]{ByteBuffer.wrap (b)};
	    } else {
		return body ? new ByteBuffer[]{ByteBuffer.wrap (b), ByteBuffer.wrap (_body)} : new ByteBuffer[]{ByteBuffer.wrap (b)};
	    }
	}
	// body can be set to false if data is empty
	if (_reqChunked){
	    if (_isLast){
		return body ? new ByteBuffer[]{chunkInfo (_body.length), ByteBuffer.wrap (_body),  chunkEnd ()} : new ByteBuffer[]{chunkEnd ()};
	    } else {
		return body ? new ByteBuffer[]{chunkInfo (_body.length), ByteBuffer.wrap (_body)} : null;
	    }
	} else {
	    return body ? new ByteBuffer[]{ByteBuffer.wrap (_body)} : null;
	}
    }

    private static final byte[] CHUNKED_END = HttpParser.getUTF8 ("0\r\n\r\n");
    private static final byte[] CRLF_CHUNKED_END = HttpParser.getUTF8 ("\r\n0\r\n\r\n");

    private boolean _wroteFirstChunk = false;
    private ByteBuffer chunkInfo (int len){
	String s = Integer.toHexString(len);
	int size = s.length();
	byte[] h = null;
	int off = 0;
	if (_wroteFirstChunk) {
	    h = new byte[size + 4];
	    h[0] = (byte) '\r';
	    h[1] = (byte) '\n';
	    off = 2;
	} else {
	    h = new byte[size + 2];
	}
	for (int k = 0; k < size; k++)
	    h[off++] = (byte) s.charAt(k);
	h[off++] = (byte) '\r';
	h[off++] = (byte) '\n';
	_wroteFirstChunk = true;
	return ByteBuffer.wrap(h, 0, h.length);
    }
    private ByteBuffer chunkEnd (){
	byte[] bytes = _wroteFirstChunk ? CRLF_CHUNKED_END : CHUNKED_END;
	// copy by precaution
	byte[] copy = new byte[bytes.length];
	System.arraycopy (bytes, 0, copy, 0, bytes.length);
	return ByteBuffer.wrap (copy, 0, copy.length);
    }
    
}
