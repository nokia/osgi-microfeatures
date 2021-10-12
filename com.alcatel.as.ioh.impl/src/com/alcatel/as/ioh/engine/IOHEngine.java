// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.engine;

import java.util.*;
import java.util.concurrent.*;
import java.nio.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.format.DateTimeFormatter;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.tools.*;

import com.alcatel.as.ioh.impl.tools.*;

import org.osgi.framework.*;

import alcatel.tess.hometop.gateways.reactor.*;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.*;

import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.shutdown.ShutdownService;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;
import com.alcatel.as.service.recorder.*;
import com.alcatel.as.util.sctp.*;
import com.alcatel_lucent.as.service.dns.*;
import com.alcatel.as.service.ioh.WorkerAgent;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import com.alcatel.as.ioh.impl.conf.Property;

import com.nextenso.mux.*;
import com.nextenso.mux.util.*;
import com.nextenso.mux.impl.*;
import com.nextenso.mux.impl.ioh.*;

import com.nextenso.mux.MuxFactory.ConnectionListener;

public class IOHEngine {
    
    public static final String PROP_APP_NAME = "ioh.application.name";
    public static final String PROP_APP_NAME_PREFIX = "ioh.application.name.";

    public static final String PROP_PROTOCOL_TEXT = "ioh.protocol.text";
    public static final String PROP_TCP = "ioh.tcp";
    public static final String PROP_TCP_CONNECT_SHARED = "ioh.tcp.connect.shared"; // an outbound socket is seen by all
    public static final String PROP_TCP_CONNECT_UNIQUE = "ioh.tcp.connect.unique"; // a single socket to a given remote ip/port
    public static final String PROP_TCP_CONNECT_SHARED_CLOSE = "ioh.tcp.connect.shared.close"; // a close from a single agent actually closes the connection
    public static final String PROP_TCP_CONNECT_READ_TIMEOUT = "ioh.tcp.connect.read.timeout";
    public static final String PROP_TCP_CONNECT_FROM = "ioh.tcp.connect.from";
    public static final String PROP_TCP_CONNECT_TIMEOUT = "ioh.tcp.connect.timeout";
    public static final String PROP_TCP_CONNECT_TCP_NODELAY = "ioh.tcp.connect.nodelay";
    public static final String PROP_TCP_CONNECT_WRITE_BUFFER = "ioh.tcp.connect.write.buffer";
    public static final String PROP_TCP_CONNECT_READ_BUFFER = "ioh.tcp.connect.read.buffer";    
    public static final String PROP_TCP_CONNECT_SECURE_DELAYED = "ioh.tcp.connect.secure.delayed";
    public static final String PROP_TCP_CONNECT_SECURE_CIPHER = "ioh.tcp.connect.secure.cipher";
    public static final String PROP_TCP_CONNECT_SECURE_PROTOCOL = "ioh.tcp.connect.secure.protocol";
    public static final String PROP_TCP_CONNECT_SECURE_ALPN_PROTOCOL = "ioh.tcp.connect.secure.alpn.protocol";
    public static final String PROP_TCP_CONNECT_SECURE_KEYSTORE_FILE = "ioh.tcp.connect.secure.keystore.file";
    public static final String PROP_TCP_CONNECT_SECURE_KEYSTORE_PWD = "ioh.tcp.connect.secure.keystore.pwd";
    public static final String PROP_TCP_CONNECT_SECURE_KEYSTORE_TYPE = "ioh.tcp.connect.secure.keystore.type";    
    public static final String PROP_TCP_CONNECT_SECURE_KEYSTORE_ALGO = "ioh.tcp.connect.secure.keystore.algo";    
    public static final String PROP_TCP_CONNECT_SECURE_ENDPOINT_IDENTITY_ALGO = "ioh.tcp.connect.secure.endpoint.identity.algo";
    public static final String PROP_TCP_CONNECT_SECURE_KEYSTORE_RELOAD = "ioh.tcp.connect.secure.keystore.reload";
    public static final String PROP_TCP_CONNECT_CLOSE_TIMEOUT = "ioh.tcp.connect.close.timeout";    
    public static final String PROP_TCP_CONNECT_SOCKS_IP = "ioh.tcp.connect.socks.ip";
    public static final String PROP_TCP_CONNECT_SOCKS_PORT = "ioh.tcp.connect.socks.port";
    public static final String PROP_TCP_CONNECT_SOCKS_VERSION = "ioh.tcp.connect.socks.version";
    public static final String PROP_TCP_CONNECT_SOCKS_USER = "ioh.tcp.connect.socks.user";
    public static final String PROP_TCP_CONNECT_SOCKS_PASSWORD = "ioh.tcp.connect.socks.password";
    public static final String PROP_TCP_ACCEPT_SHARED = "ioh.tcp.accept.shared";
    public static final String PROP_TCP_ACCEPT_SHARED_CLOSE = "ioh.tcp.accept.shared.close"; // a close from a single agent actually closes the connection
    public static final String PROP_TCP_ACCEPT_RENOTIFY = "ioh.tcp.accept.renotify";
    public static final String PROP_TCP_LISTEN_NOTIFY = "ioh.tcp.listen.notify";
    public static final String PROP_TCP_LISTEN_RETRY = "ioh.tcp.listen.retry";
    public static final String PROP_TCP_SEND_BUFFER = "ioh.tcp.write.buffer";
    public static final String PROP_UDP = "ioh.udp";
    public static final String PROP_UDP_BIND_SHARED = "ioh.udp.bind.shared";
    public static final String PROP_UDP_BIND_NOTIFY = "ioh.udp.bind.notify";
    public static final String PROP_UDP_BIND_RETRY = "ioh.udp.bind.retry";
    public static final String PROP_UDP_BIND_WRITE_BUFFER = "ioh.udp.bind.write.buffer";
    public static final String PROP_UDP_BIND_READ_BUFFER = "ioh.udp.bind.read.buffer";
    public static final String PROP_UDP_READ_TIMEOUT = "ioh.udp.read.timeout";
    public static final String PROP_UDP_SEND_BUFFER = "ioh.udp.write.buffer";
    public static final String PROP_SCTP = "ioh.sctp";
    public static final String PROP_SCTP_CONNECT_SHARED = "ioh.sctp.connect.shared"; // an outbound socket is seen by all
    public static final String PROP_SCTP_CONNECT_UNIQUE = "ioh.sctp.connect.unique"; // a single socket to a given remote ip/port
    public static final String PROP_SCTP_CONNECT_SHARED_CLOSE = "ioh.sctp.connect.shared.close"; // a close from a single agent actually closes the connection
    public static final String PROP_SCTP_CONNECT_READ_TIMEOUT = "ioh.sctp.connect.read.timeout";
    public static final String PROP_SCTP_CONNECT_FROM = "ioh.sctp.connect.from";
    public static final String PROP_SCTP_CONNECT_FROM_SECONDARY = "ioh.sctp.connect.from.secondary";
    public static final String PROP_SCTP_CONNECT_TIMEOUT = "ioh.sctp.connect.timeout";
    public static final String PROP_SCTP_CONNECT_SCTP_NODELAY = "ioh.sctp.connect.nodelay";
    public static final String PROP_SCTP_CONNECT_WRITE_BUFFER = "ioh.sctp.connect.write.buffer";
    public static final String PROP_SCTP_CONNECT_READ_BUFFER = "ioh.sctp.connect.read.buffer";
    public static final String PROP_SCTP_CONNECT_CLOSE_TIMEOUT = "ioh.sctp.connect.close.timeout";    
    public static final String PROP_SCTP_ACCEPT_SHARED = "ioh.sctp.accept.shared";
    public static final String PROP_SCTP_ACCEPT_SHARED_CLOSE = "ioh.sctp.accept.shared.close"; // a close from a single agent actually closes the connection
    public static final String PROP_SCTP_ACCEPT_RENOTIFY = "ioh.sctp.accept.renotify";
    public static final String PROP_SCTP_LISTEN_NOTIFY = "ioh.sctp.listen.notify";
    //public static final String PROP_SCTP_LISTEN_RETRY = "ioh.sctp.listen.retry";
    public static final String PROP_SCTP_SEND_BUFFER = "ioh.sctp.write.buffer";
    public static final String PROP_SCTP_CONNECT_SECURE_CIPHER = "ioh.sctp.connect.secure.cipher";
    public static final String PROP_SCTP_CONNECT_SECURE_PROTOCOL = "ioh.sctp.connect.secure.protocol";
    public static final String PROP_SCTP_CONNECT_SECURE_ALPN_PROTOCOL = "ioh.sctp.connect.secure.alpn.protocol";
    public static final String PROP_SCTP_CONNECT_SECURE_KEYSTORE_FILE = "ioh.sctp.connect.secure.keystore.file";
    public static final String PROP_SCTP_CONNECT_SECURE_KEYSTORE_PWD = "ioh.sctp.connect.secure.keystore.pwd";
    public static final String PROP_SCTP_CONNECT_SECURE_KEYSTORE_TYPE = "ioh.sctp.connect.secure.keystore.type";
    public static final String PROP_SCTP_CONNECT_SECURE_KEYSTORE_ALGO = "ioh.sctp.connect.secure.keystore.algo";
    public static final String PROP_SCTP_CONNECT_SECURE_ENDPOINT_IDENTITY_ALGO = "ioh.sctp.connect.secure.endpoint.identity.algo";
    public static final String PROP_SCTP_CONNECT_SECURE_KEYSTORE_RELOAD = "ioh.sctp.connect.secure.keystore.reload";
    public static final String PROP_MUX = "ioh.mux";
    public static final String PROP_MUX_SEND_BUFFER = "ioh.mux.write.buffer";
    public static final String PROP_MUX_READ_TIMEOUT = "ioh.mux.read.timeout";
    public static final String PROP_REMOTE = "ioh.remote";
    public static final String PROP_REMOTE_LISTEN_NOTIFY = "ioh.remote.listen.notify";
    public static final String PROP_REMOTE_BIND_NOTIFY = "ioh.remote.bind.notify";
    public static final String PROP_REMOTE_SEND_BUFFER = "ioh.remote.write.buffer";
    public static final String PROP_LOCAL_SEND_BUFFER = "ioh.local.write.buffer";
    public static final String PROP_AGENT_LOAD_MONITORABLE = "ioh.agent.load.monitorable";
    public static final String PROP_AGENT_LOAD_METER = "ioh.agent.load.meter";
    public static final String PROP_AGENT_LOAD_PERIOD = "ioh.agent.load.period";
    public static final String PROP_AGENT_LOAD_SELECT = "ioh.agent.load.select";
    public static final String PROP_AGENT_LOAD_MAX = "ioh.agent.load.max";
    public static final String PROP_AGENT_RECONNECT_DELAY = "ioh.agent.reconnect.delay";
    public static final String PROP_AGENT_RECONNECT_KILL = "ioh.agent.reconnect.kill";
    public static final String PROP_AGENT_RECONNECT_EXIT = "ioh.agent.reconnect.exit";
    public static final String PROP_EXT_SERVER_MIN = "ioh.ext.server.min";
    public static final String PROP_HISTORY_CHANNELS = "ioh.history.channels";
    public static final String PROP_LOG_TCP_ACCEPTED = "ioh.log.tcp.accepted";
    public static final String PROP_LOG_SCTP_ACCEPTED = "ioh.log.sctp.accepted";
    public static final String PROP_LOG_TCP_CONNECTED = "ioh.log.tcp.connected";
    public static final String PROP_LOG_SCTP_CONNECTED = "ioh.log.sctp.connected";
    public static final String PROP_LOG_TCP_FAILED = "ioh.log.tcp.failed";
    public static final String PROP_LOG_SCTP_FAILED = "ioh.log.sctp.failed";
    public static final String PROP_LOG_TCP_CLOSED = "ioh.log.tcp.closed";
    public static final String PROP_LOG_SCTP_CLOSED = "ioh.log.sctp.closed";
    
    public static final String PARAM_SECURE_DELAYED = "secure.delayed";
    public static final String PARAM_SECURE_CIPHER = "secure.cipher";
    public static final String PARAM_SECURE_PROTOCOL = "secure.protocol";
    public static final String PARAM_SECURE_ALPN_PROTOCOL = "secure.alpn.protocol";
    public static final String PARAM_SECURE_KEYSTORE_FILE = "secure.keystore.file";
    public static final String PARAM_SECURE_KEYSTORE_PWD = "secure.keystore.pwd";
    public static final String PARAM_SECURE_KEYSTORE_TYPE = "secure.keystore.type";
    public static final String PARAM_SECURE_KEYSTORE_ALGO = "secure.keystore.algo";
    public static final String PARAM_SECURE_ENDPOINT_IDENTITY_ALGO = "secure.endpoint.identity.algo";
    
    protected final static AtomicInteger SOCK_ID = new AtomicInteger(0);
    protected final static AtomicInteger REMOTE_SOCK_ID = new AtomicInteger(0);

    protected static InetAddress INVALID_DEST_ADDRESS; // we use it to piggyback a failed dns attempt
    protected static InetAddress UNALLOWED_DEST_ADDRESS; // we use it to piggyback when _allowTcpConnect/_allowSctpConnect = false
    static {
	try {INVALID_DEST_ADDRESS = InetAddress.getByName ("0.0.0.0");}catch(Exception e){}
	try {UNALLOWED_DEST_ADDRESS = InetAddress.getByName ("0.0.0.1");}catch(Exception e){}
    }

    protected IOHServices _services;
    protected Reactor _reactor;
    protected BundleContext _osgi;
    protected Logger _logger, _tcpLogger, _udpLogger, _sctpLogger, _remoteLogger;
    protected MuxClientList _agentsList = new MuxClientList (); // the list of active agents
    protected List<MuxClient> _agentsPendingActivate = new ArrayList<> (); // used between registerMuxClient and activateMuxClient
    protected List<MuxClient> _agentsPendingConnected = new ArrayList<> (); // used between activateMuxClient and agentConnected
    protected Map<Long, Date> _agentsDisconnectionTimes = new HashMap<> (); // used to manage agents reconnections policies
    protected Map<InetSocketAddress, IOHChannel> _tcpServers = new HashMap<> (); // modified/iterated in engine thread
    protected Map<InetSocketAddress, IOHChannel> _udpServers = new HashMap<> (); // modified/iterated in engine thread
    protected Map<InetSocketAddress, IOHChannel> _sctpServers = new HashMap<> (); // modified/iterated in engine thread
    protected Map<Integer, IOHChannel> _tcpChannels = new ConcurrentHashMap<> (); // modified/iterated in engine thread - read by all agents threads
    protected Map<Integer, IOHChannel> _udpChannels = new ConcurrentHashMap<> (); // modified in engine thread - read by all agents threads
    protected Map<Integer, IOHChannel> _sctpChannels = new ConcurrentHashMap<> (); // modified/iterated in engine thread - read by all agents threads
    protected Map<Object, IOHChannel> _uniqueTcpClientChannels = new ConcurrentHashMap<> (); // modified/iterated in engine thread
    protected Map<Object, IOHChannel> _uniqueSctpClientChannels = new ConcurrentHashMap<> (); // modified/iterated in engine thread
    protected Map<Integer, Object> _socketIds = new HashMap<> (); // used in synchonized blocks
    protected Map<String, Object> _props;
    protected List<MuxClientListener> _listeners = new ArrayList<MuxClientListener> ();
    protected boolean _sharedTcpAccept, _sharedTcpConnect, _uniqueTcpConnect, _sharedUdpBind, _tcpListenNotify, _udpBindNotify, _sctpListenNotify, _sharedSctpAccept, _sharedSctpConnect, _uniqueSctpConnect, _useTcp, _useUdp, _useSctp, _useRemoteIOH, _useMuxAgent, _isText, _sharedCloseTcpAccept, _sharedCloseTcpConnect, _sharedCloseSctpAccept, _sharedCloseSctpConnect;
    protected String[] _tcpConnectSecureCiphers, _tcpConnectSecureProtocols;
    protected List<String> _tcpConnectSecureAlpnProtocols, _sctpConnectSecureAlpnProtocols;
    protected String _tcpConnectSecureKeystoreFile, _tcpConnectSecureKeystorePwd, _tcpConnectSecureKeystoreType, _tcpConnectSecureKeystoreAlgo, _tcpConnectSecureEpIdAlgo;
    protected String[] _sctpConnectSecureCiphers, _sctpConnectSecureProtocols;
    protected String _sctpConnectSecureKeystoreFile, _sctpConnectSecureKeystorePwd, _sctpConnectSecureKeystoreType, _sctpConnectSecureKeystoreAlgo, _sctpConnectSecureEpIdAlgo;
    protected PlatformExecutors _execs;
    protected PlatformExecutor _exec;
    protected String _toString;
    protected boolean _started = false;
    protected boolean _stopped = false;
    protected boolean _draining = false;
    protected boolean _suspended = true;
    protected Map<Integer, RemoteIOHEngine> _remoteEngines = new ConcurrentHashMap<> (); // modified/iterated in engine thread
    protected String _name, _fullName;
    protected boolean _historyChannels = false;
    protected com.alcatel.as.service.recorder.Record _record;
    protected Level _tcpConnectedLogLevel, _sctpConnectedLogLevel, _tcpFailedLogLevel, _sctpFailedLogLevel, _tcpAcceptedLogLevel, _sctpAcceptedLogLevel, _tcpClosedLogLevel, _sctpClosedLogLevel;
    protected ChannelWriter.SendBufferMonitor _sendRemoteAgentBufferMonitor;
    protected ChannelWriter.SendBufferMonitor _sendTcpBufferMonitor;
    protected ChannelWriter.SendBufferMonitor _sendUdpBufferMonitor;
    protected ChannelWriter.SendBufferMonitor _sendSctpBufferMonitor;
    protected ChannelWriter.SendBufferMonitor _sendRemoteIOHBufferMonitor;
    protected ChannelWriter.SendBufferMonitor _sendLocalAgentBufferMonitor;
    protected long _renotifyTcpAccept = 0, _renotifySctpAccept = 0;
    protected long _tcpConnectCloseTimeout = -1L, _sctpConnectCloseTimeout = -1L;
    protected Security _tcpSecurityDelayed, _tcpSecurityNotDelayed, _sctpSecurity;
    protected long _tcpSecurityReloadDelay, _sctpSecurityReloadDelay, _tcpSecurityLastReload, _sctpSecurityLastReload;
    protected boolean _tcpConnectSecureDelayed;
    
