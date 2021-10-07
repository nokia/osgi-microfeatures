package com.alcatel.as.http.proxy;

import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;

import com.alcatel.as.http.parser.*;
import com.alcatel.as.ioh.server.Server;
import com.alcatel.as.ioh.tools.ByteBufferUtils;
import org.apache.log4j.Logger;
import java.util.*;
import java.net.*;
import java.nio.*;

import com.alcatel_lucent.as.service.dns.*;
import com.alcatel.as.http2.*;
import com.alcatel.as.http2.client.api.*;

import java.util.concurrent.*;

public class Http1ToHttp2 implements HttpMessageFilter {

    private static final byte[] CHUNKED_END = HttpParser.getUTF8 ("0\r\n\r\n");
    private static final byte[] CRLF_CHUNKED_END = HttpParser.getUTF8 ("\r\n0\r\n\r\n");
    
    private static final byte[] TRANSFER_ENCODING_END = HttpParser.getUTF8 ("Transfer-Encoding: chunked\r\n\r\n");
    private static final byte[] CONNECTION_KEEPALIVE = HttpParser.getUTF8 ("Connection: keep-alive\r\n");
    private static final byte[] CRLF = HttpParser.getUTF8 ("\r\n");

    private static final byte[] CONTINUE_0 = HttpParser.getUTF8 ("HTTP/1.0 100 Continue\r\n\r\n");
    private static final byte[] CONTINUE_1 = HttpParser.getUTF8 ("HTTP/1.1 100 Continue\r\n\r\n");

    private static final Map<Integer, byte[]>[] STATUSES = new Map[2];
    static {
	STATUSES[0] = new HashMap<> ();
	STATUSES[1] = new HashMap<> ();
	for (int i=100; i<600; i++){
	    String reason = HttpStatuses.getReason (i);
	    if (reason == null) continue;
	    String line = i+" "+reason+"\r\n";
	    STATUSES[0].put (i, HttpParser.getUTF8 ("HTTP/1.0 "+line));
	    STATUSES[1].put (i, HttpParser.getUTF8 ("HTTP/1.1 "+line));
	}
    }

    ClientContext _clientCtx;
    Http1ToHttp2RequestContext _h1ReqCtx;
    
    public Http1ToHttp2 (ClientContext cc){
	_clientCtx = cc;
    }

    /*************** HttpMessageFilter *************/

    @Override
    public void init (HttpMessage msg){
	if (_h1ReqCtx != null) throw new IllegalStateException ("Request in flight, pipeline not allowed");
	_h1ReqCtx = new Http1ToHttp2RequestContext ();
    }

    @Override
    public void method (HttpMessage msg, String method){
	_h1ReqCtx._h2ReqCtx.method (method);
    }

    @Override
    public void url (HttpMessage msg, String url){
	_h1ReqCtx._h2ReqCtx._path = url;
    }

    @Override
    public boolean header (HttpMessage msg, String name, java.util.function.Supplier<String> value){
	switch (name){
	case "expect":
	    _h1ReqCtx._expect100 = value.get ().contains("100-continue");
	    break;
	case "connection":
	case "proxy-connection":
	    _h1ReqCtx._connectionPolicy = value.get ();
	case "host":
	case "transfer-encoding":
	case "accept-encoding":
	case "http2-settings":
	case "upgrade":
	    break;
	default:
	    _h1ReqCtx._h2ReqCtx.recvReqHeader (name, value.get ());
	}
	return false;
    }
    
    /************* methods called by ClientContext ************/

    public void headers (HttpMessage msg){
	int version = msg.getVersion ();
	_h1ReqCtx._version = version;
	if (version == 0){
	    _h1ReqCtx._connectionClose = (_h1ReqCtx._connectionPolicy == null ||
					  !_h1ReqCtx._connectionPolicy.equalsIgnoreCase ("keep-alive"));
	} else {
	    _h1ReqCtx._connectionClose = (_h1ReqCtx._connectionPolicy != null &&
					  _h1ReqCtx._connectionPolicy.equalsIgnoreCase ("close"));
	}
	if (_h1ReqCtx._expect100)
	    _clientCtx._clientChannel.send(ByteBuffer.wrap(_h1ReqCtx._version == 0 ? CONTINUE_0 : CONTINUE_1), false);
	_h1ReqCtx._h2ReqCtx.exec ();
	if (msg.isLast ()){
	    _h1ReqCtx._h2ReqCtx.reqDone ();
	}
    }

