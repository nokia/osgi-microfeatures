// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http.ioh.impl;

import com.alcatel.as.http.parser.*;
import com.alcatel.as.http2.*;
import com.alcatel.as.http.ioh.*;

import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.tools.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.tools.ChannelWriter.SendBufferMonitor;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.*;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.ioh.impl.conf.Property;

import java.util.regex.*;

import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;
import com.alcatel_lucent.as.management.annotation.config.*;
import com.alcatel.as.service.discovery.*;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;
import com.nextenso.mux.*;
import com.nextenso.mux.util.MuxIdentification;

public class HttpIOHEngine extends IOHEngine implements AdvertisementTracker.Listener {
    
    private HttpIOHRouter _router;
    private AdvertisementTracker _trackerRemote, _trackerFarRemote;
    private BundleContext _osgi;
    private ConnectionFactory _connF;

    protected Meter _readReqMeter, _readWsMeter;
    protected Meter _parserErrorMeter, _parserChunkedMeter;
    //protected Meter _writeMsgMeter, _writeReqMeter;
    protected Meter _writeRespMeter, _writeWsMeter;
    protected Meter _webSocketsOpenMeter, _webSocketsUpgradedMeter;
    protected Map<Object, Meter> _writeByTypeMeters = new HashMap<> ();
    protected Map<Object, Meter> _readByTypeMeters = new HashMap<> ();
    
    protected HttpIOHEngine (String name, IOHServices services, HttpIOHRouterFactory routerFactory, ConnectionFactory cf){
	super (name, services);
	_router = routerFactory.newHttpIOHRouter ();
	_connF = cf;
    }

    public HttpIOHRouter getHttpIOHRouter (){ return _router;}
    
