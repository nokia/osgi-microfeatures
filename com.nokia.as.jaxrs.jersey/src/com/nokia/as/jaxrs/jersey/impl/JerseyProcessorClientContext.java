// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.impl;

import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.HTTP10_100;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.HTTP10_404;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.HTTP11_100;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.HTTP11_404;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.HTTP10_503;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.HTTP11_503;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.PROP_JAXRS_SERVER_AUTH_HEADER;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.PROP_JAXRS_SERVER_SCHEME_HEADER;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.PROP_JAXRS_SERVER_OVERLOAD_INJECT_PATH;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;

import com.alcatel.as.http.parser.AccessLog;
import com.alcatel.as.http.parser.CommonLogFormat;
import com.alcatel.as.http.parser.HttpMessageImpl;
import com.alcatel.as.http.parser.HttpMeters;
import com.alcatel.as.http.parser.HttpParser;
import com.alcatel.as.http2.Connection;
import com.alcatel.as.http2.Http2RequestListener;
import com.alcatel.as.ioh.MessageParser;
import com.alcatel.as.ioh.tools.ByteBufferUtils;
import com.alcatel.as.ioh.tools.ChannelWriter;
import com.nokia.as.jaxrs.jersey.common.ClientContext;
import com.nokia.as.jaxrs.jersey.common.ServerContext;
import com.nokia.as.jaxrs.jersey.common.impl.HttpBodyInputStream;
import com.nokia.as.jaxrs.jersey.common.impl.ResponseWriter;

import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpChannelListener;

final class JerseyProcessorClientContext implements TcpChannelListener, ClientContext, Http2RequestListener {

	private static final int LOAD_INIT = Integer.getInteger ("jaxrs.server.overload.init", 0); // for testing !
	private static AtomicInteger _load = new AtomicInteger (LOAD_INIT);
    
	public static final byte[] VOID_BYTES = new byte[0];
	
	private ServerContext _serverCtx;
	private boolean _ignoreData;
	private TcpChannel _clientChannel;
	private MessageParser<HttpMessageImpl> _httpParser;
	private ContainerRequest _containerRequest;
	private HttpBodyInputStream _bodyStream;
	private Logger _log;
	private HttpMeters _meters;
	private final String _alias;
	private URI _baseUri;
	private ApplicationHandler _appHandler;
	private Connection _http2Connection;
	private boolean _http2;
	private DefaultProcessorSecurityContext securityContext = new DefaultProcessorSecurityContext();
	private Map<String, Object> _exportedKey;
	private final Executor _resourceExec;
	private AccessLog _accessLog;
	
	public JerseyProcessorClientContext(ServerContext serverCtx, TcpChannel client, URI baseUri, Executor resourceExec) {
		_serverCtx = serverCtx;
		_clientChannel = client;
		_log = _serverCtx.getLogger ();
		_baseUri = baseUri;
		_meters = _serverCtx.getMeters ();
		_meters.getOpenChannelsMeter().inc(1);
		_alias = _serverCtx.getAlias ();
		_appHandler = _serverCtx.getApplicationHandler ();
		if (serverCtx.getHttp2Config ().priorKnowledge ()){
			// prior-knowledge
			initHttp2 (false);
		} else {
			_httpParser = new HttpParser ().skipChunkDelimiters();
		}
		_resourceExec = resourceExec;
	}
	public String toString (){
		return new StringBuilder ().append ("JerseyProcessorClientContext[")
			.append (_baseUri).append (' ')
			.append (_clientChannel.getRemoteAddress ())
			.append (']').toString ();
	}
	private void initHttp2 (boolean skipPRI){
		_http2 = true;
		_http2Connection = _serverCtx.getHttp2ConnectionFactory ().newServerConnection (_serverCtx.getHttp2Config (), _clientChannel, this);
		_http2Connection.skipPRI (skipPRI).init ();
	}
	
	public ServerContext getServerContext (){ return _serverCtx;}
	public URI baseURI (){ return _baseUri;}
	public String alias (){ return _alias;}
    public ApplicationHandler appHandler (){ return _appHandler;}
	public Logger logger (){ return _log;}
	public JerseyProcessorClientContext exportedKey(Map<String, Object> key){ _exportedKey = key; return this;}
	public Map<String, Object> exportedKey (){ return _exportedKey;}
	public Executor getResourceExecutor() { return _resourceExec; }

	public boolean checkLoad (){
	    boolean ok = _serverCtx.getSendBufferMonitor ().check (_load.incrementAndGet (), null);
	    if (ok) return true;
	    _load.decrementAndGet ();
	    return false;
	}
	public void offLoad (){ _load.decrementAndGet ();}
	