    public void data (HttpMessage msg){
	_h1ReqCtx._h2ReqCtx.reqData (ByteBuffer.wrap (msg.getBody ()), false);
	if (msg.isLast ()){
	    _h1ReqCtx._h2ReqCtx.reqDone ();
	}
    }

    public void abort (){
	if (_h1ReqCtx != null){
	    if (_h1ReqCtx._closed) return;
	    _h1ReqCtx._closed = true;
	    _h1ReqCtx._h2ReqCtx.abort ();
	    _h1ReqCtx = null;
	}
    }

    /***************************/

    private class Http1ToHttp2RequestContext implements Http2RequestContext.Callback {

	
	boolean _closed, _chunked, _wroteFirstChunk;
	String _connectionPolicy;
	boolean _connectionClose, _expect100;
	int _clen = -1;
	int _version;
	Http2RequestContext _h2ReqCtx;

	private Http1ToHttp2RequestContext (){
	    _h2ReqCtx = new Http2RequestContext (_clientCtx, this);
	}
	
	public Executor requestExecutor (){ return _clientCtx._exec;}
	
	public Executor responseExecutor (){ return _clientCtx._exec;}
	
	public boolean isClosed (){ return _closed;}

	public void setRespStatus (int status){
	    byte[] bytes = STATUSES[_version].get (status);
	    if (bytes == null) bytes = _version == 0 ?
				   HttpParser.getUTF8 ("HTTP/1.0 "+status+"\r\n"):
				   HttpParser.getUTF8 ("HTTP/1.1 "+status+"\r\n");
	    _clientCtx._clientChannel.send (bytes, false);
	    if (_version == 0 && !_connectionClose) _clientCtx._clientChannel.send (CONNECTION_KEEPALIVE, false);
	}

	public void setRespHeader (String name, String value){
	    String s = new StringBuilder ().append (name).append (':').append (value).append ("\r\n").toString ();
	    _clientCtx._clientChannel.send (HttpParser.getUTF8 (s), false);
	    if (name.equalsIgnoreCase ("content-length"))
		_clen = Integer.parseInt (value);
	}

	public void sendRespHeaders (boolean done){
	    if (_clen == -1){
		_chunked = true;
		_clientCtx._clientChannel.send (TRANSFER_ENCODING_END, false);
	    } else {
		_clientCtx._clientChannel.send (CRLF, false);
	    }
	    if (done) done ();
	}
	
	public void sendRespData (ByteBuffer data, boolean copy, boolean done){
	    if (data != null && data.remaining () > 0){
		if (_chunked) {
		    String s = Integer.toHexString(data.remaining ());
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
		    _clientCtx._clientChannel.send(ByteBuffer.wrap(h, 0, h.length), false);
		    _wroteFirstChunk = true;
		}
		_clientCtx._clientChannel.send (data, copy);
	    }
	    if (done && _chunked) {
		if (_wroteFirstChunk){
		    _clientCtx._clientChannel.send(ByteBuffer.wrap(CRLF_CHUNKED_END, 0, CRLF_CHUNKED_END.length), false);
		} else {
		    _clientCtx._clientChannel.send(ByteBuffer.wrap(CHUNKED_END, 0, CHUNKED_END.length), false);
		}
	    }
	    if (done) done ();
	}

	public void abortReq (String msg){
	    // called by h2 : do not abort h2 in reaction
	    _closed = true;
	    _h1ReqCtx = null;
	    _clientCtx.closeClient (true);
	}

	private void done (){
	    _closed = true;
	    _h1ReqCtx = null;
	    if (_connectionClose) _clientCtx.closeClient (false);
	}
	
    }

}
