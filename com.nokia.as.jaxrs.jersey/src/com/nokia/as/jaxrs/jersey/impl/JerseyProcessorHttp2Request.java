// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.impl;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

import com.alcatel.as.http2.Http2RequestListener;
import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.nokia.as.jaxrs.jersey.common.impl.HttpBodyInputStream;
import com.nokia.as.jaxrs.jersey.common.ServerContext;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.PROP_JAXRS_SERVER_OVERLOAD_INJECT_PATH;

public class JerseyProcessorHttp2Request extends OutputStream implements ContainerResponseWriter {

    private JerseyProcessorClientContext _clientCtx;
    private String _method, _scheme, _auth, _path;
    private ContainerRequest _cr;
    private HttpBodyInputStream _bodyStream = new HttpBodyInputStream();
    private Http2RequestListener.RequestContext _reqCtx;
    
    public JerseyProcessorHttp2Request (JerseyProcessorClientContext cc, Http2RequestListener.RequestContext reqCtx){
	_clientCtx = cc;
	_reqCtx = reqCtx;
    }
    private void initContainerRequest (){
	if (_cr != null) return;
	MapPropertiesDelegate propertiesDelegate = new MapPropertiesDelegate();
	if (_clientCtx.exportedKey () != null)
	    propertiesDelegate.setProperty ("tcp.secure.keyexport", _clientCtx.exportedKey ());
	_cr = new ContainerRequest(null,
				   null,
				   _method,
				   new DefaultProcessorSecurityContext(),
				   propertiesDelegate);
	_cr.setWriter(this);
    }

    public void recvReqMethod (String method){
	_method = method;
    }

    public void recvReqPath (String path){
	_path = path;
    }

    public void recvReqScheme (String scheme){_scheme = scheme;}

    public void recvReqAuthority (String auth){_auth = auth;}
    
    public void recvReqHeader (String name, String value){
	initContainerRequest ();
	_cr.header (name, value);
    }

    public void recvReqHeaders (){
	initContainerRequest ();
    }

    public void recvReqData (ByteBuffer data){
	byte[] bytes = new byte[data.remaining ()];
	data.get (bytes);
	_bodyStream.addBody(bytes);
    }
    
    public void endRequest (){
	final ServerContext sc = _clientCtx.getServerContext ();
	try{	    
	    String scheme = null;
	    String schemeHeader = sc.getSchemeHeaderH2 ();
	    if (schemeHeader != null){
		scheme = _cr.getHeaderString (schemeHeader);
	    }
	    if (scheme == null){
		if (sc.usePseudoSchemeHeader ()) scheme = _scheme;
		if (scheme == null) scheme = _clientCtx.baseURI ().getScheme ();
	    }
	    String auth = null;
	    String authHeader = sc.getAuthHeaderH2 ();
	    if (authHeader != null){
		auth = _cr.getHeaderString (authHeader);
	    }
	    if (auth == null){
		if (sc.usePseudoAuthHeader ()) auth = _auth;
		if (auth == null) auth = _clientCtx.baseURI ().getAuthority ();
	    }
	    URI baseURI = new URI (scheme, auth, sc.getAlias (), null, null);
	    URI reqURI = UriBuilder.fromUri(_path).build();
	    _cr.setRequestUri (baseURI, reqURI);
	}catch(Exception e){	    // cannot call failure (logs in error and rethrows exception
	    if (sc.getLogger ().isDebugEnabled ())
		sc.getLogger ().debug(_clientCtx + " : invalid URI", e);
	
	    Runnable r = new Runnable (){
		    public void run (){
			if (_reqCtx.isClosed ()) return;
			_reqCtx.setRespStatus (500);
			_reqCtx.sendRespHeaders (true);
			sc.getMeters ().getWriteRespMeter(500).inc(1);
		    }
		};
	    _reqCtx.responseExecutor ().execute (r);
	    
	    return;
	}
	if (_bodyStream.available() > 0)
	    _cr.setEntityStream(_bodyStream);
	
	if (_clientCtx.checkLoad ()){
	    ContainerRequest creq = _cr;
	    Runnable r = new Runnable (){
		    public void run (){
			try{ _clientCtx.appHandler ().handle(creq); }
			finally { _clientCtx.offLoad ();}
		    }
		};	
	    _clientCtx.getResourceExecutor ().execute(r);
	} else {
	    if (sc.injectOverload ()){
		String s = sc.getProperties ().get (PROP_JAXRS_SERVER_OVERLOAD_INJECT_PATH).toString ()+
		    _path.substring (_clientCtx.alias ().length ());						
		URI overuri = UriBuilder.fromUri(s).build();
		_cr.setRequestUri (overuri);
		_clientCtx.appHandler ().handle(_cr);
	    } else {
		Runnable r = new Runnable (){
			public void run (){
			    if (_reqCtx.isClosed ()) return;			    
			    _reqCtx.setRespStatus (503);
			    _reqCtx.sendRespHeaders (true);
			    sc.getMeters ().getWriteRespMeter(503).inc(1);
			}
		    };
		_reqCtx.responseExecutor ().execute (r);
	    }
	}
    }