    protected Meter _agentMeter, _agentRemoteMeter, _agentIOHMeter, _agentLocalMeter;
    protected Meter _agentClosedMeter, _agentRemoteClosedMeter, _agentLocalClosedMeter, _agentIOHClosedMeter;
    protected Meter _agentStoppedMeter, _agentRemoteStoppedMeter, _agentIOHStoppedMeter, _agentLocalStoppedMeter;
    protected Meter _remoteIOHMeter, _remoteTcpServersMeter, _remoteSctpServersMeter, _remoteUdpChannelsMeter, _remoteTcpChannelsMeter, _remoteSctpChannelsMeter, _remoteSendTcpMeter, _remoteSendUdpMeter, _remoteSendSctpMeter, _remoteReadTcpMeter, _remoteReadUdpMeter, _remoteReadSctpMeter;
    protected Meter _stateSuspendedMeter, _stateDrainingMeter;
    
    protected IOHMeters _meters;
    protected Meter _voidMeter;

    protected InetSocketAddress _socksProxy;
    protected int _socksVersion;
    protected String _socksUser, _socksPassword;

    public IOHEngine (String name, IOHServices services){
	_name = name;
	_fullName = "as.ioh.engine."+name;
	_toString = new StringBuilder ().append ("IOHEngine[").append (name).append (']').toString ();
	_logger = Logger.getLogger (_fullName);
	_udpLogger = Logger.getLogger (_fullName+".traffic.udp");
	_tcpLogger = Logger.getLogger (_fullName+".traffic.tcp");
	_sctpLogger = Logger.getLogger (_fullName+".traffic.sctp");
	_remoteLogger = Logger.getLogger (_fullName+".traffic.remote");
	_logger.info (this+" : created");
	_services = services;
	_execs = _services.getPlatformExecutors ();
    }
    public String toString (){ return _toString;}
    public String name (){ return _name;}
    public String fullName (){ return _fullName;}
    // if not called in reactor, then a synchronized must flush the memory
    public IOHEngine init (TcpServer server){
	if (_started) return this; // make it idempotent for server opened/closed
	_logger.info (this+" : init on : "+server);
	return init (server.getProperties ());
    }
    public IOHEngine init (Map<String, Object> props){
	_props = props; // TODO make a thread-safe copy
	_reactor = (Reactor) _props.get (TcpServer.PROP_SERVER_REACTOR);
	_exec = _reactor.getPlatformExecutor ();
	int readTimeout = getIntProperty (TcpServer.PROP_READ_TIMEOUT, _props, -1);
	if (readTimeout == -1){
	    // set a def value
	    readTimeout = getIntProperty (PROP_MUX_READ_TIMEOUT, _props, -1); // it is an alias for compliancy with agent side
	    if (readTimeout == -1)
		_props.put (TcpServer.PROP_READ_TIMEOUT, "3600"); // 3.6 secs by def --> let agent ping first at 3secs with 500ms accuracy
	}

	_useMuxAgent = getBooleanProperty (PROP_MUX, _props, true);

	_useUdp = getBooleanProperty (PROP_UDP, _props, true);
	_useTcp = getBooleanProperty (PROP_TCP, _props, true);
	_useSctp = getBooleanProperty (PROP_SCTP, _props, false);

	_historyChannels = getBooleanProperty (PROP_HISTORY_CHANNELS, _props, false);
	_record = _services.getRecorderService ().newRecord (_fullName, null, false);

	_tcpConnectedLogLevel = Level.toLevel (getStringProperty (PROP_LOG_TCP_CONNECTED, _props, "INFO"));
	_sctpConnectedLogLevel = Level.toLevel (getStringProperty (PROP_LOG_SCTP_CONNECTED, _props, "INFO"));
	_tcpFailedLogLevel = Level.toLevel (getStringProperty (PROP_LOG_TCP_FAILED, _props, "INFO"));
	_sctpFailedLogLevel = Level.toLevel (getStringProperty (PROP_LOG_SCTP_FAILED, _props, "INFO"));
	_tcpAcceptedLogLevel = Level.toLevel (getStringProperty (PROP_LOG_TCP_ACCEPTED, _props, "INFO"));
	_sctpAcceptedLogLevel = Level.toLevel (getStringProperty (PROP_LOG_SCTP_ACCEPTED, _props, "INFO"));
	_tcpClosedLogLevel = Level.toLevel (getStringProperty (PROP_LOG_TCP_CLOSED, _props, "INFO"));
	_sctpClosedLogLevel = Level.toLevel (getStringProperty (PROP_LOG_SCTP_CLOSED, _props, "INFO"));
	
	_tcpListenNotify = getBooleanProperty (PROP_TCP_LISTEN_NOTIFY, _props, false);
	_udpBindNotify = getBooleanProperty (PROP_UDP_BIND_NOTIFY, _props, false);
	_sctpListenNotify = getBooleanProperty (PROP_SCTP_LISTEN_NOTIFY, _props, false);
	_sharedTcpAccept = getBooleanProperty (PROP_TCP_ACCEPT_SHARED, _props, true);
	_sharedCloseTcpAccept = getBooleanProperty (PROP_TCP_ACCEPT_SHARED_CLOSE, _props, true);
	_sharedTcpConnect = getBooleanProperty (PROP_TCP_CONNECT_SHARED, _props, false);
	_uniqueTcpConnect = getBooleanProperty (PROP_TCP_CONNECT_UNIQUE, _props, false);
	_sharedCloseTcpConnect = getBooleanProperty (PROP_TCP_CONNECT_SHARED_CLOSE, _props, true);
	if (_uniqueTcpConnect) _sharedTcpConnect = true; // if unique then shared = true
	_sharedSctpAccept = getBooleanProperty (PROP_SCTP_ACCEPT_SHARED, _props, true);
	_sharedCloseSctpAccept = getBooleanProperty (PROP_SCTP_ACCEPT_SHARED_CLOSE, _props, true);
	_sharedSctpConnect = getBooleanProperty (PROP_SCTP_CONNECT_SHARED, _props, false);
	_uniqueSctpConnect = getBooleanProperty (PROP_SCTP_CONNECT_UNIQUE, _props, false);
	_sharedCloseSctpConnect = getBooleanProperty (PROP_SCTP_CONNECT_SHARED_CLOSE, _props, true);
	if (_uniqueSctpConnect) _sharedSctpConnect = true; // if unique then shared = true
	_sharedUdpBind = getBooleanProperty (PROP_UDP_BIND_SHARED, _props, true); // if true then agent cannot use sharedUdp=false
	_isText = getBooleanProperty (PROP_PROTOCOL_TEXT, _props, true);
	_useRemoteIOH = getBooleanProperty (PROP_REMOTE, _props, false);
	_sendRemoteAgentBufferMonitor = new ChannelWriter.BoundedSendBufferMonitor (getIntProperty (PROP_MUX_SEND_BUFFER, _props, 2*1024*1024)); // 2M inside
	_sendTcpBufferMonitor = new ChannelWriter.BoundedSendBufferMonitor (getIntProperty (PROP_TCP_SEND_BUFFER, _props,128*1024)); // 128K outside TCP
	_sendUdpBufferMonitor = new ChannelWriter.BoundedSendBufferMonitor (getIntProperty (PROP_UDP_SEND_BUFFER, _props,1*1024*1024)); // 1M outside UDP
	_sendSctpBufferMonitor = new ChannelWriter.BoundedSendBufferMonitor (getIntProperty (PROP_SCTP_SEND_BUFFER, _props,128*1024)); // 128K outside SCTP
	_sendRemoteIOHBufferMonitor = new ChannelWriter.BoundedSendBufferMonitor (getIntProperty (PROP_REMOTE_SEND_BUFFER, _props,5*1024*1024)); // 5M remote
	_sendLocalAgentBufferMonitor = new ChannelWriter.BoundedSendBufferMonitor (getIntProperty (PROP_LOCAL_SEND_BUFFER, _props, 10000)); // 10000 entries
	
	_meters = new IOHMeters (fullName (), _services.getMeteringService ()).setIOHEngineMeters (this);
	_voidMeter = _services.getMeteringService ().createValueSuppliedMeter ("void", new ValueSupplier (){public long getValue (){return 0;}});
	
	_agentMeter = _meters.createIncrementalMeter ("agent", null);
	_agentRemoteMeter = _meters.createIncrementalMeter ("agent.remote", _agentMeter);
	_agentLocalMeter = _meters.createIncrementalMeter ("agent.local", _agentMeter);
	_agentIOHMeter = _meters.createIncrementalMeter ("agent.ioh", _agentMeter);
	_agentClosedMeter = _meters.createIncrementalMeter ("agent.closed", null);
	_agentRemoteClosedMeter = _meters.createIncrementalMeter ("agent.remote.closed", _agentClosedMeter);
	_agentLocalClosedMeter = _meters.createIncrementalMeter ("agent.local.closed", _agentClosedMeter);
	_agentIOHClosedMeter = _meters.createIncrementalMeter ("agent.ioh.closed", _agentClosedMeter);
	_agentStoppedMeter = _meters.createIncrementalMeter ("agent.stopped", null);
	_agentRemoteStoppedMeter = _meters.createIncrementalMeter ("agent.remote.stopped", _agentStoppedMeter);
	_agentIOHStoppedMeter = _meters.createIncrementalMeter ("agent.ioh.stopped", _agentStoppedMeter);
	_agentLocalStoppedMeter = _meters.createIncrementalMeter ("agent.local.stopped", _agentStoppedMeter);
	
	_stateSuspendedMeter = _meters.createAbsoluteMeter ("state.suspended");
	_stateDrainingMeter = _meters.createAbsoluteMeter ("state.draining");
	_stateSuspendedMeter.set (1);

	if (_useTcp){
	    List<String> protocols = Property.getStringListProperty (PROP_TCP_CONNECT_SECURE_PROTOCOL, props);
	    if (protocols != null) _tcpConnectSecureProtocols = protocols.toArray (new String[0]);
	    List<String> ciphers = Property.getStringListProperty (PROP_TCP_CONNECT_SECURE_CIPHER, props);
	    if (ciphers != null) _tcpConnectSecureCiphers = ciphers.toArray (new String[0]);
	    _tcpConnectSecureAlpnProtocols = Property.getStringListProperty (PROP_TCP_CONNECT_SECURE_ALPN_PROTOCOL, props);
	    _tcpConnectSecureKeystoreFile = (String) props.get (PROP_TCP_CONNECT_SECURE_KEYSTORE_FILE);
	    _tcpConnectSecureKeystorePwd = (String) props.get (PROP_TCP_CONNECT_SECURE_KEYSTORE_PWD);
	    _tcpConnectSecureKeystoreType = (String) props.get (PROP_TCP_CONNECT_SECURE_KEYSTORE_TYPE);
	    _tcpConnectSecureKeystoreAlgo = (String) props.get (PROP_TCP_CONNECT_SECURE_KEYSTORE_ALGO);
	    _tcpConnectSecureEpIdAlgo = (String) props.get (PROP_TCP_CONNECT_SECURE_ENDPOINT_IDENTITY_ALGO);
	    _tcpSecurityReloadDelay = getLongProperty (PROP_TCP_CONNECT_SECURE_KEYSTORE_RELOAD, props, 5000L);
	    if (_tcpSecurityReloadDelay == -1L) _tcpSecurityReloadDelay = Long.MAX_VALUE; // never reload
	    else if (_tcpSecurityReloadDelay == 0L) _tcpSecurityReloadDelay = Long.MIN_VALUE; // always reload
	    _tcpConnectSecureDelayed = Property.getBooleanProperty (PROP_TCP_CONNECT_SECURE_DELAYED, props, false, true);
	    loadTcpSecurityContext (true);
	    
	    _renotifyTcpAccept = getLongProperty (PROP_TCP_ACCEPT_RENOTIFY, 0L);
	    _tcpConnectCloseTimeout = getLongProperty (PROP_TCP_CONNECT_CLOSE_TIMEOUT, -1L);

	    String tmp = (String) props.get (PROP_TCP_CONNECT_SOCKS_IP);
	    if (tmp != null){
		try{
		    _socksProxy = new InetSocketAddress (InetAddress.getByName (tmp),
							 getIntProperty (PROP_TCP_CONNECT_SOCKS_PORT, props, 1080));
		    _socksUser = (String) props.get (PROP_TCP_CONNECT_SOCKS_USER);
		    _socksPassword = (String) props.get (PROP_TCP_CONNECT_SOCKS_PASSWORD);
		    _socksVersion = getIntProperty (PROP_TCP_CONNECT_SOCKS_VERSION, props, 5);
		    if (_services.getSocksFactory (_socksVersion) == null)
			throw new Exception ("Invalid socks version : "+_socksVersion);
		}catch(Exception e){
		    _logger.error (this+" : invalid socks proxy configuration");
		    throw new RuntimeException ("Invalid Socks Proxy configuration", e);
		}
	    }
	}
	if (_useSctp){
	    List<String> protocols = Property.getStringListProperty (PROP_SCTP_CONNECT_SECURE_PROTOCOL, props);
	    if (protocols != null) _sctpConnectSecureProtocols = protocols.toArray (new String[0]);
	    List<String> ciphers = Property.getStringListProperty (PROP_SCTP_CONNECT_SECURE_CIPHER, props);
	    if (ciphers != null) _sctpConnectSecureCiphers = ciphers.toArray (new String[0]);
	    _sctpConnectSecureAlpnProtocols = Property.getStringListProperty (PROP_SCTP_CONNECT_SECURE_ALPN_PROTOCOL, props);
	    _sctpConnectSecureKeystoreFile = (String) props.get (PROP_SCTP_CONNECT_SECURE_KEYSTORE_FILE);
	    _sctpConnectSecureKeystorePwd = (String) props.get (PROP_SCTP_CONNECT_SECURE_KEYSTORE_PWD);
	    _sctpConnectSecureKeystoreType = (String) props.get (PROP_SCTP_CONNECT_SECURE_KEYSTORE_TYPE);
	    _sctpConnectSecureKeystoreAlgo = (String) props.get (PROP_SCTP_CONNECT_SECURE_KEYSTORE_ALGO);
	    _sctpConnectSecureEpIdAlgo = (String) props.get (PROP_SCTP_CONNECT_SECURE_ENDPOINT_IDENTITY_ALGO);
	    _sctpSecurityReloadDelay = getLongProperty (PROP_SCTP_CONNECT_SECURE_KEYSTORE_RELOAD, props, 5000L);
	    if (_sctpSecurityReloadDelay == -1L) _sctpSecurityReloadDelay = Long.MAX_VALUE; // never reload
	    else if (_sctpSecurityReloadDelay == 0L) _sctpSecurityReloadDelay = Long.MIN_VALUE; // always reload
	    loadSctpSecurityContext (true);

	    _renotifySctpAccept = getLongProperty (PROP_SCTP_ACCEPT_RENOTIFY, 0L);
	    _sctpConnectCloseTimeout = getLongProperty (PROP_SCTP_CONNECT_CLOSE_TIMEOUT, -1L);
	}
	
	if (_useRemoteIOH){
	    _remoteIOHMeter = _meters.createIncrementalMeter ("ioh.remote", null);
	    if (_useTcp){
		_remoteSendTcpMeter = _meters.createIncrementalMeter ("write.remote.tcp", null);
		_remoteReadTcpMeter = _meters.createIncrementalMeter ("read.remote.tcp", null);
		_remoteTcpChannelsMeter = _meters.createIncrementalMeter ("channel.open.remote.tcp", null);
		_remoteTcpServersMeter = _meters.createIncrementalMeter ("server.open.remote.tcp", null);
	    }
	    if (_useUdp){
		_remoteSendUdpMeter = _meters.createIncrementalMeter ("write.remote.udp", null);
		_remoteReadUdpMeter = _meters.createIncrementalMeter ("read.remote.udp", null);
		_remoteUdpChannelsMeter = _meters.createIncrementalMeter ("channel.open.remote.udp", null);
	    }
	    if (_useSctp){
		_remoteSendSctpMeter = _meters.createIncrementalMeter ("write.remote.sctp", null);
		_remoteReadSctpMeter = _meters.createIncrementalMeter ("read.remote.sctp", null);
		_remoteSctpChannelsMeter = _meters.createIncrementalMeter ("channel.open.remote.sctp", null);
		_remoteSctpServersMeter = _meters.createIncrementalMeter ("server.open.remote.sctp", null);
	    }
	}

	if (getIntProperty (PROP_AGENT_RECONNECT_DELAY, -1) == 0)
	    _props.remove (PROP_AGENT_RECONNECT_DELAY); // dont handle 0 : remove explicitly the prop

	if (getBooleanProperty (PROP_AGENT_RECONNECT_KILL, false) ||
	    getBooleanProperty (PROP_AGENT_RECONNECT_EXIT, false)){
	    if (getIntProperty (PROP_AGENT_RECONNECT_DELAY, -1) == -1)
		_props.put (PROP_AGENT_RECONNECT_DELAY, Integer.MAX_VALUE);
	    // if we kill or exit w/o delay set, then we set Integer.MAX_VALUE for the delay
	}
	
	history ("init");
	return this;
    }
    // must be called in Engine thread
    public boolean start (BundleContext osgi){
	if (_started) return false; // make idem potent to make usage friendly
	history ("start");
	_logger.info (this+" start");
	_started = true;
	_osgi = osgi;
	_meters.start (_osgi);
	new IOHGogoCommands (this).register (osgi);
	resume ();
	return true;
    }
    // return true if the min nb of ext servers is reached
    protected boolean checkMinExtServer (){
	int min = getIntProperty (PROP_EXT_SERVER_MIN, 1);
	if (min == 0) return true;
	int count = 0;
	if (_meters.getOpenTcpServersMeter () != null)
	    count += _meters.getOpenTcpServersMeter ().getValue ();
	if (_meters.getOpenSctpServersMeter () != null)
	    count += _meters.getOpenSctpServersMeter ().getValue ();
	if (_meters.getOpenSharedUdpChannelsMeter () != null)
	    count += _meters.getOpenSharedUdpChannelsMeter ().getValue ();
	return count >= min;
    }
    // must be called in Engine thread
    public void suspend (){
	if (_suspended) return;
	if (checkMinExtServer ()) return;
	history ("suspend");
	_logger.warn (this+" : suspend");
	_suspended = true;
	_stateSuspendedMeter.set (1);
	MuxClientList.Iterator it = new MuxClientList.Iterator (){
		public Object next (MuxClient agent, Object ctx){
		    _logger.warn (IOHEngine.this+" : suspended : closing : "+agent);
		    agent.close ();
		    return null;
		}};
	_agentsList.iterate (it, null);
	_agentsPendingConnected.stream ().forEach (agent -> agent.close ());
	//_agentsPendingActivate is empty
    }
    // must be called in Engine thread
    public void resume (){
	if (!_suspended) return;
	if (!checkMinExtServer ()) return;
	_suspended = false;
	_stateSuspendedMeter.set (0);
	history ("resume");
	_logger.warn (this+" : resume");
	_agentsPendingActivate.stream ().forEach (agent -> activateMuxClient (agent));
	_agentsPendingActivate.clear ();
    }
    // must be called in Engine thread
    public void stop (){
	_logger.warn (this+" : stop");
	_stopped = true;
	_meters.stop ();
	MuxClientList.Iterator it = new MuxClientList.Iterator (){
		public Object next (MuxClient agent, Object ctx){
		    _logger.warn (IOHEngine.this+" : stopped : closing : "+agent);
		    agent.close ();
		    return null;
		}};
	_agentsList.iterate (it, null);
	_agentsPendingActivate.stream ().forEach (agent -> agent.close ());
	_agentsPendingConnected.stream ().forEach (agent -> agent.close ());
    }
    