    public IOHEngine init (TcpServer server, BundleContext osgi, Dictionary<String, String> system){
	_osgi = osgi;
	server.getProperties ().put (PROP_TCP_CONNECT_SHARED, "false");
	server.getProperties ().put (PROP_TCP_LISTEN_RETRY, 5); // give 5 secs to complete
	server.getProperties ().put (PROP_TCP_LISTEN_NOTIFY, "true");
	server.getProperties ().put (PROP_UDP, "false");
	server.getProperties ().put (PROP_PROTOCOL_TEXT, "false");
	if (server.getProperties ().get (PROP_AGENT_LOAD_METER) == null)
	    server.getProperties ().put (PROP_AGENT_LOAD_METER, "resp.latency");
	super.init (server);
	_router.init (this);

	MeteringService metering = getIOHServices ().getMeteringService ();

	_readReqMeter = getIOHMeters ().createIncrementalMeter ("read.req", null);
	getIOHMeters ().addMeter (Meters.createRateMeter (metering, _readReqMeter, 1000L));
	_readWsMeter = getIOHMeters ().createIncrementalMeter ("read.tcp.ws", null);
	getIOHMeters ().addMeter (Meters.createRateMeter (metering, _readWsMeter, 1000L));
	
	_parserChunkedMeter = getIOHMeters ().createIncrementalMeter ("parser.chunked", null);
	_parserErrorMeter = getIOHMeters ().createIncrementalMeter ("parser.error", null);
	_webSocketsOpenMeter = getIOHMeters ().createIncrementalMeter ("channel.open.ws", null);
	_webSocketsUpgradedMeter = getIOHMeters ().createIncrementalMeter ("channel.upgraded.ws", null);
	
	_writeRespMeter = getIOHMeters ().createIncrementalMeter ("write.resp", null);
	getIOHMeters ().addMeter (Meters.createRateMeter (metering, _writeRespMeter, 1000L));
	_writeWsMeter = getIOHMeters ().createIncrementalMeter ("write.tcp.ws", null);
	getIOHMeters ().addMeter (Meters.createRateMeter (metering, _writeWsMeter, 1000L));
	
	String[] methods = new String[]{"OPTIONS", "GET", "HEAD", "POST", "PUT", "DELETE", "TRACE", "CONNECT", "PATCH", "OTHER"};
	for (String method : methods){
	    Meter tmp = null;
	    _readByTypeMeters.put (method, tmp = getIOHMeters ().createIncrementalMeter ("read.req."+method, _readReqMeter));
	    //getIOHMeters ().addMeter (Meters.createRateMeter (metering, tmp, 1000L));
	    //_writeByTypeMeters.put (method, tmp = getIOHMeters ().createIncrementalMeter ("write.req."+method, null));
	    //getIOHMeters ().addMeter (Meters.createRateMeter (metering, tmp, 1000L));
	}
	String[] statuses = new String[]{"100", "101", "200", "201", "202", "203", "204", "205", "206", "300", "301", "302", "303", "304", "305", "306", "307", "308", "310", "400", "401", "402", "403", "404", "405", "406", "407", "408", "409", "410", "411", "412", "413", "414", "415", "416", "417", "426", "428", "429", "431", "500", "501", "502", "503", "504", "505", "506", "509", "510", "520", "999"};
	for (String status : statuses){
	    Meter tmp = null;
	    //tmp = getIOHMeters ().createIncrementalMeter ("read.msg.resp."+status, null);
	    //_readByTypeMeters.put (status, tmp);
	    //_readByTypeMeters.put (Integer.parseInt (status), tmp);
	    //getIOHMeters ().addMeter (Meters.createRateMeter (metering, tmp, 1000L));
	    _writeByTypeMeters.put (status, tmp = getIOHMeters ().createIncrementalMeter ("write.resp."+status, _writeRespMeter));
	    _writeByTypeMeters.put (Integer.parseInt (status), tmp);
	    //getIOHMeters ().addMeter (Meters.createRateMeter (metering, tmp, 1000L));
	}

	if (useRemoteIOH ()){
	    _trackerRemote = new AdvertisementTracker (this)
		.addModuleIdFilter ("292", true)
		.addTargetGroupFilter (system.get (ConfigConstants.GROUP_NAME))
		.addFilter ("application.name", _name, true)
		.addInstanceNameFilter (system.get (ConfigConstants.INSTANCE_NAME), false); // we dont connect to ourselves - else infinite loop
	    _trackerFarRemote = new AdvertisementTracker (this)
		.addModuleIdFilter ("293", true)
		.addTargetGroupFilter (system.get (ConfigConstants.GROUP_NAME))
		.addFilter ("application.name", _name, true);
	    if (getBooleanProperty (HttpIOH.PROP_REMOTE_IMMEDIATE, false)){
		_logger.debug (this+" : tracking remote HttpIOH");
		_trackerRemote.open (_osgi);
		_trackerFarRemote.open (_osgi);
	    }
	}

	new HttpIOHStats (this).register (_osgi);
	
	return this;
    }
    @Override
    public boolean start (BundleContext osgi){
	if (super.start (osgi) == false) return false;
	// track remote http ioh
	if (useRemoteIOH () && !getBooleanProperty (HttpIOH.PROP_REMOTE_IMMEDIATE, false)){
	    _logger.debug (this+" : tracking remote HttpIOH");
	    _trackerRemote.open (_osgi);
	    _trackerFarRemote.open (_osgi);
	}
	return true;
    }
    @Override
    public void stop (){
	super.stop ();
	_trackerRemote.close ();
	_trackerFarRemote.close ();
	//TODO : unregister HttpIOHStats
    }
    //****** AdvertisementTracker.Listener ***********//
    public Object up (AdvertisementTracker tracker, InetSocketAddress addr, ServiceReference ref){
	return null;
    }
    public void down (AdvertisementTracker tracker, ServiceReference ref, Object ctx){
    }

    @Override
    protected IOHTcpServerChannel newTcpServerChannel (IOHEngine engine, TcpServer server){
	return new HttpIOHTcpServerChannel (engine, server);
    }
    
