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

import com.alcatel.as.http2.*;
import com.alcatel.as.http2.client.api.*;

import java.util.concurrent.*;

import com.alcatel.as.http2.Http2RequestListener.RequestContext;

public class Http2RequestContext implements HttpRequest.BodyPublisher, Flow.Subscription, HttpResponse.BodyHandler<byte[]>, HttpResponse.BodySubscriber<byte[]>{

    ClientContext _clientCtx;
    Callback _cb;
    String _path, _auth, _scheme;
    URI _uri;
    HttpRequest.Builder _builder;
    Flow.Subscriber<? super ByteBuffer> _reqFlowSub;
    CompletableFuture _cf;
    long _contentLen = -1L;
    ArrayList<ByteBuffer> _reqBuffers;
    int _reqBuffersSize = 0;
    boolean _reqData = false;
    boolean _reqDataDone = false;
    HttpProxy.HeadersContext _reqHeadersCtx, _respHeadersCtx;
    Logger _logger;
    HttpProxy.ProxyConf _pxConf;
    
    protected Http2RequestContext (ClientContext cc, Callback cb){
	_clientCtx = cc;
	_cb = cb;
	_pxConf = _clientCtx._pxConf;
	_logger = _clientCtx._logger;
	_builder =  _clientCtx._proc.h2ClientFactory ().newHttpRequestBuilder ();
	_reqHeadersCtx = _pxConf.newClientHeadersContext ();
	_respHeadersCtx = _pxConf.newServerHeadersContext ();
    }
    @Override
    public String toString (){ return _clientCtx.toString ();}
    
    protected void method (String method){
	switch (method.toUpperCase ()){
	case "GET": _builder.GET (); break;
	case "DELETE": _builder.DELETE (); break;
	case "POST": _builder.POST (this); _reqData = true; break;
	case "PUT": _builder.PUT (this); _reqData = true; break;
	default: _builder.method (method, this); _reqData = true; break;
	}
    }
    protected void recvReqHeader (String name, String value){
	switch (name){
	case "content-length":
	    _contentLen = Long.parseLong (value);
	    return;
	case "x-forwarded-for":
	case "x-forwarded-proto":
	case "x-forwarded-port":
	    if (_pxConf._xForwardedRemove)
		return;
	}
	final String valueF = value;
	if (_pxConf.handleClientHeader (name, ()->{return valueF;}, _reqHeadersCtx))
	    _builder.header (name, value);
    }
    protected void exec (){
	_pxConf.endClientHeaders ((name, value) -> {_builder.header (name, value);}, _reqHeadersCtx);
	if (_pxConf._xForwardedAdd){
	    _builder.header ("x-forwarded-for", _clientCtx._clientChannel.getRemoteAddress ().getAddress ().getHostAddress ());
	    _builder.header ("x-forwarded-port", String.valueOf (_clientCtx._clientChannel.getLocalAddress ().getPort ()));
	    _builder.header ("x-forwarded-proto", _clientCtx._clientChannel.isSecure () ? "https" : "http");
	}
	_reqHeadersCtx = null; // help GC
	try {
	    if (_path.startsWith ("http://") ||
		_path.startsWith ("https://")){ // curl sends the whole URI in the path : it looks invalid, but lets support it
		_uri = new URI (_path);
		StringBuilder sb = new StringBuilder ();
		sb.append (_uri.getRawPath ());
		String tmp = null;
		if ((tmp=_uri.getRawQuery ()) != null) sb.append ('?').append (tmp);
		if ((tmp=_uri.getRawFragment ()) != null) sb.append ('#').append (tmp);
		_path = sb.toString ();
	    } else
		_uri = new URI(_scheme, _auth, _path, null, null);
	} catch (URISyntaxException e) {
	    if (_logger.isInfoEnabled ())
		_logger.info(this+": invalid uri : scheme="+_scheme+", auth="+_auth+", path="+_path, e);
	    _cb.responseExecutor ().execute (() -> {
		    abortClientReq ("Invalid URI");
		});
	    return;
	}
	HttpRequest req = _builder.uri (_uri)
	    .header(ClientContext.RAW_PATH_PSEUDOHEADER, _path)
	    .build ();
	  
	_builder = null;
	_cf = _clientCtx._http2Client.sendAsync (req, this);
	_cf.whenCompleteAsync((response, exception) -> {
		if (exception != null) {
		    if (exception instanceof AbortException) return; // triggered by us
		    Throwable t = (Throwable) exception;
		    if (_logger.isInfoEnabled ()) _logger.info (this+" : unexpected error", t);
		    abortClientReq (t.getMessage ());
		}}, _cb.responseExecutor ());
    }

