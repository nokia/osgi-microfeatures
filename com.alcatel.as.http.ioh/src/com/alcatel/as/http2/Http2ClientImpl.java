// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2;

import java.util.concurrent.Executor;
import java.util.Map;
import alcatel.tess.hometop.gateways.reactor.*;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.*;
import alcatel.tess.hometop.gateways.reactor.spi.ChannelListenerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import org.apache.log4j.Logger;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import com.alcatel.as.http2.client.*;
import java.net.*;
import com.alcatel.as.service.concurrent.*;
import org.osgi.service.component.annotations.*;
import java.util.function.Consumer;

@Component
public class Http2ClientImpl implements Http2Client {

    private static final HashMap<String, Object> DEF_PROPS = new HashMap<> ();
    private static Http2ClientImpl INSTANCE;
    
    public static final long DEF_CONNECT_TIMEOUT = Long.getLong(PROP_TCP_CONNECT_TIMEOUT, 1500L);
    public static final long DEF_INIT_DELAY = Long.getLong(PROP_CLIENT_INIT_DELAY, DEF_CONNECT_TIMEOUT + 500L);

    private ReactorProvider _reactorP;
    private volatile ReactorProvider _reactorPMux;
    private PlatformExecutors _execs;
    private ConnectionFactory _connF;
    
    @Reference(target="(type=tls)")
    private ChannelListenerFactory _TLSChannelListenerFactory;