    @Override
    protected IOHTcpChannel newTcpChannel (IOHEngine engine, TcpServer server, TcpChannel channel, Map<String, Object> props){
	return new HttpIOHTcpChannel (engine, server, channel, props);
    }
    
    @Override
    protected IOHTcpClientChannel newTcpClientChannel (MuxClient agent, long connectionId, InetSocketAddress remote, Map<ReactorProvider.TcpClientOption, Object> opts){
	return new HttpIOHTcpClientChannel (agent, connectionId, remote, opts);
    }

    @Override
    public void initMuxClient (MuxClient agent){
	super.initMuxClient (agent);
	if (agent.isLocalAgent ()){
	    // we make an alias to discriminate the jetty agent and the pxlet agent : this was useful for http2 termination - may still be convenient
	    // in case we have both co-located
	    String protocol = agent.getApplicationParam ("agent.protocol", null);
	    if (protocol != null) agent.aliases ().add ("local."+protocol);
	}
	agent.getIOHMeters ().createAbsoluteMeter ("resp.latency");
	_router.initMuxClient (agent);
    }
    
    @Override
    public boolean sendMuxData(MuxClient agent, MuxHeader header, boolean copy, ByteBuffer ... buf) {
	if (header.getVersion () == 0){
	    switch (header.getFlags ()){
	    case 0x4A:
		// a channel is moved to websocket mode
		int sockId = header.getChannelId ();
		if (_logger.isDebugEnabled ())
		    _logger.debug (agent+" : upgrade websocket : "+sockId);
		IOHChannel channel = agent.getTcpChannel (sockId);
		if (channel == null) return false;
		((HttpIOHTcpChannel)channel).upgradeWebSocket (agent);
		_webSocketsUpgradedMeter.inc (1);
		return true;
	    case 0x4C:
		// two channels are piped (CONNECT use case)
		int clientSocketId = (int) header.getSessionId ();
		int serverSocketId = header.getChannelId ();
		if (_logger.isDebugEnabled ())
		    _logger.debug (agent+" : connect sockets : client="+clientSocketId+" : server="+serverSocketId);
		IOHChannel clientChannel = agent.getTcpChannel (clientSocketId);
		IOHChannel serverChannel = agent.getTcpChannel (serverSocketId);
		if (clientChannel == null){
		    if (serverChannel != null){
			if (_logger.isInfoEnabled ())
			    _logger.info (agent+" : CONNECT sockets : cannot find client socket : "+clientSocketId+" : closing server socket : "+serverChannel);
			serverChannel.close ();
		    } else {
			if (_logger.isInfoEnabled ())
			    _logger.info (agent+" : CONNECT sockets : cannot find client socket : "+clientSocketId+" : cannot find server socket : "+serverSocketId);
		    }
		    return true;
		    
		}
		if (serverChannel == null){
		    if (_logger.isInfoEnabled ())
			_logger.info (agent+" : CONNECT sockets : cannot find server socket : "+serverSocketId+" : closing client socket : "+clientChannel);
		    clientChannel.close ();
		    return true;
		}
		((HttpIOHTcpChannel)clientChannel).connect (serverChannel);
		((HttpIOHTcpClientChannel)serverChannel).connect (clientChannel);
		return true;
	    }
	}
	return false;
    }

    private static class HttpIOHTcpServerChannel extends IOHTcpServerChannel {
	protected boolean _isServerMode, _isProxyMode;
	protected List<Pattern> _acceptPatterns;
	protected ConnectionConfig _http2Config;
	
