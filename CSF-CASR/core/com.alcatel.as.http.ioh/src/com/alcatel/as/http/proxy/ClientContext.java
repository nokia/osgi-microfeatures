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

public class ClientContext implements TcpChannelListener, DNSHelper.Listener<RecordAddress>, Http2RequestListener, HttpMessageFilter {

    public static final byte[] X_FORWARDED_FOR_B = HttpParser.getUTF8 ("X-Forwarded-For");
    public static final byte[] X_FORWARDED_PORT_B = HttpParser.getUTF8 ("X-Forwarded-Port");
    public static final byte[] X_FORWARDED_PROTO_B = HttpParser.getUTF8 ("X-Forwarded-Proto");
    
    protected HttpProxy.Processor _proc;
    protected TcpChannel _clientChannel, _serverChannel;
    protected HttpProxyPlugin _plugin;
    protected Logger _logger;
    protected Map<String, Object> _props;
    protected PlatformExecutor _exec;
    protected Meters _meters;
    protected boolean _ignoreData;
    protected HttpParser _parser;
    protected String _toString;
    protected State _state;
    protected ReactorProvider _provider;
    protected Reactor _reactor;
    protected boolean _tls, _nextProxy1;
    protected HttpProxy.ProxyConf _pxConf;
    protected Connection _http2Conn;
    protected HttpClient _http2Client;
    protected boolean _isHttp2, _isHttp1Http2;
    protected Http1ToHttp2 _h1Toh2;
    
    public static final String RAW_PATH_PSEUDOHEADER = "__raw_path";

    
    public ClientContext (HttpProxy.Processor proc, TcpChannel channel, HttpProxyPlugin plugin, Meters meters, Map<String, Object> props){
	_proc = proc;
	_clientChannel = channel;
	_plugin = plugin;
	_meters = meters;
	_props = props;
	_exec = (PlatformExecutor) _props.get (Server.PROP_READ_EXECUTOR);
	_provider = (ReactorProvider) _props.get ("system.reactor.provider");
	try{
	    _reactor = _provider.getDefaultReactor ();
	}catch(Exception e){
	    // cannot happen
	}
	_pxConf = proc.getProxyConf (props);
	_nextProxy1 = _pxConf._proxy1 != null;
	_logger = _pxConf._logger;
	_meters.getOpenAcceptedChannelsMeter ().inc (1);
	_toString = new StringBuilder ().append ("Client[").append (channel.getRemoteAddress ()).append (']').toString ();
	_isHttp1Http2 = _pxConf._h1h2Gateway;
    }
    public String toString (){ return _toString;}

    public void start (){
	if (_pxConf._h2Config.priorKnowledge ()){
	    initHttp2 (false);
	} else {
	    _parser = new HttpParser ();
	    if (_isHttp1Http2){
		_http2Client = _proc.acquireH2Client (_pxConf);
		_parser.filter (_h1Toh2 = new Http1ToHttp2 (this));
	    } else 
		_parser.filter (this);
	}
	_state = STATE_INIT.enter (this, null);
	_clientChannel.enableReading ();
    }

    /*********** HttpMessageFilter ******/

    @Override
    public void init (HttpMessage msg){ msg.attach (_pxConf.newClientHeadersContext ());}

    @Override
    public boolean header (HttpMessage msg, String name, java.util.function.Supplier<String> value){
	switch (name){
	case "x-forwarded-for":
	case "x-forwarded-proto":
	case "x-forwarded-port":
	    if (_pxConf._xForwardedRemove)
		return false;
	}
	return _pxConf.handleClientHeader (name, value, (HttpProxy.HeadersContext)(msg.attachment ()));
    }