	@Override
	public int messageReceived(TcpChannel client, ByteBuffer data) {
		
		if (_ignoreData) {
			data.position(data.limit());
			return 0;
		}

		if (_http2){
			return http2Received (client, data);
		}

		int init = data.position(); // in case of error to log the buffer
		try {
			// parseHttpMessage
			HttpMessageImpl req;
			
			while ((req = _httpParser.parseMessage(data)) != null) {
				if (req.isRequest () == false)
					throw new java.io.IOException ("Received a response : only requests are expected");				
				CommonLogFormat clf = _serverCtx.getCommonLogFormat ();
				if (_log.isDebugEnabled())
					_log.debug(this + " : client messageReceived:\n" + req);
				else if (clf.getLogger ().isDebugEnabled ()) // this is to facilitate traffic logging : access in DEBUG --> traffic
				    clf.getLogger ().debug(this + " : client messageReceived:\n" + req);
				
				if (req.isFirst()) {
					req.setHasMore(); // required by the parser - so next read does not return the req if no new data
					_meters.getReadReqMeter(req.getMethod()).inc(1);

					// log request
					_accessLog = new AccessLog();
					if (clf.isEnabled())
						_accessLog.request(req).remoteIP(client.getRemoteAddress().getAddress());

					String expect = req.getHeaderValue("expect");
					if (expect != null && expect.contains("100-continue")) {
						respond (client, 100, req.getVersion () == 0 ? HTTP10_100 : HTTP11_100, null, null, false);
					}

					// create request for jersey
					MapPropertiesDelegate propertiesDelegate = new MapPropertiesDelegate();
					propertiesDelegate.setProperty(TcpChannel.class.getName(), client.toString());
					
					if(client.isSecure()) {
						if(client.getSecurity().authenticateClients()) {
							propertiesDelegate.setProperty("javax.servlet.request.X509Certificate", client.getSSLEngine().getSession().getPeerCertificates());
						}
					}

					URI base = _baseUri;
					URI uri = UriBuilder.fromUri(req.getURL ()).build();

					String host = null;
					String scheme = null;
					String authHeader = _serverCtx.getAuthHeaderH1 ();
					if (authHeader != null){
					    host = req.getHeaderValue(authHeader);					    
					}
					String schemeHeader = _serverCtx.getSchemeHeaderH1 ();
					if (schemeHeader != null){
					    scheme = req.getHeaderValue(schemeHeader);					    
					}
					
					if (host != null){
					    if (scheme == null) scheme = _baseUri.getScheme ();
					    base = new URI (scheme, host, _alias, null, null);
					}else{
					    if (scheme != null){
						base = new URI (scheme, _baseUri.getAuthority (), _alias, null, null);
					    }
					}
					
					_containerRequest = new ContainerRequest(base, uri, req.getMethod(),
										 securityContext, propertiesDelegate);

					if (_serverCtx.tlsExportEnabled () &&
					    _exportedKey == null){
					    exportedKey (client.exportTlsKey (_serverCtx.tlsExportLabel (), VOID_BYTES, _serverCtx.tlsExportLen ()));
					}
					if (_exportedKey != null)
					    propertiesDelegate.setProperty ("tcp.secure.keyexport", _exportedKey);

					boolean keepAlive;
					String ka = req.getHeaderValue("connection");
					if (req.getVersion() == 1) {
						keepAlive = (ka == null) || (ka.toLowerCase().indexOf("close") == -1);
					} else {
						keepAlive = ka != null && ka.toLowerCase().indexOf("keep-alive") > -1;
					}

					_containerRequest.setWriter(new ResponseWriter(this, keepAlive, req.getVersion(), _accessLog));

					req.iterateHeaders(_containerRequest::header);

					_bodyStream = new HttpBodyInputStream();
				}
				_bodyStream.addBody(req.getBody());
				if (req.isLast()) {
					if (_bodyStream.available() > 0) {
						_containerRequest.setEntityStream(_bodyStream);
					}
					// do request
					if (checkLoad ()){
					    ContainerRequest creq = _containerRequest;
					    Runnable r = new Runnable (){
						public void run (){
						    try{ _appHandler.handle(creq); }
						    finally { offLoad ();}
						}
					    };
					    _resourceExec.execute(r);
					} else {
					    if (_serverCtx.injectOverload ()){
						String s = _serverCtx.getProperties ().get (PROP_JAXRS_SERVER_OVERLOAD_INJECT_PATH).toString ()+
						    req.getURL ().substring (_alias.length ());						
						URI overuri = UriBuilder.fromUri(s).build();
						_containerRequest.setRequestUri (overuri);
						_appHandler.handle(_containerRequest);
					    } else {
						respond (client, 503, req.getVersion () == 0 ? HTTP10_503 : HTTP11_503, _accessLog, clf, true);
					    }
					}
					_containerRequest = null;
					_bodyStream = null;
					_accessLog = null;
				}
			}
		} catch (Exception e) {
			if (e instanceof HttpParser.Http2Exception){
				if (_log.isDebugEnabled ())
					_log.debug(this + " : switching to http2");
				initHttp2 (true);
				return messageReceived (client, data);
			}
			if (_log.isInfoEnabled()) {
				data.position(init);
				String s = ByteBufferUtils.toUTF8String(true, data);
				_log.info(this + " : exception while parsing\n" + s, e);
			}
			_meters.getParserErrorMeter().inc(1);
			data.position(data.limit());
			close();
			_ignoreData = true; // disable future reads until closed is called back
		}

		return 0;
	}