	protected HttpIOHTcpServerChannel (IOHEngine engine, TcpServer server){
	    super (engine, server);
	    _isProxyMode = IOHEngine.getBooleanProperty (HttpIOH.PROP_MODE_PROXY, server.getProperties (), false);
	    _isServerMode = IOHEngine.getBooleanProperty (HttpIOH.PROP_MODE_SERVER, server.getProperties (), false);
	    List<String> acceptList = Property.getStringListProperty ("http.ioh.URL.accept", server.getProperties ());
	    if (acceptList != null){
		for (String accept : acceptList){
		    Pattern p = Pattern.compile(accept);
		    if (_acceptPatterns == null) _acceptPatterns = new ArrayList<> ();
		    _acceptPatterns.add (p);
		}
	    }
	    acceptList = Property.getStringListProperty ("http.ioh.URL.accept", engine.getProperties ());
	    if (acceptList != null){
		for (String accept : acceptList){
		    Pattern p = Pattern.compile(accept);
		    if (_acceptPatterns == null) _acceptPatterns = new ArrayList<> ();
		    _acceptPatterns.add (p);
		}
	    }

	    Object s = engine.getProperties ().get (IOHEngine.PROP_TCP_SEND_BUFFER);
	    if (s != null &&
		server.getProperties ().get (ConnectionConfig.PROP_CONN_WRITE_BUFFER) == null)
		server.getProperties ().put (ConnectionConfig.PROP_CONN_WRITE_BUFFER, s); // propagate to http2 config
	    Settings settings = new Settings ().load (server.getProperties ());
	    _http2Config = new ConnectionConfig (settings, _logger)
		.priorKnowledge (false)
		.load (true, server.getProperties ());
	}
    }

    private static class HttpIOHTcpClientChannel extends IOHTcpClientChannel {
	protected IOHChannel _connected;
	protected HttpIOHTcpClientChannel (MuxClient agent, long connectionId, InetSocketAddress dest, Map<ReactorProvider.TcpClientOption, Object> opts){
	    super (agent, connectionId, dest, opts);
	}
	protected void connect (IOHChannel other){
	    _connected = other;
	}
	@Override
	public void connectionClosed (){
	    if (_connected != null) _connected.close ();
	    super.connectionClosed ();
	}
	@Override
	public int messageReceived(TcpChannel cnx,
				   ByteBuffer buff){
	    if (_connected == null){
		return super.messageReceived (cnx, buff);
	    }
	    if (disabled (buff))
		return 0;

	    logReceived (null, buff);

	    _readMeter.inc (buff.remaining ());

	    if (_connected.sendOut (null, null, true, true, buff) == false){
		buff.limit (buff.position ());
		close ();
	    }
	    return 0;
	}
    }
    
    protected static class HttpIOHTcpChannel extends IOHTcpChannel implements Http2RequestListener {
	private HttpIOHChannelImpl _httpChannel;
	protected HttpParser _parser = new HttpParser ();
	protected Map<Object, Boolean> _notifiedAgentsMap = new HashMap<> ();
	protected boolean _isWebSocket, _isHttp2;
	protected IOHChannel _connected;
	protected long _sentRequestTimestamp = Long.MAX_VALUE;
	protected HttpIOHTcpServerChannel _serverChannel;
	protected Connection _http2Conn;
	protected Map<String, Object> _props;
	