    public void headers (HttpMessage msg){
	_pxConf.endClientHeaders ((name, value)->{msg.addHeader (HttpParser.getUTF8(name), value);},
				  (HttpProxy.HeadersContext)(msg.attachment ()));
	if (_pxConf._xForwardedAdd){
	    msg.addHeader (X_FORWARDED_FOR_B, _clientChannel.getRemoteAddress ().getAddress ().getHostAddress ());
	    msg.addHeader (X_FORWARDED_PORT_B, String.valueOf (_clientChannel.getLocalAddress ().getPort ()));
	    msg.addHeader (X_FORWARDED_PROTO_B, _clientChannel.isSecure () ? "https" : "http");
	}
	msg.attach (null);
    }

    /************************************************
     ** TcpChannelListener for client connections **
     ************************************************/
	
    public int messageReceived (TcpChannel channel, ByteBuffer data){
	if (_ignoreData){
	    data.position (data.limit ());
	    return 0;
	}
	if (_isHttp2){
	    http2Received (data);
	    return 0;
	}
	if (_tls){
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : binary messageReceived : size="+data.remaining ());
	    _state = _state.clientMessageReceived (this, data);
	    // we empty the data here so states dont have to do it in case of error
	    data.position (data.limit ());
	    return 0;
	}

	int init = data.position (); // in case of error to log the buffer
	try{
	    HttpMessage req;
	    while ((req = _parser.parseMessage (data)) != null){
		if (req.isRequest () == false)
		    throw new java.io.IOException ("Received a response : only requests are expected");
		if (_logger.isDebugEnabled ())
		    _logger.debug (this+" : client messageReceived : isFirst="+req.isFirst ()+" isLast="+req.isLast ());
		if (req.isFirst ()){
		    if (_logger.isDebugEnabled ())
			_logger.debug (this+" : messageReceived : URL : "+req.getURL ());
		    _meters.getReadReqMeter (req.getMethod ()).inc (1);
		    if (_isHttp1Http2){
			_h1Toh2.headers (req);
			if (req.getBody () != null) _h1Toh2.data (req);
			((HttpMessageImpl)req).setHasMore (); // required by the parser - so next read does not return the req if no new data
			continue;
		    } else {
			headers (req);
		    }
		} else {
		    if (_isHttp1Http2){
			_h1Toh2.data (req);
			continue;
		    }
		}
		_state = _state.clientMessageReceived (this, req);
		((HttpMessageImpl)req).setHasMore (); // required by the parser - so next read does not return the req if no new data
	    }
	}catch(HttpParser.Http2Exception h2e){
	    if (!_pxConf._h2Config.enabled ()){
		if (_logger.isInfoEnabled ()) _logger.info (this+" : read HTTP2 request : upgrade http2 forbidden");
		data.position (data.limit ());
		_meters.getParserErrorMeter ().inc (1);
		closeClient (false);
		return 0;
	    }
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : read HTTP2 request : upgrade http2 spontaneously");
	    initHttp2 (true);
	    _http2Conn.received (data);
	}catch(Exception e){
	    if (_logger.isInfoEnabled ()){
		data.position (init);
		String s = ByteBufferUtils.toUTF8String (true, data);
		_logger.info (this+" : exception while parsing\n"+s, e);
	    }
	    data.position (data.limit ());
	    _meters.getParserErrorMeter ().inc (1);
	    closeClient (true);
	}
	return 0;
    }
    
    public void receiveTimeout (TcpChannel channel){
	if (_isHttp2){
	    _http2Conn.receiveTimeout ();
	    return;
	}
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : client receiveTimeout : closing");
	channel.close ();
    }

    public void writeBlocked (TcpChannel channel){}
    public void writeUnblocked (TcpChannel channel){}

    public void connectionClosed(TcpChannel cnx){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : clientClosed");
	_meters.getOpenAcceptedChannelsMeter ().inc (-1);
	_meters.getClosedAcceptedChannelsMeter ().inc (1);
	_state = _state.clientClosed (this);
	if (_isHttp2){
	    _http2Conn.closed ();
	    _proc.releaseH2Client (_pxConf, _http2Client);
	} else if (_isHttp1Http2){
	    _h1Toh2.abort ();
	    _proc.releaseH2Client (_pxConf, _http2Client);
	}
    }