    public void abortRequest (){};


    /**************** ContainerResponseWriter ***************/

    private boolean _done;

    public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse response){
	_done = contentLength == 0;  // -1 means jersey streaming
	int code = response.getStatusInfo().getStatusCode();
	Runnable r = new Runnable (){
		public void run (){
		    if (_reqCtx.isClosed ()) return;
		    _reqCtx.setRespStatus (code);
		    for (final Map.Entry<String, List<String>> e : response.getStringHeaders().entrySet()) {
			for (final String value : e.getValue())
			    _reqCtx.setRespHeader (e.getKey (), value);
		    }
		    _reqCtx.sendRespHeaders (_done);
		    _clientCtx.getServerContext ().getMeters ().getWriteRespMeter(code).inc(1);
		}
	    };
	_reqCtx.responseExecutor ().execute (r);
	return this;
    }

    
    public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) {}
    public boolean suspend(long timeOut, TimeUnit timeUnit, TimeoutHandler timeoutHandler) {
	return true;
    }

    public void commit(){
	if (!_done)
	    _reqCtx.responseExecutor ().execute (() -> {
		    if (_reqCtx.isClosed ()) return;
		    _reqCtx.sendRespData (null, false, true);
		});
    }

    public void failure(Throwable error){
	_clientCtx.getServerContext ().getLogger ().error(_clientCtx + " : jersey processing error", error);
	
	Runnable r = new Runnable (){
		public void run (){
		    if (_reqCtx.isClosed ()) return;
		    _reqCtx.setRespStatus (500);
		    _reqCtx.sendRespHeaders (true);
		    _clientCtx.getServerContext ().getMeters ().getWriteRespMeter(500).inc(1);
		}
	    };
	_reqCtx.responseExecutor ().execute (r);

	// Rethrow the original exception as required by JAX-RS, 3.3.4.
	if (error instanceof RuntimeException) {
	    throw (RuntimeException) error;
	} else {
	    throw new ContainerException(error);
	}
    }
	
    public boolean enableResponseBuffering(){return false;}

    /************************************/
    /** Implementation of OutputStream **/
    /************************************/
    public void write(int b) {
	write(new byte[] { (byte) b }, 0, 1);
    }
    
    public void write(byte b[]) {
	write(b, 0, b.length);
    }
    
    public void write(byte b[], int from, int len) {
	if (len == 0)
	    return;
	ByteBuffer buffer = ByteBuffer.allocate (len);
	buffer.put (b, from, len);
	buffer.flip ();
	_reqCtx.responseExecutor ().execute (() -> {
		    if (_reqCtx.isClosed ()) return;
		    _reqCtx.sendRespData (buffer, false, false);
		});
    }
    
    public void flush() {
    }

}