	private HttpIOHTcpChannel (IOHEngine engine, TcpServer server, TcpChannel channel, Map<String, Object> props){
	    super (engine, channel, props);
	    _props = props;
	    _httpChannel = new HttpIOHChannelImpl ((HttpIOHEngine) engine, this, true);
	    _serverChannel = server.attachment ();
	    _httpChannel.setAcceptURLPatterns (_serverChannel._acceptPatterns);
	    if (IOHEngine.getBooleanProperty (ConnectionConfig.PROP_CONN_PRIOR_KNOWLEDGE, props, false))
		initHttp2 (false);
	}
	private void initHttp2 (boolean skipPRI){
	    ConnectionConfig cc = _serverChannel._http2Config.copy();
	    cc.writeExecutor(_engine.createQueueExecutor());
	    _http2Conn = ((HttpIOHEngine) _engine)._connF.newServerConnection (cc,
									       (TcpChannel) _channel,
									       this);
	    _http2Conn.skipPRI (skipPRI).init ();
	    _isHttp2 = true;
	}
	protected void upgradeWebSocket (final MuxClient agent){
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			if (_isWebSocket) return; // may have been done spontaneously by parser
			((HttpIOHEngine)_engine)._webSocketsOpenMeter.inc (1);
			_isWebSocket = true;
			_httpChannel.attachAgent (agent);
		    }};
	    schedule (r);
	}
	protected void connect (IOHChannel other){
	    _connected = other;
	}
	@Override
	public boolean agentConnected (MuxClient agent, MuxClientState state){
	    return super.agentConnected (agent, state) && _httpChannel.agentConnected (agent, state);
	}
	@Override
	public boolean agentClosed (MuxClient agent){
	    return super.agentClosed (agent) && _httpChannel.agentClosed (agent);
	}
	@Override
	public boolean agentStopped (MuxClient agent){
	    return super.agentStopped (agent) && _httpChannel.agentStopped (agent);
	}
	@Override
	public boolean agentUnStopped (MuxClient agent){
	    return super.agentUnStopped (agent) && _httpChannel.agentUnStopped (agent);
	}
	@Override
	protected void notifyOpenToAgent (MuxClient agent, long connectionId){
	}
	@Override
	protected void notifyCloseToAgent (MuxClient agent){
	    if (_notifiedAgentsMap.remove (agent) != null)
		super.notifyCloseToAgent (agent);
	}
	private void notifyOpenToAgentNow (MuxClient agent){
	    if (_notifiedAgentsMap.get (agent) == null){
		super.notifyOpenToAgent (agent, 0L);
		_notifiedAgentsMap.put (agent, Boolean.TRUE);
	    }
	}
	@Override
	public void connectionClosed (){
	    if (_isWebSocket) ((HttpIOHEngine)_engine)._webSocketsOpenMeter.inc (-1);
	    if (_connected != null) _connected.close ();
	    if (_http2Conn != null) _http2Conn.closed ();
	    super.connectionClosed ();
	}
	@Override
	public void receiveTimeout (){
	    if (_isHttp2){
		_http2Conn.receiveTimeout ();
	    } else {
		super.receiveTimeout ();
	    }
	}
	@Override
	public int messageReceived(TcpChannel cnx,
				   ByteBuffer buff){
	    if (disabled (buff))
		return 0;

	    _readMeter.inc (buff.remaining ());

	    while (true){
		if (_connected != null){
		    if (_connected.sendOut (null, null, true, true, buff) == false){
			buff.limit (buff.position ());
			close ();
		    }
		    return 0;
		}
		if (_isWebSocket){
		    if (_logger.isDebugEnabled ()) _logger.debug (this+" : RECEIVED WebSocket data : "+buff.remaining ());
		    ((HttpIOHEngine)_engine)._readWsMeter.inc (buff.remaining ());
		    if (_httpChannel.handleWebSocket (buff) == false){
			buff.position (buff.limit ()); // empty by precaution
			close ();
		    }
		    return 0;
		}

		if (_isHttp2){
		    _http2Conn.received (buff);
		    return 0;
		}
		
		HttpMessageImpl msg = null;
		try{
		    msg = _parser.parseMessage (buff);
		    if (msg == null)
			return 0;
		    if (msg.isRequest () == false)
			throw new IOException ("Received a response : only requests are expected");
		    if (msg.isFirst ()){
			boolean checkURL = false;
			if (_serverChannel._isServerMode)
			    checkURL = msg.getURL ().startsWith ("/");
			if (!checkURL && _serverChannel._isProxyMode){
			    String url = msg.getURL ();
			    checkURL = url.startsWith ("http") &&
				(url.regionMatches (false, 4, "://", 0, 3) || url.regionMatches (false, 4, "s://", 0, 4));
			}
			if (!checkURL){
			    if (_serverChannel._isServerMode && _serverChannel._isProxyMode) throw new IOException ("request URL is not acceptable : "+msg.getURL ());
			    if (_serverChannel._isServerMode) throw new IOException ("request URL is not in server mode : "+msg.getURL ());
			    if (_serverChannel._isProxyMode) throw new IOException ("request URL is not in proxy mode : "+msg.getURL ());
			}
		    }
		} catch(HttpParser.Http2Exception he){ // we were not upgraded, but we receive an HTTP2 PRI
		    if (IOHEngine.getBooleanProperty ("http2.enabled", _props, true) == false){
			if (_logger.isInfoEnabled ()) _logger.info (this+" : read HTTP2 request : upgrade http2 forbidden");
			((HttpIOHEngine)_engine)._parserErrorMeter.inc (1);
			buff.position (buff.limit ());
			close ();
			return 0;
		    }
		    if (_logger.isInfoEnabled ()) _logger.info (this+" : read HTTP2 request : upgrade http2 spontaneously");
		    initHttp2 (true);
		    _http2Conn.received (buff);
		    return 0;
		} catch (Throwable t){
		    if (_logger.isDebugEnabled ()) _logger.debug (this+" : parsing exception", t.getCause () != null ? t.getCause () : t);
		    ((HttpIOHEngine)_engine)._parserErrorMeter.inc (1);
		    buff.position (buff.limit ());
		    close ();
		    return 0;
		}
		if (msg.isFirst ()){
		    if (msg.isChunked ()) ((HttpIOHEngine)_engine)._parserChunkedMeter.inc (1);
		}
		if (_logger.isDebugEnabled ()) _logger.debug (this+" : RECEIVED :\n["+msg+"]");
		if (msg.isLast ()) _sentRequestTimestamp = System.currentTimeMillis ();
		if (_httpChannel.handleMessage (msg) == false){
		    buff.position (buff.limit ()); // empty by precaution
		    close ();
		    return 0;
		}
		msg.setHasMore ();
	    }
	}
	@Override
	public boolean sendAgent (MuxClient agent, InetSocketAddress from, boolean checkBuffer, long sessionId, boolean copy, ByteBuffer... buffs){
	    notifyOpenToAgentNow (agent);
	    return super.sendAgent (agent, from, checkBuffer, sessionId, copy, buffs);
	}
	@Override
	public boolean sendOut (MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	    if (_isWebSocket){
		int size = ByteBufferUtils.remaining (buffs);
		if (super.sendOut (agent, to, checkBuffer, copy, buffs)){
		    ((HttpIOHEngine)_engine)._writeWsMeter.inc (size);
		    return true;
		} else {
		    return false;
		}
	    } else {
		if (agent != null){
		    long now = System.currentTimeMillis ();
		    long elapsed = now - _sentRequestTimestamp;
		    if (elapsed >= 0) { // by precaution avoid negative value - in case of pipeline or early response before end of request body
			HttpIOHRouter.AgentContext ctx = agent.getContext ();
			Meter latencyMeter = ctx._latencyMeter;
			long value = latencyMeter.getValue ();
			long newValue = value + (elapsed >> 3) - (value >> 3);
			latencyMeter.set (newValue);
		    }
		}
		String status = HttpParser.getStatus (buffs);
		if (super.sendOut (agent, to, checkBuffer, copy, buffs)){
		    _httpChannel.incWriteResp (status);
		    return true;
		} else {
		    //_writeDroppedRespMeter.inc (1);
		    return false;
		}
	    }
	}

	//////////////////// Http2RequestListener
	private Map<Integer, Http2Message> _http2Msgs = new ConcurrentHashMap<> ();
	@Override
	public void newRequest (RequestContext rc){
	    Http2Message msg = new Http2Message (this, rc);
	    rc.attach (msg);
	}
	@Override
	public void recvReqMethod (RequestContext rc, String method){
	    ((Http2Message) rc.attachment ()).recvReqMethod (method);
	}
	@Override
	public void recvReqPath (RequestContext rc, String path){
	    ((Http2Message) rc.attachment ()).recvReqPath (path);
	}
	@Override
	public void recvReqScheme (RequestContext rc, String scheme){
	    ((Http2Message) rc.attachment ()).recvReqScheme (scheme);
	}
	@Override
	public void recvReqAuthority (RequestContext rc, String auth){
	    ((Http2Message) rc.attachment ()).recvReqAuthority (auth);
	}
	@Override
	public void recvReqHeader (RequestContext rc, String name, String value){
	    ((Http2Message) rc.attachment ()).recvReqHeader (name, value);
	}
	@Override
	public void recvReqHeaders (RequestContext rc, boolean done){
	    Http2Message msg = ((Http2Message) rc.attachment ());
	    if (done) msg.done ();
	    triggerReq (msg);
	    if (!done) msg.setHasMore ();
	}
	@Override
	public void recvReqData (RequestContext rc, ByteBuffer data, boolean done){
	    Http2Message msg = ((Http2Message) rc.attachment ());
	    msg.recvReqData (data);
	    if (done) msg.done ();
	    triggerReq (msg);
	}
	private void triggerReq (Http2Message msg){
	    if (_httpChannel.handleMessage (msg) == false){
		//TODO
		return;
	    }
	}
	@Override
	public void abortRequest (RequestContext rc){
	    Http2Message msg = ((Http2Message) rc.attachment ());
	    msg.aborted ();
	}
    }
    
    protected static class HttpIOHChannelImpl implements HttpIOHChannel {
	protected HttpIOHEngine _engine;
	protected boolean _isRemote;
	protected Map<Object, MuxClient> _agentsMap = new HashMap<> ();
	protected IOHChannel _channel;
	protected boolean _incoming;
	protected MuxClient  _agent;
	protected List<Pattern> _acceptPatterns;
	
	protected HttpIOHChannelImpl (HttpIOHEngine engine, IOHChannel channel, boolean incoming){
	    _engine = engine;
	    _channel = channel;
	    _incoming = incoming;
	}
	public String toString (){ return _channel.toString ();}
	protected void setAcceptURLPatterns (List<Pattern> patterns){
	    _acceptPatterns = patterns;
	}
	protected HttpIOHChannelImpl setRemoteIOHEngine (){
	    _isRemote = true;
	    return this;
	}
	public boolean agentConnected (MuxClient agent, MuxClientState state){
	    for (String alias : agent.aliases ())
		_agentsMap.put (alias, agent);
	    _engine._router.agentConnected (this, agent, state);
	    return true;
	}
	public boolean agentClosed (MuxClient agent){
	    for (String alias : agent.aliases ())
		_agentsMap.remove (alias);
	    _engine._router.agentClosed (this, agent);
	    if (agent == agentAttached ()){
		// we were in the middle of a transaction
		attachAgent (null);
		close (null);
	    }
	    return true;
	}
	public boolean agentStopped (MuxClient agent){
	    _engine._router.agentStopped (this, agent);
	    return true;
	}
	public boolean agentUnStopped (MuxClient agent){
	    _engine._router.agentUnStopped (this, agent);
	    return true;
	}
	
	protected boolean handleMessage (HttpMessage msg){
	    try{
		// always a request
		if (msg.isFirst ()){
		    Meter meter = _engine._readByTypeMeters.get (msg.getMethod ());
                    if (meter != null){
                        meter.inc (1);
                    } else {
                        if (_channel.getLogger ().isInfoEnabled ())
                            _channel.getLogger ().info (_channel+" : received request with unknown Method : "+msg.getMethod ());
                        _engine._readByTypeMeters.get ("OTHER").inc (1);
                    }
		    
		    if (_acceptPatterns != null){ // NOTE THAT Http2Termination does not set the _acceptPatterns (TODO ?)
			boolean ok = false;
			for (Pattern pattern : _acceptPatterns){
			    if (pattern.matcher (msg.getURL ()).find ()){
				ok = true;
				break;
			    }
			}
			if (!ok){
			    _engine._router.handleError (this, msg, 404, "URL not accepted");
			    return true;
			}
		    }

		    if (!(msg instanceof Http2Message)){
			// TODO for Http2
			HttpMessage.Header h = msg.getHeader("expect");
			if (h != null){
			    String expect = h.getValue ();
			    if (expect != null && expect.contains("100-continue")) {
				msg.removeHeader (h);
				_channel.sendOut (null, null, true, true, ByteBuffer.wrap (msg.getVersion () == 0 ? HttpUtils.HTTP10_100 : HttpUtils.HTTP11_100));
			    }
			}
		    }
		    
		    _engine._router.handleRequestHeaders (this, msg);
		} else {
		    _engine._router.handleRequestBody (this, msg, msg.isLast ());
		}
	    } catch (Throwable t){
		_channel.getLogger ().warn (_channel+" : exception while routing msg : ["+msg+"]", t);
		return false;
	    }
	    return true;
	}
	protected boolean handleWebSocket (ByteBuffer buff){
	    try{
		return _engine._router.handleWebSocketData (this, buff);
	    } catch (Throwable t){
		_channel.getLogger ().warn (_channel+" : exception while routing websocket data", t);
		return false;
	    }
	}
	protected void incWriteResp (String status){
	    if (status == null) return; // this is not the beginnning of a response
	    Meter meter = _engine._writeByTypeMeters.get (status);
	    if (meter != null){
		meter.inc (1);
	    } else {
		if (_channel.getLogger ().isInfoEnabled ())
		    _channel.getLogger ().info (_channel+" : sent response with unknown Status : "+status);
		_engine._writeByTypeMeters.get ("999").inc (1);
	    }
	}
	public boolean incoming (){ return _incoming;}
	public IOHChannel getIOHChannel (){ return _channel;}
	public <T extends AsyncChannel> T getChannel (){ return _channel.getChannel ();}
	public Logger getLogger (){ return _channel.getLogger ();}
	public PlatformExecutor getPlatformExecutor (){ return _channel.getPlatformExecutor ();}
	public boolean sendAgent (IOHEngine.MuxClient agent, HttpMessage msg){
	    if (agent == null) return false;
	    if (msg instanceof Http2Message){
		return ((Http2Message)msg).sendAgent (agent);
	    }
	    ByteBuffer[] buffs = msg.toByteBuffers ();
	    msg.setAgent (agent);
	    return _channel.sendAgent (agent, null, false, 0L, false, buffs);
	}
	public boolean sendOut (HttpMessage msg, boolean checkBuffer, ByteBuffer... data){
	    if (msg instanceof Http2Message)
		return ((Http2Message)msg).sendOut (null, null, checkBuffer, false, data);
	    else
		return _channel.sendOut (null, null, checkBuffer, false, data);
	}
	public MuxClient getAgent (String instance){ return _agentsMap.get (instance);}
	public Map<Object, IOHEngine.MuxClient> getAgents (){ return _agentsMap;}
	public IOHEngine.MuxClient pickAgent (Object preferenceHint){ return _channel.getAgents ().pick (preferenceHint);}
	public void attach (Object attachment){ _channel.attach (attachment);}
	public <T> T attachment (){ return (T) _channel.attachment ();}
	public boolean isRemoteIOHEngine (){ return _isRemote;}
	public void close (HttpMessage msg){ // msg can be null for Websocket
	    if (msg != null && msg instanceof Http2Message)
		((Http2Message)msg).close ();
	    else
		_channel.close ();
	}
	public void attachAgent (MuxClient agent){ // used for websocket
	    _agent = agent;
	}
	public MuxClient agentAttached (){ return _agent;}
    }
}