    private void initHttp2 (boolean skipPRI){
	_http2Conn = _proc.newHttp2Connection (_pxConf._h2Config, // no need to clone it since writeExecutor is not set in it
					       (TcpChannel) _clientChannel,
					       this);
	if (_isHttp1Http2){
	    // we already have a client - but lets disable the gw flag
	    _isHttp1Http2 = false;
	    _h1Toh2.abort (); // should not do anything
	    _h1Toh2 = null;
	} else {
	    _http2Client = _proc.acquireH2Client (_pxConf);
	}
	_http2Conn.skipPRI (skipPRI).init ();
	_isHttp2 = true;
    }

    private void http2Received (ByteBuffer data){
	_http2Conn.received (data);
    }

    /************************************************
     ** TcpClientChannelListener for server connections **
     ************************************************/

    private TcpClientChannelListener _serverListener = new TcpClientChannelListener (){

	    public void connectionEstablished(TcpChannel cnx){
		if (_logger.isDebugEnabled ())
		    _logger.debug (ClientContext.this+" : server connectionEstablished");
		_serverChannel = cnx;
		_meters.getOpenConnectedChannelsMeter ().inc (1);
		_state = _state.serverConnected (ClientContext.this);
	    }
	    public void connectionFailed(TcpChannel cnx, java.lang.Throwable error){
		if (_logger.isDebugEnabled ())
		    _logger.debug (ClientContext.this+" : server connectionFailed");
		_meters.getFailedConnectedChannelsMeter ().inc (1);
		_state = _state.serverFailed (ClientContext.this);
	    }	
	    public int messageReceived (TcpChannel channel, ByteBuffer data){
		_state = _state.serverMessageReceived (ClientContext.this, data);
		// we empty the data here so states dont have to do it in case of error
		data.position (data.limit ());
		return 0;
	    }
    	    public void receiveTimeout (TcpChannel channel){
		if (_logger.isDebugEnabled ())
		    _logger.debug (ClientContext.this+" : server receiveTimeout");
		channel.close ();
	    }

	    public void writeBlocked (TcpChannel channel){}
	    public void writeUnblocked (TcpChannel channel){}

	    public void connectionClosed(TcpChannel cnx){
		if (_logger.isDebugEnabled ())
		    _logger.debug (ClientContext.this+" : serverClosed");
		_meters.getOpenConnectedChannelsMeter ().inc (-1);
		_meters.getClosedConnectedChannelsMeter ().inc (1);
		if (cnx.getRemoteAddress ().equals (_serverAddress))
		    _state = _state.serverClosed (ClientContext.this);
		// else we closed an old server	    
	    }
	};

    /************************************************
     *************** Actions  ***********************
     ************************************************/