    public Reactor getReactor (){ return _reactor;}
    public Map<String, Object> getProperties (){ return _props;}
    public PlatformExecutor getPlatformExecutor (){ return _exec;}
    public IOHMeters getIOHMeters (){ return _meters;}
    public void schedule (Runnable r){
	_reactor.schedule (r);
    }
    public PlatformExecutor getProcessingThreadPoolExecutor (){ return _execs.getProcessingThreadPoolExecutor ();}
    public PlatformExecutor createQueueExecutor (){ return _execs.createQueueExecutor (_execs.getProcessingThreadPoolExecutor ());}
    public PlatformExecutor getCurrentExecutor (){ return _execs.getCurrentThreadContext ().getCurrentExecutor ();}

    public IOHServices getIOHServices (){ return _services;}

    public MuxClientList copyMuxClientList (){ return new MuxClientList (_agentsList, _useMuxAgent);}
    public MuxClientList getMuxClientList (){ return _agentsList;}

    public boolean historyChannels (){ return _historyChannels;}
    public com.alcatel.as.service.recorder.Record getRecord (){ return _record;}
    public Level tcpConnectedLogLevel (){ return _tcpConnectedLogLevel;}
    public Level sctpConnectedLogLevel (){ return _sctpConnectedLogLevel;}
    public Level tcpFailedLogLevel (){ return _tcpFailedLogLevel;}
    public Level sctpFailedLogLevel (){ return _sctpFailedLogLevel;}
    public Level tcpAcceptedLogLevel (){ return _tcpAcceptedLogLevel;}
    public Level sctpAcceptedLogLevel (){ return _sctpAcceptedLogLevel;}
    public Level tcpClosedLogLevel (){ return _tcpClosedLogLevel;}
    public Level sctpClosedLogLevel (){ return _sctpClosedLogLevel;}
    public boolean sharedTcpAccept (){ return _sharedTcpAccept; }
    public boolean sharedCloseTcpAccept (){ return _sharedCloseTcpAccept;}
    public boolean sharedTcpConnect (){ return _sharedTcpConnect; }
    public boolean uniqueTcpConnect (){ return _uniqueTcpConnect;}
    public boolean sharedCloseTcpConnect (){ return _sharedCloseTcpConnect;}
    public boolean sharedSctpAccept (){ return _sharedSctpAccept; }
    public boolean sharedCloseSctpAccept (){ return _sharedCloseSctpAccept;}
    public boolean sharedSctpConnect (){ return _sharedSctpConnect; }
    public boolean uniqueSctpConnect (){ return _uniqueSctpConnect;}
    public boolean sharedCloseSctpConnect (){ return _sharedCloseSctpConnect;}
    public boolean sharedUdpBind (){ return _sharedUdpBind;}
    public boolean notifyTcpListen (){ return _tcpListenNotify; }
    public boolean notifyUdpBind (){ return _udpBindNotify; }
    public boolean notifySctpListen (){ return _sctpListenNotify; }
    public boolean useMuxAgent (){ return _useMuxAgent;}
    public boolean useRemoteIOH (){ return _useRemoteIOH;}
    public boolean useTcp (){ return _useTcp;}
    public boolean useUdp (){ return _useUdp;}
    public boolean useSctp (){ return _useSctp;}
    public boolean notifyRemoteTcpListen (){ return getBooleanProperty (PROP_REMOTE_LISTEN_NOTIFY, notifyTcpListen ());}
    public boolean notifyRemoteUdpBind (){ return getBooleanProperty (PROP_REMOTE_BIND_NOTIFY, notifyUdpBind ());}
    public boolean notifyRemoteSctpListen (){ return getBooleanProperty (PROP_REMOTE_LISTEN_NOTIFY, notifySctpListen ());}
    public long renotifyTcpAccept (){ return _renotifyTcpAccept;}
    public long renotifySctpAccept (){ return _renotifySctpAccept;}
    public boolean isTextProtocol (){ return _isText;}
    public Logger getLogger (){ return _logger;}
    public Logger getTcpLogger (){ return _tcpLogger;}
    public Logger getUdpLogger (){ return _udpLogger;}
    public Logger getSctpLogger (){ return _sctpLogger;}
    public Logger getRemoteLogger (){ return _remoteLogger;}
    public ChannelWriter.SendBufferMonitor getSendRemoteAgentBufferMonitor (){ return _sendRemoteAgentBufferMonitor;}
    public ChannelWriter.SendBufferMonitor getSendLocalAgentBufferMonitor (){ return _sendLocalAgentBufferMonitor;}
    public ChannelWriter.SendBufferMonitor getSendTcpBufferMonitor (){ return _sendTcpBufferMonitor;}
    public ChannelWriter.SendBufferMonitor getSendUdpBufferMonitor (){ return _sendUdpBufferMonitor;}
    public ChannelWriter.SendBufferMonitor getSendSctpBufferMonitor (){ return _sendSctpBufferMonitor;}
    public ChannelWriter.SendBufferMonitor getSendRemoteIOHBufferMonitor (){ return _sendRemoteIOHBufferMonitor;}
    
    public Meter getRemoteTcpServersMeter (){ return _remoteTcpServersMeter;}
    public Meter getRemoteTcpChannelsMeter (){ return _remoteTcpChannelsMeter;}
    public Meter getRemoteUdpChannelsMeter (){ return _remoteUdpChannelsMeter;}
    public Meter getRemoteSctpServersMeter (){ return _remoteSctpServersMeter;}
    public Meter getRemoteSctpChannelsMeter (){ return _remoteSctpChannelsMeter;}
    public Meter getRemoteSendTcpMeter (){ return _remoteSendTcpMeter;}
    public Meter getRemoteSendUdpMeter (){ return _remoteSendUdpMeter;}
    public Meter getRemoteSendSctpMeter (){ return _remoteSendSctpMeter;}
    public Meter getRemoteReadTcpMeter (){ return _remoteReadTcpMeter;}
    public Meter getRemoteReadUdpMeter (){ return _remoteReadUdpMeter;}
    public Meter getRemoteReadSctpMeter (){ return _remoteReadSctpMeter;}
    
    protected Map<Integer, RemoteIOHEngine> getRemoteIOHEngines (){ return _remoteEngines;}
    protected Map<Integer, IOHChannel> getTcpChannels (){ return _tcpChannels;}
    protected Map<Integer, IOHChannel> getUdpChannels (){ return _udpChannels;}
    protected Map<Integer, IOHChannel> getSctpChannels (){ return _sctpChannels;}
    
    public void history (String s){ // kept old method name for compatibility
	_record.record (new Event (s));
    }

    // must be scheduled in Engine thread
    public void drain (){ drain (null, null);}  // kept for compatibility
    public void drain (String agentName, String src){
	if (_draining) return; // idempotent by principle
	boolean drainEngine = (agentName == null);
	if (drainEngine){
	    history ("draining");
	    _logger.warn (this+" start draining");
	    _draining = true;
	    _stateDrainingMeter.set (1);
	} else {
	    _logger.info (this+" : "+src+" : stop agent : "+agentName);
	    history ("stopping "+agentName+" from "+src);
	}
	MuxClientList.Iterator it = new MuxClientList.Iterator (){
		public Object next (final MuxClient next, Object o){
		    if (drainEngine || next.getInstanceName ().equals (agentName))
			next.schedule (() -> next.drain ());
		    return o;
		}
	    };
	_agentsList.iterate (it, null);
	_agentsPendingActivate.stream ().forEach (agent -> {
		if (drainEngine || agent.getInstanceName ().equals (agentName))
		    agent.schedule (() -> agent.drain ());
	    });
	_agentsPendingConnected.stream ().forEach (agent -> {
		if (drainEngine || agent.getInstanceName ().equals (agentName))
		    agent.schedule (() -> agent.drain ());
	    });
    }
    public boolean draining (){ return _draining;}
    public void undrain (){ undrain (null, null);} // kept for compatibility
    public void undrain (String agentName, String src){
	boolean undrainEngine = (agentName == null);
	if (undrainEngine){
	    if (!_draining) return;
	    history ("un-draining");
	    _logger.warn (this+" un-draining");
	    _draining = false;
	    _stateDrainingMeter.set (0);
	} else {
	    if (_draining){
		_logger.info (this+" : draining : "+src+" cannot un-stop agent : "+agentName);
		return; // cannot un-stop an agent while draining : safer for now
	    }
	    _logger.info (this+" : "+src+" : un-stop agent : "+agentName);
	    history ("un-stopping "+agentName+" from "+src);
	}
	MuxClientList.Iterator it = new MuxClientList.Iterator (){
		public Object next (final MuxClient next, Object o){
		    if (undrainEngine || next.getInstanceName ().equals (agentName))
			next.schedule (() -> next.undrain ());
		    return o;
		}
	    };
	_agentsList.iterate (it, null);
	_agentsPendingActivate.stream ().forEach (agent -> {
		if (undrainEngine || agent.getInstanceName ().equals (agentName))
		    agent.schedule (() -> agent.undrain ());
	    });
	_agentsPendingConnected.stream ().forEach (agent -> {
		if (undrainEngine || agent.getInstanceName ().equals (agentName))
		    agent.schedule (() -> agent.undrain ());
	    });
    }

    protected boolean loadTcpSecurityContext (boolean force){
	long now = System.currentTimeMillis ();
	if ((!force) && // if we force, then no check of the delay
	    ((now - _tcpSecurityLastReload) < _tcpSecurityReloadDelay))
	    return false;
	try{
	    _tcpSecurityNotDelayed = makeTcpSecurityContext (false);
	    _tcpSecurityDelayed = makeTcpSecurityContext (true);
	    _tcpSecurityLastReload = now;
	    return true;
	}catch(Exception e){
	    _tcpSecurityNotDelayed = _tcpSecurityDelayed = null;
	    _logger.warn (this+" : secure TCP connect not well configured", e);
	    return false;
	}
    }
    protected boolean loadSctpSecurityContext (boolean force){
	long now = System.currentTimeMillis ();
	if ((!force) && // if we force, then no check of the delay
	    ((now - _sctpSecurityLastReload) < _sctpSecurityReloadDelay))
	    return false;
	try{
	    _sctpSecurity = makeSctpSecurityContext ();
	    _sctpSecurityLastReload = now;
	    return true;
	}catch(Exception e){
	    _sctpSecurity = null;
	    _logger.warn (this+" : secure SCTP connect not well configured", e);
	    return false;
	}
    }    

    public Security makeTcpSecurityContext (boolean delayed) throws Exception {
	return makeTcpSecurityContext (_tcpConnectSecureProtocols, _tcpConnectSecureCiphers,
				       _tcpConnectSecureKeystoreFile, _tcpConnectSecureKeystorePwd, _tcpConnectSecureKeystoreType, _tcpConnectSecureKeystoreAlgo,
				       _tcpConnectSecureEpIdAlgo, _tcpConnectSecureAlpnProtocols,
				       delayed);
    }
    public Security makeSctpSecurityContext () throws Exception {
	return makeSctpSecurityContext (_sctpConnectSecureProtocols, _sctpConnectSecureCiphers,
					_sctpConnectSecureKeystoreFile, _sctpConnectSecureKeystorePwd, _sctpConnectSecureKeystoreType, _sctpConnectSecureKeystoreAlgo,
					_sctpConnectSecureEpIdAlgo, _sctpConnectSecureAlpnProtocols);
    }
    // this one was left to avoid a major version upgrade - should not be used
    public static Security makeTcpSecurityContext (String[] tcpConnectSecureProtocols, String[] tcpConnectSecureCiphers,
						   String tcpConnectSecureKeystoreFile, String tcpConnectSecureKeystorePwd, String tcpConnectSecureKeystoreType,
						   List<String> tcpConnectSecureAlpnProtocols,
						   boolean delayed) throws Exception {
	return makeTcpSecurityContext (tcpConnectSecureProtocols, tcpConnectSecureCiphers,
				       tcpConnectSecureKeystoreFile, tcpConnectSecureKeystorePwd, tcpConnectSecureKeystoreType, null, null,
				       tcpConnectSecureAlpnProtocols,
				       delayed);
    }
    public static Security makeTcpSecurityContext (String[] tcpConnectSecureProtocols, String[] tcpConnectSecureCiphers,
						   String tcpConnectSecureKeystoreFile, String tcpConnectSecureKeystorePwd, String tcpConnectSecureKeystoreType, String tcpConnectSecureKeystoreAlgo,
						   String tcpConnectSecureEpIdAlgo, List<String> tcpConnectSecureAlpnProtocols,
						   boolean delayed) throws Exception {
	Security security = new Security();
	if (tcpConnectSecureProtocols != null) security.addProtocol (tcpConnectSecureProtocols);
	if (tcpConnectSecureCiphers != null) security.addCipher (tcpConnectSecureCiphers);
	if (tcpConnectSecureKeystoreFile != null) security.keyStore (new java.io.FileInputStream(tcpConnectSecureKeystoreFile));
	if (tcpConnectSecureKeystorePwd != null) security.keyStorePassword (tcpConnectSecureKeystorePwd);
	if (tcpConnectSecureKeystoreType != null) security.keyStoreType (tcpConnectSecureKeystoreType);
	if (tcpConnectSecureKeystoreAlgo != null) security.keyStoreAlgorithm (tcpConnectSecureKeystoreAlgo);
	if (tcpConnectSecureEpIdAlgo != null) security.endpointIdentificationAlgorithm (tcpConnectSecureEpIdAlgo);
	if (tcpConnectSecureAlpnProtocols != null) {for (String protocol : tcpConnectSecureAlpnProtocols) security.addApplicationProtocols (protocol);}
	if(delayed) security.delayed();
	return security.build ();
    }
    // this one was left to avoid a major version upgrade - should not be used
    public static Security makeSctpSecurityContext (String[] sctpConnectSecureProtocols, String[] sctpConnectSecureCiphers,
						    String sctpConnectSecureKeystoreFile, String sctpConnectSecureKeystorePwd, String sctpConnectSecureKeystoreType,
						    List<String> sctpConnectSecureAlpnProtocols) throws Exception {
	return makeSctpSecurityContext (sctpConnectSecureProtocols, sctpConnectSecureCiphers,
					sctpConnectSecureKeystoreFile, sctpConnectSecureKeystorePwd, sctpConnectSecureKeystoreType, null, null,
					sctpConnectSecureAlpnProtocols);
    }
    public static Security makeSctpSecurityContext (String[] sctpConnectSecureProtocols, String[] sctpConnectSecureCiphers,
						    String sctpConnectSecureKeystoreFile, String sctpConnectSecureKeystorePwd, String sctpConnectSecureKeystoreType, String sctpConnectSecureKeystoreAlgo,
						    String sctpConnectSecureEpIdAlgo, List<String> sctpConnectSecureAlpnProtocols) throws Exception {
	Security security = new Security();
	if (sctpConnectSecureProtocols != null) security.addProtocol (sctpConnectSecureProtocols);
	if (sctpConnectSecureCiphers != null) security.addCipher (sctpConnectSecureCiphers);
	if (sctpConnectSecureKeystoreFile != null) security.keyStore (new java.io.FileInputStream(sctpConnectSecureKeystoreFile));
	if (sctpConnectSecureKeystorePwd != null) security.keyStorePassword (sctpConnectSecureKeystorePwd);
	if (sctpConnectSecureKeystoreType != null) security.keyStoreType (sctpConnectSecureKeystoreType);
	if (sctpConnectSecureKeystoreAlgo != null) security.keyStoreAlgorithm (sctpConnectSecureKeystoreAlgo);
	if (sctpConnectSecureEpIdAlgo != null) security.endpointIdentificationAlgorithm (sctpConnectSecureEpIdAlgo);
	if (sctpConnectSecureAlpnProtocols != null) {for (String protocol : sctpConnectSecureAlpnProtocols) security.addApplicationProtocols (protocol);}
	return security.build ();
    }
    