	public void send (ByteBuffer data, boolean copy){
		_clientChannel.send (data, copy);
	}

	protected int respond (TcpChannel client, int status, byte[] data, AccessLog log, CommonLogFormat cl, boolean close){
		send(ByteBuffer.wrap(data), false);
		_meters.getWriteRespMeter(status).inc(1);
		if (log != null && !log.isEmpty ()){
			cl.log (log.responseStatus (status).responseSize (0));
		}
		if (close){
			_ignoreData = true; // disable future reads until closed is called back
			close();
		}
		return 0;
	}

	public void close() {
		if (_log.isDebugEnabled())
			_log.debug(this + " : close client");
		_clientChannel.close();
	}

	public void setSuspendTimeout (long timeOut){
		_clientChannel.setSoTimeout(timeOut);
	}

	@Override
	public void receiveTimeout(TcpChannel channel) {
		if (_http2){
			_http2Connection.receiveTimeout ();
		} else {
			if (_log.isDebugEnabled())
				_log.debug(this + " : client receiveTimeout : closing");
			close ();
		}
	}

	@Override
	public void writeBlocked(TcpChannel channel) {
	}

	@Override
	public void writeUnblocked(TcpChannel channel) {
	}

	@Override
	public void connectionClosed(TcpChannel cnx) {
		if (_log.isDebugEnabled()) {
			_log.debug(this + " : clientClosed");
		}
		_meters.getOpenChannelsMeter().inc(-1);
		_meters.getClosedChannelsMeter().inc(1);
		if (_http2Connection != null)
			_http2Connection.closed ();
	}

	/************************ HTTP2 ***************************/
	
	private int http2Received (TcpChannel client, ByteBuffer data) {
		if (_serverCtx.tlsExportEnabled () &&
		    _exportedKey == null){
		    exportedKey (client.exportTlsKey (_serverCtx.tlsExportLabel (), VOID_BYTES, _serverCtx.tlsExportLen ()));
		}
		try{
			_http2Connection.received (data);
		}catch(Throwable t){
			_log.info(this + " : exception in http2 parser", t);
			_meters.getParserErrorMeter().inc(1);
			data.position(data.limit());
			close();
			_ignoreData = true; // disable future reads until closed is called back
		}
		return 0;
	}
    
	@Override
	public void newRequest (RequestContext rc){
		if (_log.isDebugEnabled ())
			_log.debug (this+" : http2 : newRequest");
		rc.attach (new JerseyProcessorHttp2Request (this, rc));
	}
	@Override
	public void recvReqMethod (RequestContext rc, String method){
		if (_log.isDebugEnabled ())
			_log.debug (this+" : http2 : recvReqMethod "+method);
		_meters.getReadReqMeter(method).inc(1);
		((JerseyProcessorHttp2Request) rc.attachment ()).recvReqMethod (method);
	}
	@Override
	public void recvReqPath (RequestContext rc, String path){
		if (_log.isDebugEnabled ())
			_log.debug (this+" : http2 : recvReqPath "+path);
		((JerseyProcessorHttp2Request) rc.attachment ()).recvReqPath (path);
	}
	@Override
	public void recvReqScheme (RequestContext rc, String scheme){
		if (_log.isDebugEnabled ())
			_log.debug (this+" : http2 : recvReqScheme "+scheme);
		((JerseyProcessorHttp2Request) rc.attachment ()).recvReqScheme (scheme);
	}
	@Override
	public void recvReqAuthority (RequestContext rc, String auth){
		if (_log.isDebugEnabled ())
			_log.debug (this+" : http2 : recvReqAuthority "+auth);
		((JerseyProcessorHttp2Request) rc.attachment ()).recvReqAuthority (auth);
	}
	@Override
	public void recvReqHeader (RequestContext rc, String name, String value){
		if (_log.isDebugEnabled ())
			_log.debug (this+" : http2 : recvReqHeader "+name+" "+value);
		((JerseyProcessorHttp2Request) rc.attachment ()).recvReqHeader (name, value);
	}
	@Override
	public void recvReqHeaders (RequestContext rc, boolean done){
		if (_log.isDebugEnabled ())
			_log.debug (this+" : http2 : recvReqHeaders");
		((JerseyProcessorHttp2Request) rc.attachment ()).recvReqHeaders ();
	}
	@Override
	public void recvReqData (RequestContext rc, ByteBuffer data, boolean done){
		if (_log.isDebugEnabled ())
			_log.debug (this+" : http2 : recvReqData "+data.remaining ());
		((JerseyProcessorHttp2Request) rc.attachment ()).recvReqData (data);
	}
	@Override
	public void endRequest (RequestContext rc){
		if (_log.isDebugEnabled ())
			_log.debug (this+" : http2 : endRequest");
		((JerseyProcessorHttp2Request) rc.attachment ()).endRequest ();
	}
	@Override
	public void abortRequest (RequestContext rc){
		if (_log.isDebugEnabled ())
			_log.debug (this+" : http2 : abortRequest");
		((JerseyProcessorHttp2Request) rc.attachment ()).abortRequest ();
	}
}