    private HttpMessage _req;
    private void storeReq (HttpMessage message){ _req = message;}
    private HttpMessage releaseReq (){ try{return _req;}finally{_req = null;}}
    private String _host;
    private int _port = -1;
    private InetSocketAddress _serverAddress;
    // return 0 : all same , 1 : same host, new port , 2 : new host, new port
    private int storeDestination (String host, int port){
	boolean sameHost = host.equals (_host);
	boolean samePort = (port == _port);
	_host = host;
	_port = port;
	return sameHost ? (samePort ? 0 : 1) : 2 ;
    }
    // return -1 : failed, 1 : ok same dest, 2 : ok, but new dest
    private int parseDestination (HttpMessage req){
	String method = req.getMethod ();
	String url = req.getURL ();
	if (_nextProxy1){
	    _serverAddress = _pxConf._proxy1._address;
	    if (method.equals ("CONNECT")){
		_tls = true;
		if (_logger.isDebugEnabled ())
		    _logger.debug (this+" : CONNECT "+url);
	    }
	    return 1;
	}
	String host = null;
	int port = 0;
	if (method.equals ("CONNECT")){
	    _tls = true;
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : CONNECT "+url);
	} else {    
	    if (url.startsWith ("/")){
		if (_logger.isDebugEnabled ())
		    _logger.debug (this+" : invalid url format : "+url);
		return -1;
	    }
	}
	Object[] hostPort = HttpParser.parseURL (url, !_tls);
	if (hostPort == null) return -1;
	host = (String) hostPort[0];
	port = (Integer) hostPort[1];
	// rewrite first line if not CONNECT
	if (!_tls){
	    String newUrl = null;
	    int indexPath = (Integer) hostPort[2];
	    if (indexPath == url.length ()) newUrl = "/";
	    else newUrl = url.substring (indexPath);
	    String fl = new StringBuilder ()
		.append (req.getMethod ())
		.append (' ')
		.append (newUrl)
		.append (' ')
		.append (req.getVersion () == 0 ? "HTTP/1.0" : "HTTP/1.1")
		.append ("\r\n")
		.toString ();
	    byte[] flb = HttpParser.getUTF8 (fl);
	    ByteBuffer bb = ByteBuffer.wrap (flb);
	    ((HttpMessageImpl)req).setFirstLine (bb);
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : setFirstLine : "+fl);
	}
	int stored = storeDestination (host, port);
	switch (stored){
	case 0: return 1; // same _serverAddress
	case 1: // new _serverAddress with new port --> no need to resolve
	    try{
		_serverAddress = new InetSocketAddress (_serverAddress.getAddress (), port);
	    }catch(Exception e){} // cannot happen
	    return 2;
	case 2:	// new _serverAddress
	    char c = host.charAt (0);
	    if (c < '0' || c > '9'){
		_serverAddress = null; // will need resolve
	    } else {
		try{
		    _serverAddress = new InetSocketAddress (InetAddress.getByName (host), port);
		}catch(Exception e){
		    if (_logger.isDebugEnabled ())
			_logger.debug (this+" : invalid destination : "+url, e);
		    return -1;
		}
	    }
	    return 2;
	}
	return -1; // unreachable
    }
    
    private void resolve (){
	DNSHelper.getHostByName (_host, this);
    }
    private int rnd = 0;
    public void requestCompleted(String query, List<RecordAddress> records){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : resolved "+_host+" : "+records);
	if (records.size () == 0){
	    if (_logger.isInfoEnabled ())
		_logger.info (this+" : DNS resolution failed for : "+_host);
	    _meters.getFailedDNSMeter ().inc (1);
	    _state = _state.serverResolved (this);
	    return;
	}
	int startIndex = (rnd++) & 0xFFFFFF;
	for (int i = 0; i<records.size (); i++){
	    RecordAddress address = records.get (startIndex % records.size ());
	    try{
		_serverAddress = new InetSocketAddress (InetAddress.getByName (address.getAddress ()), _port);
		break;
	    }catch(Exception e){
		// try next record
	    }
	    startIndex++;
	}
	if (_serverAddress == null){
	    if (_logger.isInfoEnabled ())
		_logger.info (this+" : DNS resolution failed for : "+_host);
	    _meters.getFailedDNSMeter ().inc (1);
	} else {
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : DNS resolution  for : "+_host+" : "+_serverAddress);
	}
	_state = _state.serverResolved (this);
    }
    private void connect (){
	Map<ReactorProvider.TcpClientOption, Object> props = new HashMap<> ();
	props.put (ReactorProvider.TcpClientOption.INPUT_EXECUTOR, _exec);
	props.put (ReactorProvider.TcpClientOption.USE_DIRECT_BUFFER, true);
	props.put (ReactorProvider.TcpClientOption.TIMEOUT, _pxConf._connectTimeout);
	props.put (ReactorProvider.TcpClientOption.TCP_NO_DELAY, true);
	_pxConf.setFromAddress (from -> {props.put (ReactorProvider.TcpClientOption.FROM_ADDR, new InetSocketAddress (from, 0));});
	if (_nextProxy1){
	    // we use the next proxy
	    if (_pxConf._proxy1._security != null){
		props.put (ReactorProvider.TcpClientOption.SECURE, Boolean.TRUE);
		props.put (ReactorProvider.TcpClientOption.SECURITY, _pxConf._proxy1._security);
	    }
	}
	_provider.tcpConnect (_reactor, _serverAddress, _serverListener, props);
    }
    public void closeClient (boolean failedReq){
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : close client");
	if (failedReq) _meters.getFailedReqsMeter ().inc (1);
	_ignoreData = true; // disable future reads until closed is called back
	if (_h1Toh2 != null) _h1Toh2.abort (); // idempotent - also called in connectionClosed
	_clientChannel.close ();
    }
    private void closeServer (){
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : close server");
	_serverChannel.close ();
    }