    /*
     * [1][111 1111] [FF FF FF]
     * [shared][remote-ioh] [sockId]
     */
    public static boolean isSharedSocketId (int sockId){
	return (sockId & 0x80000000) == 0x80000000;
    }
    public static int getRemoteIOH (int sockId){
	return sockId & 0x7F000000;
    }
    // must be thread-safe - called from anywhere
    public int reserveSocketId (Object x, int remoteId, boolean shared){
	int i = REMOTE_SOCK_ID.incrementAndGet ();
	if (i == 0) return reserveSocketId (x, remoteId, shared);
	i = i & 0xFFFFFF;
	i = shared ? i | 0x80000000 : i;
	i = remoteId | i;
	boolean ok = false;
	synchronized (_socketIds){
	    if (ok = (!_socketIds.containsKey (i))){
		_socketIds.put (i, x);
	    }
	}
	return ok ? i : reserveSocketId (x, remoteId, shared);
    }
    // must be thread-safe - called from anywhere
    public int reserveSocketId (Object x, boolean shared){
	int i = SOCK_ID.incrementAndGet ();
	if (i == 0) return reserveSocketId (x, shared);
	i = i & 0xFFFFFF;
	i = shared ? i | 0x80000000 : i;
	boolean ok = false;
	synchronized (_socketIds){
	    if (ok = (!_socketIds.containsKey (i))){
		_socketIds.put (i, x);
	    }
	}
	return ok ? i : reserveSocketId (x, shared);
    }
    // must be thread-safe - called from anywhere
    public Object releaseSocketId (int id){
	synchronized (_socketIds){
	     return _socketIds.remove (id);
	}
    }

    // called in Agent thread before register : to be overridden if needed
    public void initMuxClient (MuxClient agent){}
    // called in Agent thread : when agent sends first muxstart or first muxstop
    public void startMuxClient (MuxClient agent){}
    // called in Agent thread : to be overridden if needed
    public void resetMuxClient (MuxClient agent){}