    // called in _cb _reqExec
    protected void abort (){
	abortServerReq ();
    }

    // called in _cb _respExec
    protected void abortClientReq (String msg){
	if (_cb.isClosed ()) return;
	_cb.abortReq (msg);
    }
    protected void abortServerReq (){
	if (_cf != null)
	    _cf.completeExceptionally(new AbortException());
    }

    protected void reqData (ByteBuffer data, boolean copy){
	int n = 0;
	if (data != null && (n=data.remaining ()) > 0){
	    ByteBuffer copied = data;
	    if (copy){
		copied = ByteBuffer.allocate (n);
		copied.put (data);
		copied.flip ();
	    }
	    if (_reqFlowSub != null){
		_reqFlowSub.onNext (copied);
	    }else{
		if (_reqBuffers == null) _reqBuffers = new ArrayList<> (3);
		_reqBuffers.add (copied);
		_reqBuffersSize += n;
		if (_reqBuffersSize > _pxConf._serverStreamMaxSendBuffer){
		    _reqBuffers = null;
		    _cb.responseExecutor ().execute (() -> {
			    abortClientReq ("Server Buffer exceeded");
			});
		    abortServerReq ();
		}
	    }
	}
    }
    protected void reqDone (){
	if (_reqData){
	    if (_reqFlowSub != null) _reqFlowSub.onComplete ();
	    else _reqDataDone = true;
	}
    }
	
    // HttpRequest.BodyPublisher
	
    public long contentLength(){
	return _contentLen;
    }

    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber){
	subscriber.onSubscribe (this);
	_cb.requestExecutor ().execute (() -> {
		_reqFlowSub = subscriber;
		if (_reqBuffers != null){
		    for (ByteBuffer buffer : _reqBuffers){
			_reqFlowSub.onNext (buffer);
		    }
		    _reqBuffers = null;
		}
		if (_reqDataDone) _reqFlowSub.onComplete ();
	    });
    }

    // Flow.Subscription

    public void request(long n){
    }
	
    public void cancel(){
    }

    // HttpResponse.BodyHandler
	
    public HttpResponse.BodySubscriber<byte[]> apply(HttpResponse.ResponseInfo responseInfo) {
	_cb.responseExecutor ().execute (() -> {
		if (_cb.isClosed ()) return;
		_cb.setRespStatus (responseInfo.statusCode ());
		Map<String, List<String>> headers = responseInfo.headers ().map();
		for (String name : headers.keySet ()){
		    for (String value: headers.get (name)){
			final String valueF = value;
			if (_pxConf.handleServerHeader (name, () -> {return valueF;}, _respHeadersCtx))
			    _cb.setRespHeader (name, valueF);
		    }
		}
		_pxConf.endServerHeaders ((name, value) -> {_cb.setRespHeader (name, value);}, _respHeadersCtx);
		_respHeadersCtx = null; // help GC
		_cb.sendRespHeaders (false);
	    });
	return this;
    }

    // HttpResponse.BodySubscriber
	
    public void onSubscribe(Flow.Subscription subscription){}
    public void onNext(List<ByteBuffer> item){
	_cb.responseExecutor ().execute (() -> {
		if (_cb.isClosed ()) return;
		for (ByteBuffer buffer : item){
		    try{
			_cb.sendRespData (buffer, false, false);
		    }catch(IndexOutOfBoundsException e){
			// buffer exceeded
			abortClientReq ("SendBuffer exceeded");
			abortServerReq ();
		    }
		}
	    });
    }
    public void onError(java.lang.Throwable throwable){
	// handled in CompletableFuture
    }
    public void onComplete(){
	_cb.responseExecutor ().execute (() -> {
		if (_cb.isClosed ()) return;
		_cb.sendRespData (null, false, true);
	    });
    }
    public java.util.concurrent.CompletionStage<byte[]> getBody(){
	CompletableFuture<byte[]> cf = new CompletableFuture<byte[]>();
	cf.complete(null);
	return cf;
    }
    
    public static class AbortException extends Exception {
	public AbortException (){ super ();}
    }


    public interface Callback {

	public Executor requestExecutor ();
	public Executor responseExecutor ();
	public boolean isClosed ();
	public void abortReq (String msg);
	public void setRespStatus (int status);
	public void setRespHeader (String name, String value);
	public void sendRespHeaders (boolean done);
	public void sendRespData (ByteBuffer data, boolean copy, boolean done);
    }
}