    /************************************************
     *************** State machine ******************
     ************************************************/

    private static class State {
	protected State (){
	}
	protected State enter (ClientContext ctx, State from, Object... msg){
	    if (ctx._logger.isDebugEnabled ()){
		String s = "";
		for (Object o : msg) s = s+o.toString ();
		ctx._logger.debug (ctx+" : change state from "+from+" to "+this+" : "+s);
	    }
	    return this;
	}
	protected State clientMessageReceived (ClientContext ctx, ByteBuffer data){ return this;}
	protected State clientMessageReceived (ClientContext ctx, HttpMessage req){ return this;}
	protected State serverMessageReceived (ClientContext ctx, ByteBuffer data){ return this;}
	protected State clientClosed (ClientContext ctx){ return this;}
	protected State serverResolved (ClientContext ctx){ return this;}
	protected State serverConnected (ClientContext ctx){ return this;}
	protected State serverClosed (ClientContext ctx){ return this;}
	protected State serverFailed (ClientContext ctx){ return this;}
    }
    private static State STATE_INIT = new State (){
	    public String toString (){ return "STATE_INIT";}
	    @Override
	    protected State clientMessageReceived (ClientContext ctx, HttpMessage req){
		ctx._clientChannel.disableReading ();
		if (ctx.parseDestination (req) == -1){
		    ctx.closeClient (true);
		    return STATE_CLOSED.enter (ctx, this, "Invalid request URL (invalid host)");
		}
		ctx.storeReq (req);
		if (ctx._serverAddress == null){
		    ctx.resolve ();
		    return STATE_RESOLVING_SERVER.enter (ctx, this, "Resolving destination : "+ctx._host);
		}
		ctx.connect ();
		return STATE_CONNECTING_SERVER.enter (ctx, this, "Connect to ", ctx._serverAddress);
	    }
	    @Override
	    protected State clientClosed (ClientContext ctx){
		return STATE_CLOSED.enter (ctx, this, "Client Closed");
	    }
	};
    private static State STATE_RESOLVING_SERVER = new State (){
	    public String toString (){ return "STATE_RESOLVING_SERVER";}
	    @Override
	    protected State clientClosed (ClientContext ctx){
		return STATE_CLOSED.enter (ctx, this, "Client Closed");
	    }
	    @Override
	    protected State serverResolved (ClientContext ctx){
		if (ctx._serverAddress == null){
		    ctx._clientChannel.send (HttpUtils.getUnknownHostResponse (ctx.releaseReq ()));
		    ctx.closeClient (true);
		    return STATE_CLOSED.enter (ctx, this, "Cannot resolve destination address");
		}
		ctx.connect ();
		return STATE_CONNECTING_SERVER.enter (ctx, this, "Connect to ", ctx._serverAddress);
	    }
	};
    private static State STATE_CONNECTING_SERVER = new State (){
	    public String toString (){ return "STATE_CONNECTING_SERVER";}
	    @Override
	    protected State clientClosed (ClientContext ctx){
		return STATE_CLOSED.enter (ctx, this, "Client Closed");
	    }
	    @Override
	    protected State serverConnected (ClientContext ctx){
		if (ctx._tls && !ctx._nextProxy1){ // CONNECT and no next proxy --> respond OK
		    ctx._clientChannel.send (HttpUtils.getConnectionEstablishedResponse (ctx.releaseReq (), true));
		} else {
		    ctx._serverChannel.send (ctx.releaseReq ().toByteBuffers (true, true, true), false);
		}
		ctx._clientChannel.enableReading ();
		return STATE_CONNECTED.enter (ctx, this, "Connected to server");
	    }
	    @Override
	    protected State serverFailed (ClientContext ctx){
		if (ctx._tls){
		    ctx._clientChannel.send (HttpUtils.getConnectionEstablishedResponse (ctx.releaseReq (), false));
		} else {
		    ctx._clientChannel.send (HttpUtils.getHostUnreachableResponse (ctx.releaseReq ()));
		}
		ctx.closeClient (true);
		return STATE_CLOSED.enter (ctx, this, "Connection Failed to server");
	    }
	};
    private static State STATE_CONNECTED = new State (){
	    public String toString (){ return "STATE_CONNECTED";}
	    @Override
	    protected State clientMessageReceived (ClientContext ctx, HttpMessage req){
		if (req.isFirst ()){
		    int parsed = ctx.parseDestination (req);
		    if (parsed == -1){
			ctx.closeClient (true);
			ctx.closeServer ();
			return STATE_CLOSED.enter (ctx, this, "Invalid request URL (invalid host)");
		    }
		    if (parsed == 2){
			// new destination
			if (ctx._logger.isDebugEnabled ())
			    ctx._logger.debug (ctx+" : switch server");
			ctx._meters.getChannelSwitchMeter ().inc (1);
			ctx._clientChannel.disableReading ();
			ctx.closeServer ();
			ctx.storeReq (req);
			if (ctx._serverAddress == null){
			    ctx.resolve ();
			    return STATE_RESOLVING_SERVER.enter (ctx, this, "Resolving : "+ctx._host);
			}
			ctx.connect ();
			return STATE_CONNECTING_SERVER.enter (ctx, this, "Connect to ", ctx._serverAddress);
		    } // else same destination
		}
		State check = checkServerBuffer (ctx);
		if (check == STATE_CLOSED) return STATE_CLOSED;
		ctx._serverChannel.send (req.toByteBuffers (req.isFirst (), req.isFirst (), true), false);
		return this;
	    }
	    @Override
	    protected State clientMessageReceived (ClientContext ctx, ByteBuffer data){
		State check = checkServerBuffer (ctx);
		if (check == STATE_CLOSED) return STATE_CLOSED;
		ctx._serverChannel.send (data, true);
		return this;
	    }
	    @Override
	    protected State serverMessageReceived (ClientContext ctx, ByteBuffer data){
		int size = ctx._clientChannel.getSendBufferSize ();
		if (size > ctx._pxConf._clientMaxSendBuffer){
		    if (ctx._logger.isInfoEnabled ())
			ctx._logger.info (ctx+" : client : getSendBufferSize="+size+" too large : closing");
		    ctx.closeServer ();
		    ctx.closeClient (false);
		    return STATE_CLOSED.enter (ctx, this, "Client buffer exceeded");
		}
		ctx._clientChannel.send (data, true);
		return this;
	    }
	    @Override
	    protected State clientClosed (ClientContext ctx){
		ctx.closeServer ();
		return STATE_CLOSED.enter (ctx, this, "Client Closed");
	    }
	    @Override
	    protected State serverClosed (ClientContext ctx){
		ctx.closeClient (false);
		return STATE_CLOSED.enter (ctx, this, "Server Closed");
	    }
	    private State checkServerBuffer (ClientContext ctx){
		int size = ctx._serverChannel.getSendBufferSize ();
		if (size > ctx._pxConf._serverMaxSendBuffer){
		    if (ctx._logger.isInfoEnabled ())
			ctx._logger.info (ctx+" : server : getSendBufferSize="+size+" too large : closing");
		    ctx.closeServer ();
		    ctx.closeClient (false);
		    return STATE_CLOSED.enter (ctx, this, "Server buffer exceeded");
		}
		return this;
	    }
	};
    private static State STATE_CLOSED = new State (){
	    public String toString (){ return "STATE_CLOSED";}
	    @Override
	    protected State clientMessageReceived (ClientContext ctx, HttpMessage req){ return this;}
	    @Override
	    protected State serverMessageReceived (ClientContext ctx, ByteBuffer data){ return this;}
	    @Override
	    protected State clientClosed (ClientContext ctx){ return this;}
	    @Override
	    protected State serverResolved (ClientContext ctx){ return this;}
	    @Override
	    protected State serverConnected (ClientContext ctx){
		ctx.closeServer ();
		return this;
	    }
	    @Override
	    protected State serverClosed (ClientContext ctx){ return this;}
	    @Override
	    protected State serverFailed (ClientContext ctx){ return this;}
	};