    @Reference(target=("(type=nio)"))
    public void setReactor (ReactorProvider prov){
	_reactorP = prov;
    }
    @Reference(target="(type=mux)", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setMuxReactor (ReactorProvider prov){
	_reactorPMux = prov;
    }
    public void unsetMuxReactor (ReactorProvider prov){
	_reactorPMux = null;
    }
    @Reference
    public void setExecs (PlatformExecutors execs){
	_execs = execs;
    }
    @Reference
    public void setConnectionF (ConnectionFactory cf){
	_connF = cf;
    }
    
    @Activate
    public void activate (){
	INSTANCE = this;
    }

    public void newHttp2Connection (java.net.InetSocketAddress dest,
				    Consumer<Http2Connection> onSuccess,
				    Runnable onFailure,
				    Runnable onClose,
				    Map<String, Object> props){
	if (props == null) props = DEF_PROPS;
	Http2ConnectionImpl conn = new Http2ConnectionImpl (dest, onSuccess, onFailure, onClose, props);

	long initDelay = Utils.getLongProperty (PROP_CLIENT_INIT_DELAY, props, DEF_INIT_DELAY);
	long connectDelay = Utils.getLongProperty (PROP_TCP_CONNECT_TIMEOUT, props, DEF_CONNECT_TIMEOUT);
	if (initDelay < connectDelay) connectDelay = initDelay;
	
	Map<TcpClientOption, Object> opts = new HashMap<> ();
	opts.put (TcpClientOption.TIMEOUT, connectDelay);
	InetAddress addr = Utils.getInetAddressProperty (PROP_TCP_CONNECT_SRC, props, null);
	if (addr != null) opts.put (TcpClientOption.FROM_ADDR, new InetSocketAddress (addr, 0));	
	opts.put (TcpClientOption.TCP_NO_DELAY, Utils.getBooleanProperty (PROP_TCP_NO_DELAY, props, true));
	opts.put (TcpClientOption.USE_DIRECT_BUFFER, Boolean.TRUE);

	Executor ex = (Executor) props.get (PROP_EXECUTOR_READ);
	if (ex != null)
	    opts.put (TcpClientOption.INPUT_EXECUTOR, ex);
	else
	    opts.put (TcpClientOption.INPUT_EXECUTOR, ex = _execs.createQueueExecutor (_execs.getProcessingThreadPoolExecutor ()));
	
	InetSocketAddress pxAddr = (InetSocketAddress) props.get (PROP_PROXY_ADDRESS);
	if (pxAddr == null){
	    addr = Utils.getInetAddressProperty (PROP_PROXY_IP, props, null);
	    if (addr != null){
		pxAddr = new InetSocketAddress (addr, Utils.getIntProperty (PROP_PROXY_PORT, props, conn.secure () ? 443 : 3128));
	    }
	}
	boolean useProxy = pxAddr != null;

	Security security = null;
	Security pxSecurity = null;
	if (conn.secure ()){
	    try{
		List<String> protocols = Utils.getStringListProperty (PROP_TCP_SECURE_PROTOCOL, props);
		List<String> ciphers = Utils.getStringListProperty (PROP_TCP_SECURE_CIPHER, props);
		String ksFile = (String) props.get (PROP_TCP_SECURE_KEYSTORE_FILE);
		String ksPwd = (String) props.get (PROP_TCP_SECURE_KEYSTORE_PWD);
		String ksType = (String) props.get (PROP_TCP_SECURE_KEYSTORE_TYPE);
		String ksAlgo = (String) props.get (PROP_TCP_SECURE_KEYSTORE_ALGO);
		String epIdalgo = (String) props.get (PROP_TCP_SECURE_ENDPOINT_IDENTITY_ALGO);
		boolean doSni = Utils.getBooleanProperty (PROP_TCP_SECURE_SNI, props, false); // false by def by precaution
		String host = dest.getHostString ();
		String sni = doSni ? (Utils.isHostName (host) ? host : null) : null;
		
		security = new Security();
		if (protocols != null) security.addProtocol (protocols.toArray (new String[0]));
		if (ciphers != null) security.addCipher (ciphers.toArray (new String[0]));
		if (ksFile != null) security.keyStore (new java.io.FileInputStream(ksFile));
		if (ksPwd != null) security.keyStorePassword (ksPwd);
		if (ksType != null) security.keyStoreType (ksType);
		if (ksAlgo != null) security.keyStoreAlgorithm (ksAlgo);
		if (epIdalgo != null) security.endpointIdentificationAlgorithm (epIdalgo);
		if (sni != null) security.setSNI (sni);
		security.endpointIdentity (dest.getHostString ());
		security.addApplicationProtocols ("h2");
		security.build ();
	    } catch (Exception e){
		throw new IllegalArgumentException ("Invalid security arguments : "+e);
	    }
	}
	if (conn.proxySecure ()){
	    try{
		List<String> protocols = Utils.getStringListProperty (PROP_PROXY_SECURE_PROTOCOL, props);
		List<String> ciphers = Utils.getStringListProperty (PROP_PROXY_SECURE_CIPHER, props);
		String ksFile = (String) props.get (PROP_PROXY_SECURE_KEYSTORE_FILE);
		String ksPwd = (String) props.get (PROP_PROXY_SECURE_KEYSTORE_PWD);
		String ksType = (String) props.get (PROP_PROXY_SECURE_KEYSTORE_TYPE);
		String ksAlgo = (String) props.get (PROP_PROXY_SECURE_KEYSTORE_ALGO);
		String epIdalgo = (String) props.get (PROP_PROXY_SECURE_ENDPOINT_IDENTITY_ALGO);
		String epId = (String) props.get (PROP_PROXY_SECURE_ENDPOINT_IDENTITY_NAME);

		pxSecurity = new Security();
		if (protocols != null) pxSecurity.addProtocol (protocols.toArray (new String[0]));
		if (ciphers != null) pxSecurity.addCipher (ciphers.toArray (new String[0]));
		if (ksFile != null) pxSecurity.keyStore (new java.io.FileInputStream(ksFile));
		if (ksPwd != null) pxSecurity.keyStorePassword (ksPwd);
		if (ksType != null) pxSecurity.keyStoreType (ksType);
		if (ksAlgo != null) pxSecurity.keyStoreAlgorithm (ksAlgo);
		if (epIdalgo != null) pxSecurity.endpointIdentificationAlgorithm (epIdalgo);
		if (epId != null){
		    pxSecurity.endpointIdentity (epId);
		}else{
		    if (useProxy) // by precaution
			pxSecurity.endpointIdentity (pxAddr.getHostString ());
		}
		pxSecurity.build ();
	    } catch (Exception e){
		throw new IllegalArgumentException ("Invalid Proxy Security arguments : "+e);
	    }
	}

	Map<String, Object> filterMap = null;
	if (useProxy){
	    if (conn.proxySecure ()){
		if (conn.secure ()){
		    // tls with proxy, tls with server
		    filterMap = new HashMap<> ();
		    filterMap.put ("security", security);
		    opts.put (TcpClientOption.SECURITY, pxSecurity);
		    security.delayed ();
		} else {
		    // tls with proxy, no tls with server
		    opts.put (TcpClientOption.SECURITY, pxSecurity);
		}
	    } else {
		if (conn.secure ()){
		    // no tls with proxy, tls with server
		    opts.put (TcpClientOption.SECURITY, security);
		    security.delayed ();
		} else {
		    // no tls with proxy, no tls with server
		}
	    }
	} else {
	    if (conn.secure ()){
		// no proxy, tls with server
		opts.put (TcpClientOption.SECURITY, security);
	    } else {
		// no proxy, no tls with server
	    }
	}
	
	conn.initDelay (initDelay);
	try{
	    boolean useMux = Utils.getBooleanProperty (PROP_REACTOR_MUX, props, false);
	    ReactorProvider provider = _reactorP;
	    if (useMux){
		provider = _reactorPMux;
		if (provider == null){
		    ex.execute (() -> {conn.connectionFailed (null,
		    		new Exception ("MuxReactorProvider service not available. "
		    		+ "Check if the Mux connection is properly "
		    		+ "configured or disable mux mode in your age "));});
		    return;
		}
	    }
	    TcpClientChannelListener listener = (filterMap == null) ?
		conn :
		_TLSChannelListenerFactory.createListener(conn, filterMap);
	    provider.tcpConnect (provider.getDefaultReactor (),
				 conn.proxyAddress (pxAddr).connectAddress (),
				 listener,
				 opts);
	}catch(Exception e){
	    // getDefaultReactor cannot throw an exc
	}
    }

    public static class Http2ConnectionImpl implements Http2Connection,
							TcpClientChannelListener,
							ConnectionListener
    {

	private AtomicInteger SEED = new AtomicInteger (1);

	private InetSocketAddress _remoteAddress, _localAddress, _proxyAddress;
	private ConnectionConfig _conf;
	private Connection _connection;
	private java.util.function.Consumer<Http2Connection> _onSuccess;
	private Runnable _onFailure, _onClose;
	private Map<String, Object> _props;
	private long _maxConcurrentStreams, _concurrentStreams;
	private ProxyConnect _proxyConnect;
	private Executor _writeExec;
	private State _state = STATE_TCP_CONNECTING;
	private Future _timeoutF;
	private Status _status;
	private OnAvailable _onAvailable;
	private boolean _secure, _proxySecure;
	private String _toString;
	private boolean _closing;
	private Future _closeFuture;
	private boolean _exportTlsKey;
	private String _exportTlsKeyLabel;
	private int _exportTlsKeyLen;
	private byte[] _exportTlsKeyCtx;
	private Map<String, Object> _exportedTlsKey;
	
	private Http2ConnectionImpl (InetSocketAddress dest,
				     java.util.function.Consumer<Http2Connection> onSuccess,
				     Runnable onFailure,
				     Runnable onClose,
				     Map<String, Object> props){
	    _remoteAddress = dest;
	    _onSuccess = onSuccess;
	    _onFailure = onFailure;
	    _onClose = onClose;
	    _props = props;
	    _secure = Utils.getBooleanProperty (PROP_TCP_SECURE, props, false);
	    _proxySecure = Utils.getBooleanProperty (PROP_PROXY_SECURE, props, false);
	    if (_secure){
		_exportTlsKeyLabel = (String) props.get (Http2Client.PROP_TCP_SECURE_KEYEXPORT_LABEL);
		_exportTlsKeyLen = Utils.getIntProperty (Http2Client.PROP_TCP_SECURE_KEYEXPORT_LENGTH, props, -1);
		_exportTlsKeyCtx = (byte[]) props.get (Http2Client.PROP_TCP_SECURE_KEYEXPORT_CONTEXT);
		_exportTlsKey = _exportTlsKeyLabel != null && _exportTlsKeyLen > 0;
	    }
	    _writeExec = (Executor) props.get (PROP_EXECUTOR_WRITE);
	    if (_writeExec == null) _writeExec = new SerialExecutor ();

	    Settings settings = new Settings ().load (props);
	    Object o = props.get (PROP_HTTP2_CONFIG);
	    if (o != null){
		_conf = (ConnectionConfig) o;
		if (_conf.writeExecutor () == null) _conf.writeExecutor (_writeExec); // coherence check
	    } else _conf = new ConnectionConfig (settings, Utils.getLoggerProperty (PROP_CLIENT_LOGGER, props, null))
		     .load (false, props)
		     .writeExecutor (_writeExec);
	    _toString = new StringBuilder ().append ("Http2Connection[").append (dest).append (']').toString ();
	}
	public String toString (){
	    return _toString;
	}
	private Http2ConnectionImpl proxyAddress (InetSocketAddress proxy){ _proxyAddress = proxy; return this;}
	private InetSocketAddress connectAddress (){ return _proxyAddress != null ? _proxyAddress : _remoteAddress;}
	private Http2ConnectionImpl initDelay (long delay){
	    _conf.initDelay (delay);
	    _timeoutF = INSTANCE._connF._timerS.schedule (_writeExec, this::timeout, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
	    return this;
	}
	private boolean secure (){ return _secure;}
	private boolean proxySecure (){ return _proxySecure;}
	private void exportTlsKey (TcpChannel cnx){
	    if (_exportTlsKey &&
		cnx.isSecure ()
		){
		_exportedTlsKey = cnx.exportTlsKey (_exportTlsKeyLabel, _exportTlsKeyCtx, _exportTlsKeyLen);
		_exportTlsKey = false;
	    }
	}
	public Map<String, Object> exportTlsKey (){ return _exportedTlsKey;}
	
	private void initConnection (TcpChannel cnx){
	    _connection = INSTANCE._connF.newClientConnection (_conf, cnx);
	    _onAvailable = new OnAvailable (this::isAvailable, _connection::scheduleInWriteExecutor, _connection.logger ());
	    _connection.listener (this).init ();
	}
	private void timeout (){
	    _state = _state.timeout (this);
	}
	private void opened (){
	    _status = Status.AVAILABLE;
	    _onSuccess.accept (this);
	    resetTimeoutFuture ();
	}
	private void failed (){
	    _status = Status.UNAVAILABLE_NOT_CONNECTED;
	    if (_onFailure != null) _onFailure.run ();
	    resetTimeoutFuture ();
	}
	private void closed (){
	    _status = Status.UNAVAILABLE_NOT_CONNECTED;
	    neverAvailable ();
	    if (_closeFuture != null){
		_closeFuture.cancel (true);
		_closeFuture = null;
	    }
	    if (_onClose != null) _onClose.run ();
	}
	private void resetTimeoutFuture (){ // note that the timeout may still occur afterwards since triggered by timerService
	    if (_timeoutF == null) return;
	    _timeoutF.cancel (true);
	    _timeoutF = null;
	}
	
	private static class State {
	    // all called in _writeExec
	    public State tcpOpened (Http2ConnectionImpl conn, TcpChannel cnx){ return this;}
	    public State tcpFailed (Http2ConnectionImpl conn){ return this;}
	    public State tcpClosed (Http2ConnectionImpl conn){ return this;}
	    public State http2Opened (Http2ConnectionImpl conn, Connection cnx){ return this;}
	    public State http2Failed (Http2ConnectionImpl conn){ return this;}
	    public State http2Closed (Http2ConnectionImpl conn){ return this;}
	    public State timeout (Http2ConnectionImpl conn){ return this;}
	    public boolean notConnected (){return true;}
	}
	private static State STATE_DONE = new State ();
	private static State STATE_TCP_CONNECTING = new State (){
		@Override
		public State tcpOpened (Http2ConnectionImpl conn, TcpChannel cnx){ conn.tcpOpened (cnx); return STATE_TCP_CONNECTED;}
		@Override
		public State tcpFailed (Http2ConnectionImpl conn){ conn.failed (); return STATE_DONE;}
		@Override
		public State timeout (Http2ConnectionImpl conn){ conn.failed (); return STATE_TIMEOUT;}
	    };
	private static State STATE_TCP_CONNECTED = new State (){
		@Override
		public State tcpClosed (Http2ConnectionImpl conn){ conn.failed (); return STATE_DONE;}
		@Override
		public State http2Opened (Http2ConnectionImpl conn, Connection cnx){
		    conn.remoteSettings (cnx.remoteSettings ());
		    conn.opened ();
		    return STATE_OPEN;
		}
		@Override
		public State http2Failed (Http2ConnectionImpl conn){ conn.failed (); return STATE_DONE;}
		@Override
		public State timeout (Http2ConnectionImpl conn){ conn.failed (); return STATE_TIMEOUT;}
	    };
	private static State STATE_TIMEOUT = new State (){
		@Override
		public State tcpOpened (Http2ConnectionImpl conn, TcpChannel cnx){ cnx.shutdown (); return STATE_DONE; }
		@Override
		public State tcpFailed (Http2ConnectionImpl conn){ return STATE_DONE;}
		@Override
		public State tcpClosed (Http2ConnectionImpl conn){ return STATE_DONE;}
		@Override
		public State http2Opened (Http2ConnectionImpl conn, Connection cnx){ cnx.close (Http2Error.Code.NO_ERROR, "Init delay exceeded"); return STATE_DONE;}
		@Override
		public State http2Failed (Http2ConnectionImpl conn){ return STATE_DONE; }
	    };
	private static State STATE_OPEN = new State (){
		@Override
		public State http2Closed (Http2ConnectionImpl conn){ conn.closed (); return STATE_DONE;}
		@Override
		public boolean notConnected (){return false;}
	    };
	
	/*********** TcpClientChannelListener **********/
	// called in _readExec --> must be re-scheduled for _state processing
	
	public void connectionEstablished(TcpChannel cnx){
	    _writeExec.execute (() -> {_state = _state.tcpOpened (this, cnx);});
	}
	private void tcpOpened (TcpChannel cnx){
	    cnx.setWriteBlockedPolicy (AsyncChannel.WriteBlockedPolicy.IGNORE);
	    _localAddress = cnx.getLocalAddress ();
	    if (_proxyAddress == null)
		initConnection (cnx);
	    else
		_proxyConnect = new ProxyConnect ().connect (cnx, _remoteAddress);
	}	
	public void connectionFailed(TcpChannel cnx, Throwable error){
	    if (_conf.logger().isInfoEnabled ())
		_conf.logger().info (Http2ConnectionImpl.this + " : failed to connect");
	    _writeExec.execute (() -> {_state = _state.tcpFailed (Http2ConnectionImpl.this);});
	}
	public void receiveTimeout(TcpChannel cnx){ _connection.receiveTimeout ();}
	public int messageReceived(TcpChannel cnx, ByteBuffer msg){
	    exportTlsKey (cnx);
	    if (_connection != null)
		_connection.received (msg);
	    if (_proxyConnect != null){
		_proxyConnect.received (msg,
					() -> {
					    // ok
					    _proxyConnect = null;
					    if (secure ()){
						try{ cnx.upgradeToSecure ();}
						catch(Exception e){
						    if (_conf.logger ().isInfoEnabled ())
							_conf.logger ().info (Http2ConnectionImpl.this+" : failed to upgradeToSecure", e);
						    _proxyConnect = null;
						    cnx.shutdown ();
						    return;
						}
					    }
					    initConnection (cnx);
					},
					() -> {
					    // ko - dont call failed (not in _writeExec)
					    _proxyConnect = null;
					    cnx.shutdown ();
					}
					);
	    }
	    return 0;
	}
	public void writeBlocked(TcpChannel cnx){}
	public void writeUnblocked(TcpChannel cnx){}
	public void connectionClosed(TcpChannel cnx){
	    if (_connection != null)
		_connection.closed ();
	    else
		_writeExec.execute (() -> { _state = _state.tcpClosed (Http2ConnectionImpl.this);});
	}

	/*********** ConnectionListener **********/
	// called in writeExecutor
	
	public void opened (Connection connection){
	    _state = _state.http2Opened (this, connection);	    
	}
	public void failed (Connection connection){
	    _state = _state.http2Failed (this);	    
	}
	public void closed (Connection connection){
	    _state = _state.http2Closed (this);	    
	}
	public void updated (Connection connection){
	    remoteSettings (connection.remoteSettings ());
	}
	private void remoteSettings (Settings settings){
	    _maxConcurrentStreams = settings.MAX_CONCURRENT_STREAMS;
	    if (_concurrentStreams >= _maxConcurrentStreams)
		_status = Status.UNAVAILABLE_MAX_CONCURRENT;
	}

	/*********** SendReqStream callbacks **********/

	// called in writeExecutor
	protected void closed (SendReqStream stream){
	    _concurrentStreams--;
	    switch (_status){
	    case UNAVAILABLE_EXHAUSTED:
		if (_concurrentStreams == 0)
		    close (Http2Error.CODE_NO_ERROR, "Reached max connection capacity", 0L);
		break;
	    case UNAVAILABLE_MAX_CONCURRENT:
		if (_concurrentStreams < _maxConcurrentStreams)
		    available ();
		break;
	    }
	}

	/*********** Http2Connection **********/
	// called in _writeExec

	protected Object _attachment;

	public <T> T attachment (){ return (T) _attachment;}

	public void attach (Object o){ _attachment = o;}

	public InetSocketAddress localAddress (){ return _localAddress;}
	public InetSocketAddress proxyAddress (){ return _proxyAddress;}
	public InetSocketAddress remoteAddress (){ return _remoteAddress;}

	public Http2Request newRequest (Http2ResponseListener listener){
	    if (_status.available ()){
		int id = SEED.getAndAdd (2) & 0x7F_FF_FF_FF;
		_concurrentStreams++;
		if (id == 0x7F_FF_FF_FF){
		    _status = Status.UNAVAILABLE_EXHAUSTED;
		    neverAvailable (); // TODO should be scheduled ?
		} else {
		    if (_concurrentStreams == _maxConcurrentStreams)
			_status = Status.UNAVAILABLE_MAX_CONCURRENT;
		}
		return new SendReqStream (this, _connection, id).init (listener);
	    }
	    return null;
	}

	public void close (int code, String msg, long delay){
	    close (code, msg, delay, 0L);
	}
	public void close (int code, String msg, long delay, long idleTimeout){
	    switch (_status){
	    case UNAVAILABLE_NOT_CONNECTED: return;
	    case AVAILABLE:
	    case UNAVAILABLE_MAX_CONCURRENT:
		// we inhibit further new reqs
		// and we trigger a close when last stream is closed
		_status = Status.UNAVAILABLE_EXHAUSTED;
		neverAvailable ();
		break;
	    }
	    if (!_closing){ // first time we close
		_closing = true;
		if (delay == 0L){
		    _connection.close (new ConnectionError (Http2Error.code (code), msg));
		} else if (delay == -1L){
		    if (idleTimeout > 0L) _connection.idleTimeout (idleTimeout);
		    // no action related to closing - wait for exhaustion
		} else {
		    if (idleTimeout > 0L) _connection.idleTimeout (idleTimeout);
		    _closeFuture = _connection.scheduleInWriteExecutor (() -> {
			    _connection.close (new ConnectionError (Http2Error.code (code), msg));
			    _closeFuture = null;
			}, delay);
		}
	    } else {
		if (delay == 0L){ // we allow to re-close when delay is 0
		    if (_closeFuture != null){
			_closeFuture.cancel (true);
			_closeFuture = null;
			_connection.close (new ConnectionError (Http2Error.code (code), msg));
		    }
		}
	    }
	}

	public void clone (java.util.function.Consumer<Http2Connection> onSuccess,
			   Runnable onFailure,
			   Runnable onClose){
	    INSTANCE.newHttp2Connection (_remoteAddress, onSuccess, onFailure, onClose, _props);
	}

	public Executor writeExecutor (){ return _connection.writeExecutor ();}
	
	public Executor readExecutor (){ return _connection.readExecutor ();}

	public void sendPriority (int streamId, boolean exclusive, int streamDepId, int weight){
	    _connection.sendPriority (streamId, exclusive, streamDepId, weight);
	}

	public Status status (){
	    return _status;
	}
	public int remainingRequests (){
	    if (_status == Status.UNAVAILABLE_EXHAUSTED) return 0;
	    int next = SEED.get ();
	    return (0x7F_FF_FF_FF - next) / 2;
	}
	public Http2Connection onAvailable (Runnable onSuccess, Runnable onFailure, long delay){
	    _onAvailable.add (onSuccess, onFailure, delay);
	    return this;
	}

	public boolean isAvailable (){ return _status.available ();}

	private void available (){
	    _status = Status.AVAILABLE;
	    _onAvailable.available ();
	}
	private void neverAvailable (){ // idempotent
	    _onAvailable.closed ();
	}
    }    
}