    // called in Engine thread
    public void registerMuxClient (MuxClient agent){
	history ("registerMuxClient:\t"+agent);
	agent._meters.start (_osgi);

	// check if connection is acceptable
	long uid = agent.getMuxIdentification ().getAgentID ();
	Date date = _agentsDisconnectionTimes.get (uid);
	if (date != null){
	    if (getBooleanProperty (PROP_AGENT_RECONNECT_KILL, false)){
		history ("killing:\t"+agent);
		_logger.warn (this+" : registerMuxClient : "+agent+" : KILL : last disconnection = "+date);
		sendMuxKill (agent);
	    } else if (getBooleanProperty (PROP_AGENT_RECONNECT_EXIT, false)){
		history ("Exiting:\t"+agent);
		_logger.warn (this+" : registerMuxClient : "+agent+" : EXIT : last disconnection = "+date);
		sendMuxExit (agent, 0L);
	    } else {
		if (_logger.isInfoEnabled ()) _logger.info (this+" : registerMuxClient : "+agent+" : reject : last disconnection = "+date);
	    }
	    agent.close ();
	    return;
	}
	
	if (_stopped){
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : registerMuxClient : "+agent+" : reject : stopped=true");
	    agent.close ();
	    return;
	}
	if (_draining){
	    agent.schedule (() -> agent.drain ());
	}
	if (_suspended){
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : registerMuxClient : "+agent+" : engine suspended - delaying activation");
	    _agentsPendingActivate.add (agent);
	    return;
	}
	activateMuxClient (agent);
    }
    // called in Engine thread when/if this iohengine is started : we send mux start to the agent
    public void activateMuxClient (MuxClient agent){
	history ("activateMuxClient:\t"+agent);
	if (_logger.isInfoEnabled ()) _logger.info (this+" : activateMuxClient : "+agent);
	// we send MuxStart to the Agent
	_agentsPendingConnected.add (agent);
	agent.opened (true);
	// we'll call agentConnected when the MuxStart returns from the Agent
    }
    // called in Engine thread when we receive mux start from the agent
    public boolean agentConnected (final MuxClient agent, final MuxClientState state){
	if (state.stopped ()){
	    history ("agentConnected:\t"+agent+" [Stopped]");
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : agentConnected : "+agent+" [Stopped]");
	} else {
	    history ("agentConnected:\t"+agent);
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : agentConnected : "+agent);
	}
	_agentsPendingConnected.remove (agent);
	if (_stopped){
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : agentConnected : "+agent+" : reject : stopped=true");
	    agent.close ();
	    return false;
	}
	if (_suspended){
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : agentConnected : "+agent+" : reject : suspended=true");
	    agent.close ();
	    return false;
	}
	_agentsList.add (agent, state);
	if (agent.isRemoteIOHEngine ()) _agentIOHMeter.inc (1);
	else if (agent.isLocalAgent ()) _agentLocalMeter.inc (1);
	else _agentRemoteMeter.inc (1);
	if (state.stopped ()){
	    if (agent.isRemoteIOHEngine ()) _agentIOHStoppedMeter.inc (1);
	    else if (agent.isLocalAgent ()) _agentLocalStoppedMeter.inc (1);
	    else _agentRemoteStoppedMeter.inc (1);
	}
	
	for (MuxClientListener listener : _listeners)
	    listener.agentConnected (agent, state);
	for (IOHChannel channel : _tcpServers.values ()){
	    channel.agentConnected (agent, state);	
	}
	for (IOHChannel channel : _sctpServers.values ()){
	    channel.agentConnected (agent, state);	
	}
	for (final IOHChannel channel : _udpServers.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentConnected (agent, state);	
		    }
		};
	    channel.schedule (r);
	}
	for (final IOHChannel channel : _tcpChannels.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentConnected (agent, state);	
		    }
		};
	    channel.schedule (r);
	}
	for (final IOHChannel channel : _sctpChannels.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentConnected (agent, state);	
		    }
		};
	    channel.schedule (r);
	}
	agent.registerWorkerAgentService (_osgi);
	return true;
    }

    // for overriding
    public boolean agentClosed (MuxClient agent){ return true;}
    public boolean agentStopped (MuxClient agent){ return true;}
    public boolean agentUnStopped (MuxClient agent){return true;}
    
    // called in Engine thread - when a mux stop is received - to be overriden for specific io handlers
    public void stopMuxClient (final MuxClient agent){
	if (_logger.isInfoEnabled ()) _logger.info (IOHEngine.this+" : stopMuxClient : "+agent);
	if (_agentsList.deactivate (agent) == false) // must be idempotent !
	    return;
	if (agent.isRemoteIOHEngine ()) _agentIOHStoppedMeter.inc (1);
	else if (agent.isLocalAgent ()) _agentLocalStoppedMeter.inc (1);
	else _agentRemoteStoppedMeter.inc (1);
	
	agentStopped (agent);
	
	for (MuxClientListener listener : _listeners)
	    listener.agentStopped (agent);
	for (IOHChannel channel : _tcpServers.values ()){
	    channel.agentStopped (agent);	
	}
	for (IOHChannel channel : _sctpServers.values ()){
	    channel.agentStopped (agent);	
	}
	for (final IOHChannel channel : _udpServers.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentStopped (agent);	
		    }
		};
	    channel.schedule (r);
	}
	for (final IOHChannel channel : _tcpChannels.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentStopped (agent);	
		    }
		};
	    channel.schedule (r);
	}
	for (final IOHChannel channel : _sctpChannels.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentStopped (agent);	
		    }
		};
	    channel.schedule (r);
	}
    }
    // called in Engine thread - when a mux start is received after a mux stop - to be overriden for specific io handlers
    public void unstopMuxClient (final MuxClient agent){
	if (_logger.isInfoEnabled ()) _logger.info (IOHEngine.this+" : un-stopMuxClient : "+agent);
	if (_agentsList.reactivate (agent) == false) // must be idempotent !
	    return;
	if (agent.isRemoteIOHEngine ()) _agentIOHStoppedMeter.inc (-1);
	else if (agent.isLocalAgent ()) _agentLocalStoppedMeter.inc (-1);
	else _agentRemoteStoppedMeter.inc (-1);
	
	agentUnStopped (agent);
	
	for (MuxClientListener listener : _listeners)
	    listener.agentUnStopped (agent);
	for (IOHChannel channel : _tcpServers.values ()){
	    channel.agentUnStopped (agent);	
	}
	for (IOHChannel channel : _sctpServers.values ()){
	    channel.agentUnStopped (agent);	
	}
	for (final IOHChannel channel : _udpServers.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentUnStopped (agent);	
		    }
		};
	    channel.schedule (r);
	}
	for (final IOHChannel channel : _tcpChannels.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentUnStopped (agent);	
		    }
		};
	    channel.schedule (r);
	}
	for (final IOHChannel channel : _sctpChannels.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentUnStopped (agent);	
		    }
		};
	    channel.schedule (r);
	}
    }
    
    // called in Engine thread
    public void unregisterMuxClient (final MuxClient agent){
	history ("unregisterMuxClient:\t"+agent);
	agent._meters.stop ();
	agent.unregisterWorkerAgentService ();
	
	_agentsPendingActivate.remove (agent);
	_agentsPendingConnected.remove (agent);
	boolean wasActive = _agentsList.remove (agent);
	if (_logger.isInfoEnabled ()) _logger.info (IOHEngine.this+" : unregisterMuxClient : "+agent+" wasActive="+wasActive);
	
	if (agent._blacklist){
	    int reconnectDelay = getIntProperty (PROP_AGENT_RECONNECT_DELAY, -1);
	    if (reconnectDelay != -1){
		long uid = agent.getMuxIdentification ().getAgentID ();
		boolean doSchedule = false;
		if (reconnectDelay == Integer.MAX_VALUE){
		    doSchedule = true;
		    reconnectDelay = 60;
		    // remove the entry in 60 secs to avoid a mem leak
		    // however re-schedule after each disconnection (to make it infinite)
		    // it assumes that a reconnection would happen before 1 min
		} else {
		    doSchedule = (_agentsDisconnectionTimes.get (uid) == null);
		}
		if (doSchedule){
		    Date date = new Date ();
		    _agentsDisconnectionTimes.put (uid, date);
		    if (_logger.isInfoEnabled ())
			_logger.info (this+" : scheduling re-allow registerMuxClient for : "+agent+" in "+reconnectDelay+" seconds");
		    _exec.schedule (() -> {
			    if (_agentsDisconnectionTimes.get (uid) != date){
				if (_logger.isDebugEnabled ())
				    _logger.debug (IOHEngine.this+" : ignore re-allow registerMuxClient for : "+agent+" : last disconnection : "+_agentsDisconnectionTimes.get (uid));
				return;
			    }
			    if (_logger.isInfoEnabled ())
				_logger.info (IOHEngine.this+" : re-allow registerMuxClient for : "+agent);
			    history ("re-allow registerMuxClient:\t"+agent);
			    _agentsDisconnectionTimes.remove (uid);
			},
			reconnectDelay, TimeUnit.SECONDS);
		}
	    }
	}
	
	if (!wasActive) return;
	
	if (agent.isRemoteIOHEngine ()) {
	    _agentIOHMeter.inc (-1);
	    _agentIOHClosedMeter.inc (1);
	    if (agent.isStopped ()) _agentIOHStoppedMeter.inc (-1);
	} else if (agent.isLocalAgent ()) {
	    _agentLocalMeter.inc (-1);
	    _agentLocalClosedMeter.inc (1);
	    if (agent.isStopped ()) _agentLocalStoppedMeter.inc (-1);
	} else {
	    _agentRemoteMeter.inc (-1);
	    _agentRemoteClosedMeter.inc (1);
	    if (agent.isStopped ()) _agentRemoteStoppedMeter.inc (-1);
	}
	
	agentClosed (agent);
	
	for (MuxClientListener listener : _listeners)
	    listener.agentClosed (agent);
	for (IOHChannel channel : _tcpServers.values ()){
	    channel.agentClosed (agent);	
	}
	for (IOHChannel channel : _sctpServers.values ()){
	    channel.agentClosed (agent);	
	}
	for (final IOHChannel channel : _tcpChannels.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentClosed (agent);	
		    }
		};
	    channel.schedule (r);
	}
	for (final IOHChannel channel : _sctpChannels.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentClosed (agent);	
		    }
		};
	    channel.schedule (r);
	}
	for (final IOHChannel channel : _udpServers.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentClosed (agent);	
		    }
		};
	    channel.schedule (r);
	}
    }

    // we are limited to 127 remote ioh engines
    // called in this Engine thread
    public int registerRemoteIOHEngine (RemoteIOHEngine remote){
	_remoteIOHMeter.inc (1);
	int id = 0;
	for (int i = 1; i<=0x7F; i++){
	    id = i << 24;
	    if (_remoteEngines.get (id) == null){
		break;
	    }
	}
	_remoteEngines.put (id, remote);
	addListener (remote, remote.getPlatformExecutor ());
	return id;
    }
    // called from the RemoteEngine thread
    public void unregisterRemoteIOHEngine (final RemoteIOHEngine remote){
	Runnable r = new Runnable (){
		public void run (){
		    _remoteIOHMeter.inc (-1);
		    _remoteEngines.remove (remote.id ());
		    removeListener (remote);
		}};
	schedule (r);
    }

    // called in the MuxClient thread
    public void tcpConnect(final MuxClient agent, final MuxClientState state, final InetSocketAddress remote, final Map<ReactorProvider.TcpClientOption, Object> opts){
	if (_uniqueTcpConnect && remote.getAddress () != INVALID_DEST_ADDRESS){
	    Runnable r = new Runnable (){
		    public void run (){

			if (allowUniqueTcpConnect (agent, state, remote, opts) == false){
			    tcpConnectNow (agent, state, new InetSocketAddress (UNALLOWED_DEST_ADDRESS, remote.getPort ()), opts);
			    return;
			}
			
			final IOHChannel channel = getUniqueTcpClientChannel (agent, state.connectionId (), remote);
			if (channel != null){
			    Runnable rr = new Runnable (){
				    public void run (){
					try{
					    if (channel.join (agent, state))
						return;
					}catch(IllegalStateException e){
					    // happens when the channel exec was updated after connectionEstablished
					    channel.schedule (this);
					    return;
					}
					// the channel is closed but not yet cleaned - retry
					tcpConnect (agent, state, remote, opts);
				    }
				};
			    channel.schedule (rr);
			} else {
			    tcpConnectNow (agent, state, remote, opts);
			}
		    }
		};
	    schedule (r);
	} else {
	    tcpConnectNow (agent, state, remote, opts);
	}
    }
    protected void tcpConnectNow (MuxClient agent, MuxClientState state, InetSocketAddress remote, Map<ReactorProvider.TcpClientOption, Object> opts){
	opts.put(TcpClientOption.TIMEOUT, (long) getIntProperty (PROP_TCP_CONNECT_TIMEOUT, _props, 10000));
	opts.put(TcpClientOption.TCP_NO_DELAY, getBooleanProperty (PROP_TCP_CONNECT_TCP_NODELAY, _props, true));
	opts.put(TcpClientOption.SO_RCVBUF, getIntProperty (PROP_TCP_CONNECT_READ_BUFFER, _props, 0));
	opts.put(TcpClientOption.SO_SNDBUF, getIntProperty (PROP_TCP_CONNECT_WRITE_BUFFER, _props, 0));
	if (_tcpConnectCloseTimeout >= 0L) opts.put (TcpClientOption.LINGER, _tcpConnectCloseTimeout);
	if (_sharedTcpConnect)
	    opts.put(TcpClientOption.INPUT_EXECUTOR, _exec);
	else
	    opts.put(TcpClientOption.INPUT_EXECUTOR, agent.getPlatformExecutor ());
	final IOHTcpClientChannel channel = newTcpClientChannel (agent, state.connectionId (), remote, opts);
	if (remote.getAddress () == INVALID_DEST_ADDRESS){
	    // we generate a clean callback
	    PlatformExecutor exec = (PlatformExecutor) opts.get (TcpClientOption.INPUT_EXECUTOR);
	    Runnable r = new Runnable (){
		    public void run (){
			channel.connectionFailed (new java.io.IOException ("Failed to resolve hostname"));
		    }
		};
	    exec.execute (r, ExecutorPolicy.SCHEDULE);
	    return;
	}
	if (remote.getAddress () == UNALLOWED_DEST_ADDRESS){
	    // we generate a clean callback
	    PlatformExecutor exec = (PlatformExecutor) opts.get (TcpClientOption.INPUT_EXECUTOR);
	    Runnable r = new Runnable (){
		    public void run (){
			channel.connectionFailed (new java.io.IOException ("Tcp connect not allowed"));
		    }
		};
	    exec.execute (r, ExecutorPolicy.SCHEDULE);
	    return;
	}
	if (_uniqueTcpConnect){
	    registerUniqueTcpClientChannel (channel);
	}
	if (channel.secure ()){
	    Map<String, String> params = (Map<String, String>) opts.get (ReactorProvider.TcpClientOption.ATTACHMENT);
	    try{
		if(params == null){
		    loadTcpSecurityContext (false);
		    Security security = _tcpConnectSecureDelayed ? _tcpSecurityDelayed : _tcpSecurityNotDelayed;
		    if (security == null) throw new Exception ("Secure TCP Connect not configured");
		    opts.put(TcpClientOption.SECURITY, security);
		} else {
		    if (params.size () == 1 && params.containsKey (PARAM_SECURE_DELAYED)){
			// delayed = true : note : we dont check the value ! can be "1" or "true" for ex
			loadTcpSecurityContext (false);
			Security security = _tcpSecurityDelayed;
			if (security == null) throw new Exception ("Secure TCP Connect not configured");
			opts.put(TcpClientOption.SECURITY, security);
		    } else {
			List<String> list = Property.getStringListProperty (PARAM_SECURE_PROTOCOL, params, ",");
			String[] protocols = list != null ? list.toArray (new String[0]) : _tcpConnectSecureProtocols;
			list = Property.getStringListProperty (PARAM_SECURE_CIPHER, params, ",");
			String[] ciphers = list != null ? list.toArray (new String[0]) : _tcpConnectSecureCiphers;
			List<String> alpns = Property.getStringListProperty (PARAM_SECURE_ALPN_PROTOCOL, params, ",");
			if (alpns == null) alpns = _tcpConnectSecureAlpnProtocols;
			String file = params.getOrDefault (PARAM_SECURE_KEYSTORE_FILE, _tcpConnectSecureKeystoreFile);
			String pwd = params.getOrDefault (PARAM_SECURE_KEYSTORE_PWD, _tcpConnectSecureKeystorePwd);
			String type = params.getOrDefault (PARAM_SECURE_KEYSTORE_TYPE, _tcpConnectSecureKeystoreType);
			String algo = params.getOrDefault (PARAM_SECURE_KEYSTORE_ALGO, _tcpConnectSecureKeystoreAlgo);
			String epIdalgo = params.getOrDefault (PARAM_SECURE_ENDPOINT_IDENTITY_ALGO, _tcpConnectSecureEpIdAlgo);
			Security security = makeTcpSecurityContext (protocols, ciphers, file, pwd, type, algo, epIdalgo, alpns, params.containsKey(PARAM_SECURE_DELAYED));
			opts.put(TcpClientOption.SECURITY, security);
		    }
		}
	    }catch(final Exception e){
		_logger.warn (this+" : failed to load tcp connect SSLContext", e);
		PlatformExecutor exec = (PlatformExecutor) opts.get (TcpClientOption.INPUT_EXECUTOR);
		Runnable r = new Runnable (){
			public void run (){
			    channel.connectionFailed (e);
			}
		    };
		exec.execute (r, ExecutorPolicy.SCHEDULE);
		return;
	    }
	}
	if (_socksProxy != null){
	    Map map = (Map) opts;
	    map.put("socks.dest.addr", remote);
	    if (_socksUser != null){
		map.put ("socks.user", _socksUser);
		if (_socksPassword != null)
		    map.put ("socks.password", _socksPassword);
	    }
	    try{
		TcpClientChannelListener listener = _services.getSocksFactory (_socksVersion).createListener(channel, map);
		_services.getReactorProvider ().tcpConnect(_reactor, _socksProxy, listener, opts);
	    }catch(Exception e){
		_logger.warn (this+" : failed to create SOCKS listener", e);
		PlatformExecutor exec = (PlatformExecutor) opts.get (TcpClientOption.INPUT_EXECUTOR);
		Runnable r = new Runnable (){
			public void run (){
			    channel.connectionFailed (e);
			}
		    };
		exec.execute (r, ExecutorPolicy.SCHEDULE);
		return;
	    }
	    return;
	}
	_services.getReactorProvider ().tcpConnect(_reactor, remote, channel, opts);
    }

    // all the following methods can be overridden if needed
    // they are all called in the engine exec
    // they are mostly designed for the diameter ioh
    protected boolean allowUniqueTcpConnect (MuxClient agent, MuxClientState state, InetSocketAddress remote, Map<ReactorProvider.TcpClientOption, Object> opts){
	return true;
    }
    protected boolean allowUniqueSctpConnect (MuxClient agent, MuxClientState state, InetSocketAddress remote, Map<ReactorProvider.SctpClientOption, Object> opts){
	return true;
    }
    protected IOHChannel getUniqueTcpClientChannel (MuxClient agent, long connectionId, InetSocketAddress remote){
	return _uniqueTcpClientChannels.get (remote);
    }
    protected IOHChannel getUniqueSctpClientChannel (MuxClient agent, long connectionId, InetSocketAddress remote){
	return _uniqueSctpClientChannels.get (remote);
    }
    protected void registerUniqueTcpClientChannel (IOHTcpClientChannel channel){
	_uniqueTcpClientChannels.put (channel.getRemoteAddress (), channel);
    }
    protected void registerUniqueSctpClientChannel (IOHSctpClientChannel channel){
	_uniqueSctpClientChannels.put (channel.getRemoteAddress (), channel);
    }
    protected void unregisterUniqueTcpClientChannel (IOHTcpClientChannel channel){
	_uniqueTcpClientChannels.remove (channel.getRemoteAddress ());
    }
    protected void unregisterUniqueSctpClientChannel (IOHSctpClientChannel channel){
	_uniqueSctpClientChannels.remove (channel.getRemoteAddress ());
    }
    
    // called in the MuxClient thread
    public void tcpListen(final MuxClient agent, final MuxClientState state, final InetSocketAddress requested, final boolean secure){
	tcpListen (agent, state, requested, secure, getIntProperty (PROP_TCP_LISTEN_RETRY, _props, 0));
    }
    protected void tcpListen(final MuxClient agent, final MuxClientState state, final InetSocketAddress requested, final boolean secure, final int retry){
	// can only be shared now
	Runnable r = new Runnable (){
		public void run (){
		    IOHChannel server = _tcpServers.get (requested);
		    boolean mayRetry = retry > 0;
		    if (server != null){
			if (server.secure () == secure){
			    server.join (agent, state);
			    return;
			}
			_logger.warn (IOHEngine.this+" : reject tcpListen from : "+agent+" : "+requested+"/"+secure+" : mismatch in secure configuration");
			mayRetry = false; // force reject
		    }
		    if (mayRetry){
			final int new_retry = retry - 1;
			_logger.warn (IOHEngine.this+" : delaying tcpListen from : "+agent);
			Runnable rr = new Runnable (){
				public void run (){
				    tcpListen (agent, state, requested, secure, new_retry);
				}
			    };
			_exec.schedule (rr, 1000, TimeUnit.MILLISECONDS);
			return;
		    }
		    _logger.warn (IOHEngine.this+" : reject tcpListen from : "+agent+" : "+requested+"/"+secure);
		    // TODO : check if agent is overloaded --> not useful !
		    _meters.getFailedTcpServersMeter ().inc (1);
		    agent.getMuxHandler ().tcpSocketListening (agent, 0, requested.getAddress ().getHostAddress (), requested.getPort (), secure, state.connectionId (), MuxUtils.ERROR_UNDEFINED);
		}};
	schedule (r);
    }
    // called in the MuxClient thread
    public void udpBind(final MuxClient agent, final MuxClientState state, final InetSocketAddress requested, final boolean shared, final Map<ReactorProvider.UdpOption, Object> opts){
	udpBind (agent, state, requested, shared, opts, getIntProperty (PROP_UDP_BIND_RETRY, _props, 0));
    }
    protected void udpBind(final MuxClient agent, final MuxClientState state, final InetSocketAddress requested, final boolean shared, final Map<ReactorProvider.UdpOption, Object> opts, final int retry){
	if (shared){
	    Runnable r = new Runnable (){
		    public void run (){
			final IOHChannel server = _udpServers.get (requested);
			if (server != null){
			    Runnable r = new Runnable (){
				    public void run (){
					if (!server.join (agent, state)){
					    _logger.warn (IOHEngine.this+" : reject shared udpBind from : "+agent);
					    // TODO : check if agent is overloaded --> not useful !
					    getIOHMeters ().getFailedSharedUdpChannelsMeter ().inc (1);
					    agent.getMuxHandler ().udpSocketBound (agent, 0, requested.getAddress ().getHostAddress (), requested.getPort (), shared, state.connectionId (), MuxUtils.ERROR_UNDEFINED);
					}
				    }};
			    server.schedule (r);
			    return;
			}
			if (retry > 0){
			    final int new_retry = retry - 1;
			    _logger.warn (IOHEngine.this+" : delaying shared udpBind from : "+agent);
			    Runnable rr = new Runnable (){
				    public void run (){
					udpBind (agent, state, requested, shared, opts, new_retry);
				    }
				};
			    _exec.schedule (rr, 1000, TimeUnit.MILLISECONDS);
			    return;
			}
			_logger.warn (IOHEngine.this+" : reject shared udpBind from : "+agent);
			// TODO : check if agent is overloaded --> not useful !
			getIOHMeters ().getFailedSharedUdpChannelsMeter ().inc (1);
			agent.getMuxHandler ().udpSocketBound (agent, 0, requested.getAddress ().getHostAddress (), requested.getPort (), shared, state.connectionId (), MuxUtils.ERROR_UNDEFINED);
		    }};
	    schedule (r);
	} else {
	    opts.put(UdpOption.INPUT_EXECUTOR, createQueueExecutor ());
	    opts.put(UdpOption.ENABLE_READ, false);
	    opts.put(UdpOption.SO_RCVBUF, getIntProperty (PROP_UDP_BIND_READ_BUFFER, _props, 0));
	    opts.put(UdpOption.SO_SNDBUF, getIntProperty (PROP_UDP_BIND_WRITE_BUFFER, _props, 0));
	    UdpChannelListener listener = newUdpChannel (agent, state.connectionId (), requested, opts);
	    try{
		UdpChannel channel = _services.getReactorProvider ().udpBind(_reactor, requested, listener, opts);
		listener.connectionOpened (channel);
	    }catch(Throwable t){
		getIOHMeters ().getFailedUnsharedUdpChannelsMeter ().inc (1);
		listener.connectionFailed (null, t);
	    }
	}
    }
    
    // called in the MuxClient thread
    public void sctpConnect(final MuxClient agent, final MuxClientState state, final InetSocketAddress remote, final Map<ReactorProvider.SctpClientOption, Object> opts){
	if (_uniqueSctpConnect && remote.getAddress () != INVALID_DEST_ADDRESS){
	    Runnable r = new Runnable (){
		    public void run (){

			if (allowUniqueSctpConnect (agent, state, remote, opts) == false){
			    sctpConnectNow (agent, state, new InetSocketAddress (UNALLOWED_DEST_ADDRESS, remote.getPort ()), opts);
			    return;
			}
			
			final IOHChannel channel = getUniqueSctpClientChannel (agent, state.connectionId (), remote);
			if (channel != null){
			    Runnable rr = new Runnable (){
				    public void run (){
					// the mismatch check is for CSFAR-3002 (even though this is quite diameter specific, but easier to fix here)
					IOHSctpClientChannel sctp = ((IOHSctpClientChannel) channel);
					boolean match = remote.equals (sctp.getRemoteAddress ());
					if (!match){
					    Runnable r = () -> {sctpConnectNow (agent, state, remote, opts);};
					    sctp.joinMismatch (agent, state, remote, r);
					    return;
					}
					try{
					    if (channel.join (agent, state))
						return;
					}catch(IllegalStateException e){
					    // happens when the channel exec was updated after connectionEstablished
					    channel.schedule (this);
					    return;
					}
					// the channel is closed but not yet cleaned - retry
					sctpConnect (agent, state, remote, opts);
				    }
				};
			    channel.schedule (rr);
			} else {
			    sctpConnectNow (agent, state, remote, opts);
			}
		    }
		};
	    schedule (r);
	} else {
	    sctpConnectNow (agent, state, remote, opts);
	}
    }
    protected void sctpConnectNow (MuxClient agent, MuxClientState state, InetSocketAddress remote, Map<ReactorProvider.SctpClientOption, Object> opts){
	opts.put(SctpClientOption.TIMEOUT, getLongProperty (PROP_SCTP_CONNECT_TIMEOUT, _props, 10000L));
	opts.put(SctpClientOption.SO_RCVBUF, getIntProperty (PROP_SCTP_CONNECT_READ_BUFFER, _props, 0));
	opts.put(SctpClientOption.SO_SNDBUF, getIntProperty (PROP_SCTP_CONNECT_WRITE_BUFFER, _props, 0));
	if (_sctpConnectCloseTimeout >= 0L) opts.put (SctpClientOption.LINGER, _sctpConnectCloseTimeout);
	if (_sharedSctpConnect)
	    opts.put(SctpClientOption.INPUT_EXECUTOR, _exec);
	else
	    opts.put(SctpClientOption.INPUT_EXECUTOR, agent.getPlatformExecutor ());
	final IOHSctpClientChannel channel = newSctpClientChannel (agent, state.connectionId (), remote, opts);
	if (remote.getAddress () == INVALID_DEST_ADDRESS){
	    // we generate a clean callback
	    PlatformExecutor exec = (PlatformExecutor) opts.get (SctpClientOption.INPUT_EXECUTOR);
	    Runnable r = new Runnable (){
		    public void run (){
			channel.connectionFailed (new java.io.IOException ("Failed to resolve hostname"));
		    }
		};
	    exec.execute (r, ExecutorPolicy.SCHEDULE);
	    return;
	}
	if (remote.getAddress () == UNALLOWED_DEST_ADDRESS){
	    // we generate a clean callback
	    PlatformExecutor exec = (PlatformExecutor) opts.get (SctpClientOption.INPUT_EXECUTOR);
	    Runnable r = new Runnable (){
		    public void run (){
			channel.connectionFailed (new java.io.IOException ("Sctp connect not allowed"));
		    }
		};
	    exec.execute (r, ExecutorPolicy.SCHEDULE);
	    return;
	}
	if (_uniqueSctpConnect){
	    registerUniqueSctpClientChannel (channel);
	}
	if (channel.secure ()){
	    Map<String, String> params = (Map<String, String>) opts.get (ReactorProvider.SctpClientOption.ATTACHMENT);
	    try{
		if(params == null){
		    loadSctpSecurityContext (false);
		    if (_sctpSecurity == null) throw new Exception ("Secure SCTP Connect not configured");
		    opts.put(SctpClientOption.SECURITY, _sctpSecurity);
		} else {
		    List<String> list = Property.getStringListProperty (PARAM_SECURE_PROTOCOL, params, ",");
		    String[] protocols = list != null ? list.toArray (new String[0]) : _sctpConnectSecureProtocols;
		    list = Property.getStringListProperty (PARAM_SECURE_CIPHER, params, ",");
		    String[] ciphers = list != null ? list.toArray (new String[0]) : _sctpConnectSecureCiphers;
		    List<String> alpns = Property.getStringListProperty (PARAM_SECURE_ALPN_PROTOCOL, params, ",");
		    if (alpns == null) alpns = _sctpConnectSecureAlpnProtocols;
		    String file = params.getOrDefault (PARAM_SECURE_KEYSTORE_FILE, _sctpConnectSecureKeystoreFile);
		    String pwd = params.getOrDefault (PARAM_SECURE_KEYSTORE_PWD, _sctpConnectSecureKeystorePwd);
		    String type = params.getOrDefault (PARAM_SECURE_KEYSTORE_TYPE, _sctpConnectSecureKeystoreType);
		    String algo = params.getOrDefault (PARAM_SECURE_KEYSTORE_ALGO, _sctpConnectSecureKeystoreAlgo);
		    String epIdalgo = params.getOrDefault (PARAM_SECURE_ENDPOINT_IDENTITY_ALGO, _sctpConnectSecureEpIdAlgo);
		    Security security = makeSctpSecurityContext (protocols, ciphers, file, pwd, type, algo, epIdalgo, alpns);
		    opts.put(SctpClientOption.SECURITY, security);
		}
	    }catch(final Exception e){
		_logger.warn (this+" : failed to load sctp connect SSLContext", e);
		PlatformExecutor exec = (PlatformExecutor) opts.get (SctpClientOption.INPUT_EXECUTOR);
		Runnable r = new Runnable (){
			public void run (){
			    channel.connectionFailed (e);
			}
		    };
		exec.execute (r, ExecutorPolicy.SCHEDULE);
		return;
	    }
	}
	_services.getReactorProvider ().sctpConnect(_reactor, remote, channel, opts);
    }
    
    // called in the mux server thread - not necessarily this Reactor
    public TcpChannelListener muxClientAccepted (TcpChannel channel, Map<String, Object> props, boolean remoteIOH){
	TcpChannelListener agent = newMuxClient (this, channel, props, remoteIOH);
	if (_logger.isInfoEnabled ()){
	    if (remoteIOH)
		_logger.info (this+" : remoteIOHAccepted : "+agent);
	    else
		_logger.info (this+" : muxClientAccepted : "+agent);
	}
	return agent;
    }
    // called in the server thread
    public void serverOpened (TcpServer server){
	server.attach (newTcpServerChannel (this, server).opened ());
	if (_logger.isInfoEnabled ()) _logger.info (this+" : serverOpened : "+server);
    }
    // called in the server thread
    public void serverClosed (TcpServer server){
	if (_logger.isInfoEnabled ()) _logger.info (this+" : serverClosed : "+server);
	final IOHChannel channel = server.attachment ();
	Runnable r = new Runnable (){
		public void run (){
		    channel.connectionClosed ();
		}
	    };
	schedule (r);
    }
    // called in the server thread
    public void serverOpened (SctpServer server){
	server.attach (newSctpServerChannel (this, server).opened ());
	if (_logger.isInfoEnabled ()) _logger.info (this+" : serverOpened : "+server);
    }
    // called in the server thread
    public void serverClosed (SctpServer server){
	if (_logger.isInfoEnabled ()) _logger.info (this+" : serverClosed : "+server);
	final IOHChannel channel = server.attachment ();
	Runnable r = new Runnable (){
		public void run (){
		    channel.connectionClosed ();
		}
	    };
	schedule (r);
    }
    // called in engine thread
    public int registerTcpServer (IOHChannel channel){
	history ("registerTcpServer : "+channel.getLocalAddress ());
	_meters.getOpenTcpServersMeter ().inc (1);
	int id = reserveSocketId (channel, true);
	_tcpServers.put (channel.getLocalAddress (), channel);
	resume ();
	return id;
    }
    // called in engine thread
    public void unregisterTcpServer (IOHChannel channel){
	history ("unregisterTcpServer : "+channel);
	_meters.getOpenTcpServersMeter ().inc (-1);
	_tcpServers.remove (channel.getLocalAddress ());
	releaseSocketId (channel.getSockId ());
	suspend ();
    }
    // called in the server thread
    public TcpChannelListener connectionAccepted (TcpServer server, TcpChannel channel, Map<String, Object> props){
	return newTcpChannel (this, server, channel, props).accepted ();
    }
    // called in engine thread
    public int registerSctpServer (IOHChannel channel){
	history ("registerSctpServer : "+channel.getLocalAddress ());
	_meters.getOpenSctpServersMeter ().inc (1);
	int id = reserveSocketId (channel, true);
	_sctpServers.put (channel.getLocalAddress (), channel);
	resume ();
	return id;
    }
    // called in engine thread
    public void unregisterSctpServer (IOHChannel channel){
	history ("unregisterSctpServer : "+channel);
	_meters.getOpenSctpServersMeter ().inc (-1);
	_sctpServers.remove (channel.getLocalAddress ());
	releaseSocketId (channel.getSockId ());
	suspend ();
    }
    // called in the server thread
    public SctpChannelListener connectionAccepted (SctpServer server, SctpChannel channel, Map<String, Object> props){
	return newSctpChannel (this, server, channel, props).accepted ();
    }
    // called in the server thread
    public UdpChannelListener serverOpened (UdpServer server){
	UdpChannelListener listener = newUdpChannel (this, server.getProperties ());
	listener.connectionOpened (server.getServerChannel ());
	return listener;
    }
    // called in engine thread -- applicable to shared udp servers only
    public int registerUdpServer (IOHChannel channel){
	int id = reserveSocketId (channel, true);
	_udpServers.put (channel.getLocalAddress (), channel);
	_udpChannels.put (id, channel);
	resume ();
	return id;
    }
    // called in engine thread -- applicable to shared udp servers only
    public void unregisterUdpServer (IOHChannel channel){
	_udpServers.remove (channel.getLocalAddress ());
	_udpChannels.remove (channel.getSockId ());
	releaseSocketId (channel.getSockId ());
	suspend ();
    }
    
    // this method is here to avoid making the overriding of MuxClient mandatory if a mux payload is used
    // called in MuxClient thread
    public boolean sendMuxData(MuxClient agent, MuxHeader header, boolean copy, ByteBuffer ... buf) {
	return true;
    }

    protected void sendMuxExit (MuxClient agent, long delay){
	if (agent.isLocalAgent () || agent.isRemoteIOHEngine ()) return; // not handled for now
	_logger.warn ("Send FLAG_MUX_EXIT to : "+agent+", delay="+delay);
	String s = ShutdownService.SHUTDOWN_TOPIC+" "+agent.getMuxIdentification ().getAgentID ();
	byte[] data = null;
	try{data = s.getBytes ("ascii");}catch(Exception e){}// cannot happen
	MuxHeaderV0 h = new MuxHeaderV0 ();
	h.set (delay, 0, ExtendedMuxConnection.FLAG_MUX_EXIT);
	agent.getExtendedMuxHandler ().internalMuxData (agent, h, ByteBuffer.wrap (data));
    }
    protected void sendMuxKill (MuxClient agent){
	if (agent.isLocalAgent () || agent.isRemoteIOHEngine ()) return; // not handled for now
	_logger.warn ("Send FLAG_MUX_KILL to : "+agent);
	MuxHeaderV0 h = new MuxHeaderV0 ();
	h.set (0, 0, ExtendedMuxConnection.FLAG_MUX_KILL);
	agent.getExtendedMuxHandler ().internalMuxData (agent, h, null);
    }
    
    /****************** the following can be overriden to customize the IOHEngine *******************/
    protected TcpChannelListener newMuxClient (IOHEngine engine, TcpChannel channel, Map<String, Object> props, boolean isRemoteIOH){
	return new MuxClient (engine, channel, props, isRemoteIOH);
    }
    protected IOHLocalMuxFactory.IOHLocalMuxConnection newLocalMuxClient (MuxHandler muxHandler, ConnectionListener listener, Map opts){
	return new IOHLocalMuxFactory.IOHLocalMuxConnection (this, muxHandler, listener, opts);
    }
    protected IOHTcpClientChannel newTcpClientChannel (MuxClient agent, long connectionId, InetSocketAddress remote, Map<ReactorProvider.TcpClientOption, Object> opts){
	return new IOHTcpClientChannel (agent, connectionId, remote, opts);
    }
    protected IOHTcpChannel newTcpChannel (IOHEngine engine, TcpServer server, TcpChannel channel, Map<String, Object> props){
	return new IOHTcpChannel (engine, channel, props);
    }
    protected UdpChannelListener newUdpChannel (MuxClient agent, long bindId, InetSocketAddress local, Map<ReactorProvider.UdpOption, Object> opts){
	return new IOHUdpChannel (agent, bindId, local, opts);
    }
    protected UdpChannelListener newUdpChannel (IOHEngine engine, Map<String, Object> props){
	return new IOHUdpChannel (engine, props);
    }
    protected IOHTcpServerChannel newTcpServerChannel (IOHEngine engine, TcpServer server){
	return new IOHTcpServerChannel (engine, server);
    }
    protected IOHSctpServerChannel newSctpServerChannel (IOHEngine engine, SctpServer server){
	return new IOHSctpServerChannel (engine, server);
    }
    protected IOHSctpChannel newSctpChannel (IOHEngine engine, SctpServer server, SctpChannel channel, Map<String, Object> props){
	return new IOHSctpChannel (engine, channel, props);
    }
    protected IOHSctpClientChannel newSctpClientChannel (MuxClient agent, long connectionId, InetSocketAddress remote, Map<ReactorProvider.SctpClientOption, Object> props){
	return new IOHSctpClientChannel (agent, connectionId, remote, props);
    }
    /**********************************************************************/
    
    // must be added/removed in the engine thread
    public void addListener (MuxClientListener listener){
	_listeners.add (listener);
    }
    public void addListener (MuxClientListener listener, PlatformExecutor exec){
	_listeners.add (new MuxClientListenerWrapper (listener, exec));
    }
    public void removeListener (MuxClientListener listener){
	loop: for (int i=0; i<_listeners.size (); i++){
	    if (_listeners.get (i).equals (listener)){
		_listeners.remove (i);
		break loop;
	    }
	}
    }
    
    /************* MuxClient = Agent ****************/

    public static class MuxClient extends ExtendedMuxConnection implements TcpChannelListener, WorkerAgent {

	protected static final AtomicInteger SEED = new AtomicInteger (1);
	
	protected IOHMeters _meters;
	protected Object _context;
	protected Meter _pingMeter, _tcpConnectMeter, _tcpCloseMeter, _sctpConnectMeter, _sctpCloseMeter, _stoppedMeter, _standbyMeter, _udpBindMeter, _udpCloseMeter, _tcpListenMeter, _startMeter, _stopMeter;
	protected Meter _loadMeter;
	protected int _agentLoadMax;
	protected StackSideMuxHandler _ssHandler; // superclass has a field _handler which is a MuxHandler
	protected ExtendedMuxHandler _extHandler;
	protected PlatformExecutor _exec;
	protected String _toString; // NB superclass has a field _id
	protected MuxIdentification _muxId;
	protected String _groupName, _instanceName; // instanceName in MuxIdentification is group__instance : here, we make them directly available
	protected MuxParser _parser;
	protected Map<String, Object> _props;
	protected IOHEngine _engine;
	protected List<MuxClientListener> _listeners = new ArrayList<MuxClientListener> ();
	protected Map<Integer, IOHChannel> _myTcpChannels = new HashMap<> ();
	protected Map<Integer, IOHChannel> _myUdpChannels = new HashMap<> ();
	protected Map<Integer, IOHChannel> _mySctpChannels = new HashMap<> ();
	protected boolean _isRemoteIOH, _connected; // _connected is set when the first sendMuxStart or sendMuxStop is called
	protected boolean _stopped; // this one reflects the actual status of the agent : stopped or not : maybe set by agent or by draining or by agent load meter
	protected boolean _stoppedByAgent; // this one reflects the actual status set by the agent (via MuxStop or via load meter : hence via _stoppedByAgentViaLoadMeter or _stoppedByAgentViaMuxStop)
	protected boolean _stoppedByAgentViaLoadMeter;
	protected boolean _stoppedByAgentViaMuxStop;
	protected boolean _draining; // may be triggered by gogo command : do not want to mess with _stopped
	protected List<String> _aliases = new ArrayList<>(2);
	protected String _agentId;
	protected ChannelWriter.SendBufferMonitor _sendBufferMonitor;
	protected Map<String, String> _applicationParams;
	protected int _uid;
	protected boolean _blacklist = true;
	protected ServiceRegistration _workerAgentRegistration;

	// called in the reactor thread
	public MuxClient (IOHEngine engine, TcpChannel channel, Map<String, Object> props, boolean isRemoteIOH){
	    super (null);
	    _uid = SEED.getAndIncrement ();
	    _engine = engine;
	    _props = props;
	    _isRemoteIOH = isRemoteIOH;
	    _sendBufferMonitor = _isRemoteIOH ? _engine._sendRemoteIOHBufferMonitor : _engine._sendRemoteAgentBufferMonitor;	     
	    setLogger (engine.getLogger ());
	    _exec = (PlatformExecutor) props.get (TcpServer.PROP_READ_EXECUTOR);
	    _handler = _extHandler = _ssHandler = new StackSideMuxHandler (channel, _logger4j);
	    _toString = new StringBuilder ().append ("Agent[").append (channel.getRemoteAddress ()).append (']').toString ();
	    _parser = new MuxParser ();
	    channel.setWriteBlockedPolicy (AsyncChannel.WriteBlockedPolicy.IGNORE);
	    channel.enableReading ();
	    opened (false);
	}
	// for superclass
	protected MuxClient (){
	    super (null);
	    // the super class may set _handler which may not be a StackSideMuxHandler (hence _ssHandler = null)
	    _uid = SEED.getAndIncrement ();
	}

	// called in ioh thread
	protected void registerWorkerAgentService (BundleContext osgi){
	    String protocol = getApplicationParam ("agent.protocol", null);
	    Dictionary props = new Hashtable ();
	    if (protocol != null) props.put (WorkerAgent.PROTOCOL, protocol);
	    props.put (WorkerAgent.GROUP, getGroupName ());
	    props.put (WorkerAgent.INSTANCE, getInstanceName ());
	    _workerAgentRegistration = osgi.registerService (WorkerAgent.class.getName (), this, props);
	}
	protected void unregisterWorkerAgentService (){
	    if (_workerAgentRegistration != null)
		_workerAgentRegistration.unregister ();
	}
	////////////// WorkerAgent interface ///////////
	public boolean isActive(){ return _stoppedMeter.getValue () == 0;} // avoid making _stopped a volatile
	public void activate(){_engine.schedule (() -> _engine.undrain (getInstanceName (), "api"));}
	public void deactivate(){_engine.schedule (() -> _engine.drain (getInstanceName (), "api"));}
	///////////////////////////////////////////////
	
	// dont use attach and attachment : methods already in MuxConnection and may be used by a local MuxHandler
	public <T> T getContext (){ return (T) _context;}
	public void setContext (Object o){ _context = o;}
	
	public String toString (){ return _toString;}
	public List<String> aliases (){ return _aliases;}
	public boolean isRemoteIOHEngine (){ return _isRemoteIOH;}
	public boolean isLocalAgent (){ return false;} // to be overridden
	public boolean isStopped (){ return _stopped;}
	
	public PlatformExecutor getPlatformExecutor (){ return _exec;}
	public AsyncChannel getChannel (){ return _ssHandler != null ? _ssHandler.getChannel () : null;}
	public Logger getLogger (){ return _logger4j;}
	public ChannelWriter.SendBufferMonitor getSendBufferMonitor (){ return _sendBufferMonitor;}
	public void setSendBufferMonitor (ChannelWriter.SendBufferMonitor monitor){ _sendBufferMonitor = monitor;} // def send buffer monitor can be overridden
	public IOHMeters getIOHMeters (){return _meters;}
	public Meter getLoadMeter (){ return _loadMeter;}
	public String getApplicationParam (String name, String def){
	    String p = _applicationParams.get (name);
	    return p != null ? p : def;
	}
	public int getUID (){ return _uid;}

	public void schedule (Runnable r){
	    _exec.execute (r);
	}
	// to be overridden
	public int getSendBufferSize (){
	    return _ssHandler.getChannel ().getSendBufferSize ();
	}
	public boolean checkSendBuffer (Object argument){
	    return ChannelWriter.check (getSendBufferSize (), _sendBufferMonitor, argument);
	}

	public void drain (){
	    if (_draining) return;
	    _logger.warn (this+" : start draining");
	    try{
		if (!_connected) return;
		if (!_stopped) stop ();
	    }finally{
		_draining = true; // must not be set to true in stop() above
	    }
	}
	public void undrain (){
	    if (!_draining) return;
	    _logger.warn (this+" : un-draining");
	    _draining = false;
	    if (!_connected) return;
	    if (!_stoppedByAgent) start ();
	}
	
	public Map<Integer, IOHChannel> getTcpChannels (){
	    return _myTcpChannels;
	}
	public IOHChannel getTcpChannel (int sock_id){
	    int remoteId = getRemoteIOH (sock_id);
	    if (remoteId != 0){
		RemoteIOHEngine remote = _engine.getRemoteIOHEngines ().get (remoteId);
		if (remote == null) return null;
		return remote.getTcpChannelsByLocalId ().get (sock_id);
	    }
	    Map<Integer, IOHChannel> map = isSharedSocketId (sock_id) ?  _engine.getTcpChannels () : _myTcpChannels;
	    return map.get (sock_id);
	}
	public Map<Integer, IOHChannel> getSctpChannels (){
	    return _mySctpChannels;
	}
	public IOHChannel getSctpChannel (int sock_id){
	    int remoteId = getRemoteIOH (sock_id);
	    if (remoteId != 0){
		RemoteIOHEngine remote = _engine.getRemoteIOHEngines ().get (remoteId);
		if (remote == null) return null;
		return remote.getSctpChannelsByLocalId ().get (sock_id);
	    }
	    Map<Integer, IOHChannel> map = isSharedSocketId (sock_id) ?  _engine.getSctpChannels () : _mySctpChannels;
	    return map.get (sock_id);
	}
	public Map<Integer, IOHChannel> getUdpChannels (){
	    return _myUdpChannels;
	}
	public IOHChannel getUdpChannel (int sock_id){
	    int remoteId = getRemoteIOH (sock_id);
	    if (remoteId != 0){
		RemoteIOHEngine remote = _engine.getRemoteIOHEngines ().get (remoteId);
		if (remote == null) return null;
		return remote.getUdpChannelsByLocalId ().get (sock_id);
	    }
	    Map<Integer, IOHChannel> map = isSharedSocketId (sock_id) ?  _engine.getUdpChannels () : _myUdpChannels;
	    return map.get (sock_id);
	}
	
	public MuxIdentification getMuxIdentification (){
	    return _muxId;
	}
	public String getInstanceName (){ return _instanceName;}
	public String getGroupName (){ return _groupName;}

	public String getAgentId (){
	    return _agentId;
	}

	public void addListener (MuxClientListener listener){
	    _listeners.add (listener);
	}
	public void addListener (MuxClientListener listener, PlatformExecutor exec){
	    _listeners.add (new MuxClientListenerWrapper (listener, exec));
	}
	public void removeListener (MuxClientListener listener){
	    if (_listeners.remove (listener) == false)
		_logger.warn (this+" : MuxClientListener not removed : "+listener);
	}

	public IOHEngine getIOHEngine (){
	    return _engine;
	}

	public Map<String, Object> getProperties (){
	    return _props;
	}

	public MuxHandler getMuxHandler (){
	    return _handler;
	}
	public ExtendedMuxHandler getExtendedMuxHandler (){
	    return _extHandler;
	}
	protected MuxClientState newMuxClientState (){ return new MuxClientState ().stopped (_stopped);}
	protected MuxClientState newMuxClientState (long connectionId){ return newMuxClientState ().connectionId (connectionId);}

	@Override
	public void setMuxVersion (int version){
	    super.setMuxVersion (version);
	    if (getMuxVersion () == (1 << 16)){ // this is an old agent --> need to revert to older MuxVersion
		_logger.warn ("Agent MUX version is 1.0 --> downgrading MUX version");
		_parser.downgrade ();
	    } else
		_parser.upgrade ();
	}

	@Override
	public boolean sendMuxIdentification(MuxIdentification id) {
	    _muxId = id;
	    _applicationParams = getApplicationProperties (id);
	    String instanceName = id.getInstanceName ();
	    int pos = instanceName.indexOf ("__"); // legacy...
	    switch (pos){
	    case -1:
	    case 0 : _groupName = ""; _instanceName = instanceName; break;
	    default: _groupName = instanceName.substring (0, pos); _instanceName = instanceName.substring (pos+2); break;
	    }
	    _aliases.add (_agentId = new StringBuilder ().append (id.getGroupID ()).append ('-').append (id.getAgentID ()).append ('-').append (id.getContainerIndex ()).toString ());
	    if (_logger4j.isInfoEnabled ())
		_logger4j.info (this+" : identified : "+id);
	    _logger4j = Logger.getLogger (new StringBuilder (_logger4j.getName ()).append (".agent.").append (id.getInstanceName ()).toString ());
	    StringBuilder sb = new StringBuilder ()
		.append (_isRemoteIOH ? "IOH[" : (isLocalAgent () ? "Local[" : "Agent[")).append (id.getInstanceName ());
	    String protocol = getApplicationParam ("agent.protocol", null);
	    if (protocol != null)
		sb.append ('.').append (protocol);
	    if (!isLocalAgent ())
		sb.append (((TcpChannel)getChannel ()).getRemoteAddress ());
	    _toString = sb.append (']').toString ();
	    if (_ssHandler != null) _ssHandler.setLogger (_logger4j);
	    register ();
	    return true;
	}
	public boolean sendMuxStart() {
	    _startMeter.inc (1);
	    _stoppedByAgentViaMuxStop = false;
	    _stoppedByAgent = _stoppedByAgentViaLoadMeter;
	    if (_stoppedByAgent){ // still stopped by load meter
		if (!_connected)
		    stop (); // replace with a stop
		return true;
	    }
	    start ();
	    return true;
	}
	public void start (){
	    if (_draining){
		if (_connected){
		    // keep stopped
		} else {
		    stop (); // replace with a stop
		}
		return;
	    }
	    _stopped = false;
	    _stoppedMeter.set (0);
	    if (!_connected){
		agentConnected ();
	    } else {
		_engine.schedule (new Runnable (){
			public void run (){
			    _engine.unstopMuxClient (MuxClient.this);
			}});
	    }
	}
	public boolean sendMuxStop (){
	    _stopMeter.inc (1);
	    _stoppedByAgentViaMuxStop = true;
	    _stoppedByAgent = true;
	    stop ();
	    return true;
	}
	public void stop (){
	    if (_draining){
		if (_connected){
		    // keep stopped
		    return;
		}
	    }
	    _stopped = true;
	    _stoppedMeter.set (1);
	    if (!_connected){
		agentConnected ();
	    } else {
		_engine.schedule (new Runnable (){
			public void run (){
			    _engine.stopMuxClient (MuxClient.this);
			}});
	    }
	}
	protected void agentConnected (){
	    _connected = true;
	    _standbyMeter.set (0);
	    _engine.startMuxClient (this);
	    final MuxClientState state = newMuxClientState ();
	    _engine.schedule (new Runnable (){
		    public void run (){
			_engine.agentConnected (MuxClient.this, state);
		    }});
	}
	public boolean sendMuxData(MuxHeader header, boolean copy, ByteBuffer ... buf) {
	    // this method is here to avoid making the overriding of MuxClient mandatory if a mux payload is used
	    return _engine.sendMuxData (this, header, copy, buf);
	}
	public boolean sendInternalMuxData(MuxHeader header, boolean copy, ByteBuffer ... buf) {
	    switch (header.getFlags ()){
		case FLAG_MUX_METER_VALUE :
		    Meter meter = _meters.setRemoteValue (header);
		    if (meter == _loadMeter){
			if (header.getSessionId () >= _agentLoadMax){ // getSessionId faster than getValue
			    if (_stoppedByAgentViaLoadMeter) return true;
			    _stoppedByAgentViaLoadMeter = true;
			    _stoppedByAgent = true;
			    if (!_connected) return true;
			    stop ();
			} else if (_stoppedByAgentViaLoadMeter){
			    _stoppedByAgentViaLoadMeter = false;
			    _stoppedByAgent = _stoppedByAgentViaMuxStop;
			    if (_stoppedByAgent) return true;
			    if (!_connected) return true;
			    start ();
			}
		    }
		    return true;
		};
	    return false;
	}
	protected void register (){
	    _agentLoadMax = getIntProperty (IOHEngine.PROP_AGENT_LOAD_MAX, _props, Integer.MAX_VALUE); // may be overridden in createDefaultMeters by superclass
	    _meters = instanciateMeters ();
	    createDefaultMeters ();
	    _engine.initMuxClient (this); // meters are available and some may be added
	    if (_loadMeter == null){ // if not set in createDefaultMeters by superclass
		_loadMeter = _engine._voidMeter; // init to avoid NPE
		String loadMeter = getStringProperty (PROP_AGENT_LOAD_METER, _props, "task.scheduled"); // set "" to disable feature
		boolean defMeter = "task.scheduled".equals (loadMeter);
		String loadMon = getStringProperty (PROP_AGENT_LOAD_MONITORABLE, _props, defMeter ? "remote:as.service.concurrent" : "");
		boolean remote = loadMon.startsWith ("remote:");
		if (remote) loadMon = loadMon.substring ("remote:".length ());
		if (loadMeter.length () > 0){ // else keep voidMeter
		    if (remote){			
			int period = getIntProperty (PROP_AGENT_LOAD_PERIOD, _props, 250);
			_loadMeter = _meters.createRemoteMeter (loadMon+":"+loadMeter, loadMon, loadMeter, period);
		    } else {
			_loadMeter = _meters.getMeter (loadMeter);
			if (_loadMeter == null) _loadMeter = _engine._voidMeter;
		    }
		}
		if (_logger.isDebugEnabled ()) _logger.debug (this+" : load meter = "+_loadMeter);
	    }
	    _engine.schedule (new Runnable (){
		    public void run (){
			_engine.registerMuxClient (MuxClient.this);
		    }});
	}
	// may be overridden
	protected IOHMeters instanciateMeters (){
	    StringBuilder sb = new StringBuilder ();
	    if (_isRemoteIOH) sb.append ("agent.ioh.");
	    else if (isLocalAgent ()) sb.append ("agent.local.");
	    else sb.append ("agent.remote.");
	    sb.append (_muxId.getInstanceName ());
	    sb.append ('.').append (_uid);
	    String protocol = getApplicationParam ("agent.protocol", null);	    
	    if (protocol != null) sb.append ('.').append (protocol);
	    sb.append (':').append (_engine.fullName ());
	    StringBuilder desc = new StringBuilder ();
	    if (isLocalAgent ()) desc.append ("local");
	    else desc.append ("from:").append (((TcpChannel)getChannel ()).getRemoteAddress ());
	    return new IOHMeters (sb.toString (), desc.toString (), _engine.getIOHServices ().getMeteringService ());
	}
	protected void createDefaultMeters (){	    
	    _pingMeter = _meters.createIncrementalMeter ("mux.ping", null);
	    _stoppedMeter = _meters.createAbsoluteMeter ("state.stopped");
	    _standbyMeter = _meters.createAbsoluteMeter ("state.standby"); // this meter is set to 1 until the agent is connected
	    _standbyMeter.set (1);
	    _startMeter = _meters.createIncrementalMeter ("mux.start", null);
	    _stopMeter = _meters.createIncrementalMeter ("mux.stop", null);
	    if (_engine.useTcp ()){
		_tcpConnectMeter = _meters.createIncrementalMeter ("mux.tcp.connect", null);
		_tcpCloseMeter = _meters.createIncrementalMeter ("mux.tcp.close", null);
		if (_engine.notifyTcpListen () == false) // else we dont expect listen from agent
		    _tcpListenMeter = _meters.createIncrementalMeter ("mux.tcp.listen", null);
	    }
	    if (_engine.useSctp ()){
		_sctpConnectMeter = _meters.createIncrementalMeter ("mux.sctp.connect", null);
		_sctpCloseMeter = _meters.createIncrementalMeter ("mux.sctp.close", null);
		// we dont expect listen from agent
	    }
	    if (_engine.useUdp ()){
		_udpBindMeter = _meters.createIncrementalMeter ("mux.udp.bind", null);
		_udpCloseMeter = _meters.createIncrementalMeter ("mux.udp.close", null);
	    }
	    _meters.setMuxClientMeters (this);
	}
	
	public int messageReceived(TcpChannel cnx,
				   java.nio.ByteBuffer buff){
	    _pinging = false;
	    while (true){
		MuxParser.MuxCommand command = _parser.parse (buff);
		if (command == null) return 0;
		if (_logger4j.isTraceEnabled () ||
		    (_logger4j.isDebugEnabled () &&
		     !(command instanceof MuxParser.MuxPingCommand) &&
		     !(command instanceof MuxParser.MuxPingAckCommand) &&
		     !(command instanceof MuxParser.InternalDataCommand))
		    ) _logger4j.debug (this+" : RECEIVED : "+command);
		command.run (this);
	    }
	}

	public void writeBlocked(TcpChannel cnx){
	    _logger4j.warn (this+" : writeBlocked");
	}

	public void writeUnblocked(TcpChannel cnx){
	    _logger4j.warn (this+" : writeUnblocked");
	}

	public void connectionClosed(TcpChannel cnx){
	    _logger4j.warn (this+" : connectionClosed");
	    closed ();
	}
	@Override
	public void closed (){
	    super.closed ();
	    if (_muxId == null) return; // not yet registered
	    _engine.resetMuxClient (this);
	    _engine.schedule (new Runnable (){
		    public void run (){
			_engine.unregisterMuxClient (MuxClient.this);
		    }});
	    for (final IOHChannel channel : _myTcpChannels.values ()){
		Runnable r = new Runnable (){
			public void run (){
			    channel.agentClosed (MuxClient.this);
			}};
		channel.schedule (r);
	    }
	    for (final IOHChannel channel : _mySctpChannels.values ()){
		Runnable r = new Runnable (){
			public void run (){
			    channel.agentClosed (MuxClient.this);
			}};
		channel.schedule (r);
	    }
	    for (final IOHChannel channel : _myUdpChannels.values ()){
		Runnable r = new Runnable (){
			public void run (){
			    channel.agentClosed (MuxClient.this);
			}};
		channel.schedule (r);
	    }
	    for (MuxClientListener listener : _listeners)
		listener.agentClosed (this);
	    _myTcpChannels.clear ();
	    _mySctpChannels.clear ();
	    _myUdpChannels.clear ();
	}

	protected boolean _pinging = false;
	public void receiveTimeout(TcpChannel cnx){
	    if (_pinging){
		_logger4j.error (this+" : receiveTimeout : shutdown");
		cnx.shutdown ();
	    }
	    _pinging = true;
	    _parser.sendMuxPingEvent (cnx);
	    _pingMeter.inc (1);
	    if (_logger4j.isDebugEnabled ()) _logger4j.debug (this+" : receiveTimeout : --> ping");
	}
	public void sendMuxPingAck (){
	    _ssHandler.sendMuxPingAck ();
	}

	public boolean infoCommand (TcpChannel client, String arg, Map<Object, Object> map){
	    if (_muxId != null){
		Map info = (Map) map.get (client);
		info.put ("agent", _toString);
	    }
	    return true;
	}

	public boolean stopCommand (TcpChannel client, String arg, Map<Object, Object> map){
	    stop ();
	    map.put ("System.out", "stop : "+this);
	    return true;
	}

	public void close (){
	    _blacklist = false; // we trigger the close : we allow reconnections
	    if (getChannel () != null)
		getChannel ().close ();
	}

	/******************* START implement TCP IO operations **************/
	public boolean sendTcpSocketListen(long listenId, String localIP, int localPort, boolean secure) {
	    _tcpListenMeter.inc (1);
	    final MuxClientState state = newMuxClientState (listenId);
	    _engine.tcpListen (this, state, localIP != null ? new InetSocketAddress (localIP, localPort) : new InetSocketAddress (localPort), secure);
	    return true;
	}
	@Override
	public boolean sendTcpSocketConnect(final long connectionId, final String remoteHost, final int remotePort, String localIP,
					    int localPort, boolean secure, Map<String, String> params) {
	    _tcpConnectMeter.inc (1);
    	    if (localIP == null || localIP.equals(""))
		localIP = getStringProperty (PROP_TCP_CONNECT_FROM, _props, null);
	    InetSocketAddress from = localIP != null ? new InetSocketAddress(localIP, localPort):
		new InetSocketAddress (localPort);
	    
	    final Map<TcpClientOption, Object> opts = new HashMap<TcpClientOption, Object>();
	    opts.put(TcpClientOption.FROM_ADDR, from);

	    opts.put(TcpClientOption.SECURE, secure);
	    
	    if (params != null) opts.put(TcpClientOption.ATTACHMENT, params);
	    final MuxClientState state = newMuxClientState (connectionId);
	    
	    if (needDNS (remoteHost)){
		Runnable r = new Runnable (){
			public void run (){
			    if (_logger.isDebugEnabled ())
				_logger.debug (MuxClient.this+" : need DNS resolution on : "+remoteHost);
			    List<RecordAddress> records = DNSHelper.getHostByName (remoteHost);
			    if (_logger.isDebugEnabled ())
				_logger.debug (MuxClient.this+" : done DNS resolution on : "+remoteHost+" : "+records);
			    if (records.size () > 0)
				_engine.tcpConnect(MuxClient.this, state, new InetSocketAddress(records.get (0).getAddress (), remotePort), opts);
			    else
				_engine.tcpConnect(MuxClient.this, state, new InetSocketAddress(INVALID_DEST_ADDRESS, remotePort), opts);
			}
		    };
		_engine._execs.getThreadPoolExecutor ().execute (r, ExecutorPolicy.SCHEDULE);
	    } else {
		_engine.tcpConnect(this, state, new InetSocketAddress(remoteHost, remotePort), opts);
	    }
	    return true;
	}
	public boolean sendTcpSocketReset(int sockId) {
	    IOHChannel channel = getTcpChannel (sockId);
	    if (channel == null) return false;
	    return channel.shutdown (this);
	}
	public boolean sendTcpSocketClose(int sockId) {
	    // for now we dont allow a client to close a listening socket
	    _tcpCloseMeter.inc (1);
	    IOHChannel channel = getTcpChannel (sockId);
	    if (channel == null) return false;
	    return channel.close (this);
	}
	public boolean sendTcpSocketAbort(int sockId) {
	    // TODO behavior depends on the type of io handler
	    // meant to be overriden
	    return sendTcpSocketClose (sockId);
	}
	public boolean sendTcpSocketData(int sockId, boolean copy, ByteBuffer ... bufs) {
	    IOHChannel channel = getTcpChannel (sockId);
	    if (channel == null) return false;
	    return channel.sendOut (this, null, true, copy, bufs);
	}
	@Override
	public boolean sendTcpSocketParams (int sockId, Map<String, String> params){
	    IOHChannel channel = getTcpChannel (sockId);
	    if (channel == null) return false;
	    channel.applyParamsNow(params);
	    return true;
	}
	/******************* END implement TCP IO operations **************/
	/******************* START implement UDP IO operations **************/
	public boolean sendUdpSocketBind(long bindId, String localIP, int localPort, boolean shared) {
	    // note : we dont expect the shared flag to contradict _sharedUdpBind -> no check
	    _udpBindMeter.inc (1);
	    Map<UdpOption, Object> opts = new HashMap<UdpOption, Object>();
	    final MuxClientState state = newMuxClientState (bindId);
	    _engine.udpBind (this, state, localIP != null ? new InetSocketAddress (localIP, localPort) : new InetSocketAddress (localPort), shared, opts);
	    return true;
	}
	public boolean sendUdpSocketClose(int sockId) {
	    // for now we dont allow a client to close a shared socket
	    _udpCloseMeter.inc (1);
	    if (isSharedSocketId (sockId)) return false;
	    IOHChannel channel = getUdpChannel (sockId);
	    if (channel == null) return false;
	    return channel.close (this);
	}
	public boolean sendUdpSocketData(int sockId, String remoteIP, int remotePort, String virtualIP,
					 int virtualPort, boolean copy, ByteBuffer ... bufs) {
	    IOHChannel channel = getUdpChannel (sockId);
	    if (channel == null) return false;
	    InetSocketAddress addr = new InetSocketAddress (remoteIP, remotePort);
	    return channel.sendOut (this, addr, true, copy, bufs);
	}
	/******************* START implement SCTP IO operations **************/
	@Override
	public boolean sendSctpSocketListen (long listenId, String[] localAddrs, int localPort, int maxOutStreams, int maxInStreams, boolean secure){
	    throw new RuntimeException ("Method not implemented"); // not expected in sctp
	}
	@Override
	public boolean sendSctpSocketConnect(final long connectionId, final java.lang.String remoteHost, final int remotePort, java.lang.String[] localIPs, int localPort, int maxOutStreams, int maxInStreams, boolean secure, Map<SctpSocketOption, SctpSocketParam> options, Map<String, String> params){
	    _sctpConnectMeter.inc (1);
	    boolean localIPsNotSet = localIPs == null ||
		localIPs.length == 0 || // mux parser sent new String[0]
		(localIPs.length == 1 && localIPs[0] == null); // local agent may call new String[1] w/o mux parser normalization
	    String localPrimary = null;
    	    if (localIPsNotSet)
		localPrimary = getStringProperty (PROP_SCTP_CONNECT_FROM, _props, null);
	    else
		localPrimary = localIPs[0];
	    InetSocketAddress localIP = localPrimary != null ? new InetSocketAddress(localPrimary, localPort):
		new InetSocketAddress (localPort);
	    
	    final Map<SctpClientOption, Object> opts = new HashMap<SctpClientOption, Object>();
	    opts.put (SctpClientOption.LOCAL_ADDR, localIP);
	    opts.put (SctpClientOption.MAX_OUT_STREAMS, maxOutStreams);
	    opts.put (SctpClientOption.MAX_IN_STREAMS, maxInStreams);
	    Map<SctpSocketOption, SctpSocketParam> sctpOptions = SctpUtils.createSctpOptions(_props, "ioh.sctp.connect."); // default connect options
	    if (options != null && options.size () > 0){
		for (SctpSocketOption option : options.keySet ()){
		    if (sctpOptions.containsKey (option))
			sctpOptions.put (option, sctpOptions.get (option).merge (options.get (option)));
		    else
			sctpOptions.put (option, options.get (option));
		}
	    }
	    if (sctpOptions.size () > 0){
		opts.put (SctpClientOption.SOCKET_OPTIONS, sctpOptions);
		// now, we need to override max in/out streams
		sctp_initmsg initmsg = (sctp_initmsg) sctpOptions.get(SctpSocketOption.SCTP_INITMSG);
		if (initmsg != null){
		    int i = initmsg.sinit_max_instreams;
		    if (i != 0) opts.put (SctpClientOption.MAX_IN_STREAMS, i);
		    i = initmsg.sinit_num_ostreams;
		    if (i != 0) opts.put (SctpClientOption.MAX_OUT_STREAMS, i);
		}
	    }
	    if (localIPsNotSet){
		List<String> list = Property.getStringListProperty (PROP_SCTP_CONNECT_FROM_SECONDARY, _props);
		if (list != null){
		    InetAddress[] secondaries = new InetAddress[list.size ()];
		    for (int i=0; i<list.size (); i++){
			try{
			    secondaries[i] = InetAddress.getByName (list.get (i));
			}catch(Exception e){
			    _logger.warn (this+" : invalid PROP_SCTP_CONNECT_FROM_SECONDARY : "+list.get (i));
			    // TODO return failed
			    return false;
			}
		    }
		    opts.put (SctpClientOption.SECONDARY_LOCAL_ADDRS, secondaries);
		}
	    } else if (localIPs.length > 1) {
		InetAddress[] secondaries = new InetAddress[localIPs.length - 1];
		for (int i=1; i<localIPs.length; i++){
		    try{
			secondaries[i - 1] = InetAddress.getByName (localIPs[i]);
		    }catch(Exception e){
			_logger.warn (this+" : invalid PROP_SCTP_CONNECT_FROM_SECONDARY : "+localIPs[i]);
			// TODO return failed
			return false;
		    }
		}
		opts.put (SctpClientOption.SECONDARY_LOCAL_ADDRS, secondaries);
	    }

	    // this is a temporary value for this attribute : the existence is checked in IOHSctpClientChannel to set secure boolean
	    if (secure) opts.put (SctpClientOption.SECURITY, Boolean.TRUE);
	    
	    final MuxClientState state = newMuxClientState (connectionId);
	    if (params != null) opts.put (SctpClientOption.ATTACHMENT, params);
	    
	    if (needDNS (remoteHost)){
		Runnable r = new Runnable (){
			public void run (){
			    if (_logger.isDebugEnabled ())
				_logger.debug (MuxClient.this+" : need DNS resolution on : "+remoteHost);
			    List<RecordAddress> records = DNSHelper.getHostByName (remoteHost);
			    if (_logger.isDebugEnabled ())
				_logger.debug (MuxClient.this+" : done DNS resolution on : "+remoteHost+" : "+records);
			    if (records.size () > 0)
				_engine.sctpConnect(MuxClient.this, state, new InetSocketAddress(records.get (0).getAddress (), remotePort), opts);
			    else
				_engine.sctpConnect(MuxClient.this, state, new InetSocketAddress(INVALID_DEST_ADDRESS, remotePort), opts);
			}
		    };
		_engine._execs.getThreadPoolExecutor ().execute (r, ExecutorPolicy.SCHEDULE);
	    } else {
		_engine.sctpConnect(this, state, new InetSocketAddress(remoteHost, remotePort), opts);
	    }
	    return true;
	    
	}
	@Override
	public boolean sendSctpSocketReset(int sockId) {
	    IOHChannel channel = getSctpChannel (sockId);
	    if (channel == null) return false;
	    return channel.shutdown (this);
	}
	@Override
	public boolean sendSctpSocketClose(int sockId) {
	    // for now we dont allow a client to close a listening socket
	    _sctpCloseMeter.inc (1);
	    IOHChannel channel = getSctpChannel (sockId);
	    if (channel == null) return false;
	    return channel.close (this);
	}
	@Override
	public boolean sendSctpSocketData(int sockId, String addr, boolean unordered, boolean complete, int ploadPID, int streamNumber, long timeToLive, boolean copy, ByteBuffer... data){
	    IOHChannel channel = getSctpChannel (sockId);
	    if (channel == null) return false;
	    return channel.sendSctpOut (this, addr, unordered, complete, ploadPID, streamNumber, timeToLive, true, copy, data);
	}
	@Override
	public boolean sendSctpSocketOptions(int sockId, Map<SctpSocketOption, SctpSocketParam> params){
	    IOHChannel channel = getSctpChannel (sockId);
	    if (channel == null){
		_logger.warn (this+" : Cannot set sctp options on sockId="+sockId+" : socket not found");
		return false;
	    }
	    SctpChannel sctpChannel = channel.getChannel ();
	    for (SctpSocketOption option : params.keySet ()){
		try{
		    sctpChannel.setSocketOption (option, params.get (option));
		}catch(Exception e){
		    _logger.error (this+" : Failed to set Sctp param : "+option+" = "+params.get (option)+" on sockId="+sockId);
		}
	    }
	    return true;
	}
	@Override
	public boolean sendSctpSocketParams (int sockId, Map<String, String> params){
	    IOHChannel channel = getSctpChannel (sockId);
	    if (channel == null) return false;
	    channel.applyParamsNow (params);
	    return true;
	}
	/********************* Misc. operations ************************/
	
	public void disableRead(int sockId) {
	    IOHChannel channel = getTcpChannel (sockId);
	    if (channel == null) channel = getUdpChannel (sockId);
	    if (channel == null) return;
	    channel.disableRead (this);
	}
	public void enableRead(int sockId) {
	    IOHChannel channel = getTcpChannel (sockId);
	    if (channel == null) channel = getUdpChannel (sockId);
	    if (channel == null) return;
	    channel.enableRead (this);
	}

	/********************* Gogo Commands ************************/

	public void aliasCommand (TcpChannel cnx, List<String> aliases){
	    if (_muxId != null){
		String instance = _muxId.getInstanceName ();
		aliases.add (instance);
		aliases.add (new StringBuilder ().append (instance).append ('.').append (_muxId.getContainerIndex ()).toString ());
		aliases.add (_muxId.getHostName ());
		aliases.add (String.valueOf (_muxId.getGroupID ()));
		int groupIndex = instance.indexOf ("__");
		if (groupIndex != -1){
		    aliases.add (instance.substring (0, groupIndex));
		    instance = instance.substring (groupIndex+2);
		    aliases.add (instance);
		    aliases.add (new StringBuilder ().append (instance).append ('.').append (_muxId.getContainerIndex ()).toString ());
		}
	    }
	}

	public boolean exitCommand (TcpChannel cnx, final String arg, Map<String, Object> map){
	    _logger.warn (this+" : exitCommand : delay="+arg);
	    long delay = 0L;
	    if (arg != null) delay = Long.parseLong (arg);
	    _engine.sendMuxExit (this, delay);
	    map.put ("System.out", "Sent shutdown command to "+this);
	    return true;
	}
	public boolean killCommand (TcpChannel cnx, final String arg, Map<String, Object> map){
	    _logger.warn (this+" : killCommand");
	    _engine.sendMuxKill (this);
	    map.put ("System.out", "Sent kill command to "+this);
	    return true;
	}
    }
    
    public static interface MuxClientListener {
	public boolean agentConnected (MuxClient agent, MuxClientState state);
	public boolean agentJoined (MuxClient agent, MuxClientState state);
	public boolean agentClosed (MuxClient agent);
	public boolean agentStopped (MuxClient agent);
	public boolean agentUnStopped (MuxClient agent);
    }


    private static class MuxClientListenerWrapper implements MuxClientListener {
	private MuxClientListener _listener;
	private PlatformExecutor _exec;
	private MuxClientListenerWrapper (MuxClientListener listener, PlatformExecutor exec){
	    super ();
	    _listener = listener;
	    _exec = exec;
	}
	public boolean agentConnected (final MuxClient agent, final MuxClientState state){
	    _exec.execute (new Runnable (){
		    public void run (){
			_listener.agentConnected (agent, state);
		    }
		}, ExecutorPolicy.INLINE);
	    return true;
	}
	public boolean agentJoined (final MuxClient agent, final MuxClientState state){
	    _exec.execute (new Runnable (){
		    public void run (){
			_listener.agentJoined (agent, state);
		    }
		}, ExecutorPolicy.INLINE);
	    return true;
	}
	public boolean agentClosed (final MuxClient agent){
	    _exec.execute (new Runnable (){
		    public void run (){
			_listener.agentClosed (agent);
		    }
		}, ExecutorPolicy.INLINE);
	    return true;
	}
	public boolean agentStopped (final MuxClient agent){
	    _exec.execute (new Runnable (){
		    public void run (){
			_listener.agentStopped (agent);
		    }
		}, ExecutorPolicy.INLINE);
	    return true;
	}
	public boolean agentUnStopped (final MuxClient agent){
	    _exec.execute (new Runnable (){
		    public void run (){
			_listener.agentUnStopped (agent);
		    }
		}, ExecutorPolicy.INLINE);
	    return true;
	}
	@Override
	public int hashCode (){ return _listener.hashCode ();}
	@Override
	public boolean equals (Object o){
	    if (o instanceof MuxClientListenerWrapper){
		return this == o;
	    }
	    if (o instanceof MuxClientListener){
		return _listener.equals (o);
	    }
	    return false;
	}
    }

    public boolean getBooleanProperty (String name, boolean def){ return getBooleanProperty (name, _props, def);}
    public static boolean getBooleanProperty (String name, Map props, boolean def){
	if (props == null) return def;
	Object o = props.get (name);
	if (o == null) return def;
	if (def){
	    if (Boolean.FALSE.equals (o) ||
		"false".equals (o)) return false;
	    return true;
	}
	if (Boolean.TRUE.equals (o) ||
		"true".equals (o)) return true;
	return false;
    }
    public int getIntProperty (String name, int def){ return getIntProperty (name, _props, def);}
    public static int getIntProperty (String name, Map props, int def){
	if (props == null) return def;
	Object o = props.get (name);
	if (o == null) return def;
	if (o instanceof Integer) return ((Integer) o).intValue ();
	return Integer.parseInt ((String) o);
    }
    public long getLongProperty (String name, long def){ return getLongProperty (name, _props, def);}
    public static long getLongProperty (String name, Map props, long def){
	if (props == null) return def;
	Object o = props.get (name);
	if (o == null) return def;
	if (o instanceof Number) return ((Number) o).longValue ();
	return Long.parseLong ((String) o);
    }
    public String getStringProperty (String name, String def){ return getStringProperty (name, _props, def);}
    public static String getStringProperty (String name, Map props, String def){
	if (props == null) return def;
	Object o = props.get (name);
	if (o == null) return def;
	return o.toString ();
    }

    public MuxIdentification setApplicationName (MuxIdentification id){
	StringBuilder sb = new StringBuilder ();
	sb.append (_name);
	for (String key: _props.keySet ())
	    if (key.startsWith (PROP_APP_NAME_PREFIX))
		sb.append (';').append (key.substring (PROP_APP_NAME_PREFIX.length ())).append ('=').append (_props.get (key));
	return id.setAppName (sb.toString ());
    }

    public static String getApplicationName (MuxIdentification id){
	if (id == null) return null;
	String name = id.getAppName ();
	int i = name.indexOf (';');
	return i == -1 ? name : name.substring (0, i);
    }

    public static Map<String, String> getApplicationProperties (MuxIdentification id){
	Map<String, String> props = new HashMap<> ();
	if (id == null) return props;
	String name = id.getAppName ();
	String[] toks = name.split (";");
	for (String tok : toks){
	    int i = tok.indexOf ('=');
	    if (i == -1)
		props.put (tok, "");
	    else
		props.put (tok.substring (0, i), tok.substring (i+1));
	}
	return props;
    }
    
    protected static TcpChannelListener VOID_TCP_LISTENER = new TcpChannelListener(){
	    public int messageReceived(TcpChannel cnx,
				       ByteBuffer buff){
		buff.position (buff.limit ());
		return 0;
	    }
	    public void connectionClosed (TcpChannel cnx){}
	    public void receiveTimeout(TcpChannel cnx){}
	    public void writeBlocked(TcpChannel cnx){}
	    public void writeUnblocked(TcpChannel cnx){}
	};

    // criteria not totally OK, but good enough
    protected static boolean needDNS (String hostname){
	char c = hostname.charAt (0);
	if (c < '0' || c > '9'){ // letter
	    return hostname.indexOf (':') == -1; // ipv6 letter is possible
	}
	return false;
    }

    public static class MuxClientState {
	public boolean _stopped;
	public long _connectionId;
	public MuxClientState (){}
	public boolean stopped (){ return _stopped;}
	public MuxClientState stopped (boolean stopped){ _stopped = stopped; return this;}
	public long connectionId (){ return _connectionId;}
	public MuxClientState connectionId (long connectionId){ _connectionId = connectionId; return this;}
    }

}