    //////////////////// Http2RequestListener
    
    @Override
    public void newRequest (RequestContext rc){
	rc.attach (new Http2RequestContext (this, new RequestContextCallback (this, rc)));
    }
    @Override
    public void recvReqMethod (RequestContext rc, String method){
	Http2RequestContext h2rc = rc.attachment ();	
	h2rc.method (method);
	_meters.getReadReqMeter (method).inc (1);
    }
    @Override
    public void recvReqPath (RequestContext rc, String path){
	Http2RequestContext h2rc = rc.attachment ();
	h2rc._path = path;
    }
    @Override
    public void recvReqScheme (RequestContext rc, String scheme){
	Http2RequestContext h2rc = rc.attachment ();
	h2rc._scheme = scheme;
    }
    @Override
    public void recvReqAuthority (RequestContext rc, String auth){
	Http2RequestContext h2rc = rc.attachment ();
	h2rc._auth = auth;
    }
    @Override
    public void recvReqHeader (RequestContext rc, String name, String value){
	Http2RequestContext h2rc = rc.attachment ();
	h2rc.recvReqHeader (name, value);
    }
    @Override
    public void recvReqHeaders (RequestContext rc, boolean done){
	Http2RequestContext h2rc = rc.attachment ();
	h2rc.exec ();
	if (done) h2rc.reqDone ();
    }
    @Override
    public void recvReqData (RequestContext rc, ByteBuffer data, boolean done){
	Http2RequestContext h2rc = rc.attachment ();
	h2rc.reqData (data, true);
	if (done) h2rc.reqDone ();
    }
    @Override
    public void abortRequest (RequestContext rc){
	Http2RequestContext h2rc = rc.attachment ();
	h2rc.abort ();
    }

    private static class RequestContextCallback implements Http2RequestContext.Callback {
	private RequestContext _rc;
	private SendBuffer _respBuffer;
	private RequestContextCallback (ClientContext ctx, RequestContext rc){
	    _rc = rc;
	    _respBuffer = rc.newSendRespBuffer (ctx._pxConf._clientStreamMaxSendBuffer);
	}
	public Executor requestExecutor (){ return _rc.requestExecutor ();}
	public Executor responseExecutor (){ return _rc.responseExecutor ();}
	public boolean isClosed (){ return _rc.isClosed ();}
	public void abortReq (String msg){ _rc.abortStream (Http2Error.Code.INTERNAL_ERROR, msg);}
	public void setRespStatus (int status){ _rc.setRespStatus (status);}
	public void setRespHeader (String name, String value){ _rc.setRespHeader (name, value);}
	public void sendRespHeaders (boolean done){ _rc.sendRespHeaders (done);}
	public void sendRespData (ByteBuffer data, boolean copy, boolean done){ _rc.sendRespData (data, copy, done);}
    }
}
