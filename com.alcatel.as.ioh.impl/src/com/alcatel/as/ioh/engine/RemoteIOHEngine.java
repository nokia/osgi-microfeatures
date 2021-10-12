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

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;

import alcatel.tess.hometop.gateways.reactor.*;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.*;
import alcatel.tess.hometop.gateways.reactor.util.FlowController;

import com.alcatel.as.service.concurrent.*;

import org.apache.log4j.Logger;

import com.nextenso.mux.*;
import com.nextenso.mux.util.MuxIdentification;
import com.nextenso.mux.MuxFactory.ConnectionListener;
import com.nextenso.mux.impl.*;
import com.nextenso.mux.impl.ioh.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClientState;
import com.alcatel.as.ioh.tools.*;

public class RemoteIOHEngine extends MuxHandler implements IOHEngine.MuxClientListener, Runnable {

    protected static ChannelWriter.SendBufferMonitor sendBufferMonitorRemoteIOH = new ChannelWriter.BoundedSendBufferMonitor (1024*1024); // 1MG towards remote IOH
    
    protected static enum State {INIT, CONNECTING, CLOSED_CONNECTING, SCHEDULED, OPEN, CLOSED};
    
    protected int _id;
    protected IOHEngine _localEngine;
    protected MuxClientList _agentsList;
    protected AgentSideMuxConnection _muxConnection;
    protected PlatformExecutor _exec;
    protected Logger _logger;
    protected MuxIdentification _muxId;
    protected ConnectionListener _connListener;
    protected Map<Integer, IOHChannel> _tcpChannelsByRemoteId = new HashMap<> (); // modified / read by remote ioh thread only
    protected Map<Integer, IOHChannel> _sctpChannelsByRemoteId = new HashMap<> (); // modified / read by remote ioh thread only
    protected Map<Integer, IOHChannel> _udpChannelsByRemoteId = new HashMap<> (); // modified / read by remote ioh thread only
    protected Map<Integer, IOHChannel> _tcpChannelsByLocalId = new ConcurrentHashMap<> (); // modified/read by remote ioh thread  -read by agent threads
    protected Map<Integer, IOHChannel> _sctpChannelsByLocalId = new ConcurrentHashMap<> (); // modified/read by remote ioh thread  -read by agent threads
    protected Map<Integer, IOHChannel> _udpChannelsByLocalId = new ConcurrentHashMap<> (); // modified/read by remote ioh thread  -read by agent threads
    protected Map<Integer, IOHChannel> _tcpServerChannelsByRemoteId = new HashMap<> (); // modified / read by remote ioh thread only
    protected Map<Integer, IOHChannel> _sctpServerChannelsByRemoteId = new HashMap<> (); // modified / read by remote ioh thread only
    protected String _toString;
    protected InetSocketAddress _dest;
    protected long _retry;
    protected Future _reconnectTask;
    protected State _state = State.INIT;
    protected FlowController _flowController;
    protected boolean _sentMuxStart;
    
    public RemoteIOHEngine (MuxIdentification id, InetSocketAddress dest, IOHEngine ioh, ConnectionListener listener){
	super ();
	init (0, null, null, null); // set MuxHandler defaults
	getMuxConfiguration ().put(CONF_USE_NIO, Boolean.TRUE);
	getMuxConfiguration ().put(CONF_IPV6_SUPPORT, Boolean.TRUE);
	
	_muxId = id;
	_localEngine = ioh;
	_connListener = listener;
	_dest = dest;
	_logger = _localEngine.getRemoteLogger ();
	_exec = _localEngine.createQueueExecutor ();
	_toString = new StringBuilder ().append ("RemoteIOHEngine[").append (dest).append (']').toString ();
    }
    public MuxClientList copyMuxClientList (){ return new MuxClientList (_agentsList, _localEngine.useMuxAgent ());}
    public MuxClientList getMuxClientList (){ return _agentsList;}
    public void reset (){
	_tcpChannelsByLocalId.clear ();
	_tcpChannelsByRemoteId.clear ();
	_sctpChannelsByLocalId.clear ();
	_sctpChannelsByRemoteId.clear ();
	_udpChannelsByLocalId.clear ();
	_udpChannelsByRemoteId.clear ();
	_tcpServerChannelsByRemoteId.clear ();
	_sctpServerChannelsByRemoteId.clear ();
    }
    public void schedule (Runnable r){
	_exec.execute (r);
    }
    
    // always run in Q-exec
    public void run (){
	connect ();
    }

    // can be called from anywhere
    public RemoteIOHEngine connect (long retry){
	_retry = retry;
	schedule (this);
	return this;
    }
    
    // can be called from anywhere
    public void close (){
	Runnable r = new Runnable (){
		public void run (){
		    if (_logger.isDebugEnabled ()) _logger.debug (RemoteIOHEngine.this+" : close : state="+_state);
		    switch (_state){
		    case INIT : _state = State.CLOSED; return;
		    case CONNECTING: _state = State.CLOSED_CONNECTING; return;
		    case CLOSED_CONNECTING: return;
		    case CLOSED : return;
		    case OPEN:
			getChannel ().close ();
			_state = State.CLOSED;
			return;
		    case SCHEDULED:
			_reconnectTask.cancel (true);
			_reconnectTask = null;
			_state = State.CLOSED;
			return;
		    }
		}
	    };
	schedule (r);
    }
    
    // always called in Q-exec
    protected boolean isOpen (){ return _state == State.OPEN;}
    
    // always run in Q-exec
    protected void connect (){
	_reconnectTask = null;
	if (_state == State.CLOSED) return; // close() called before connect()...
	_state = State.CONNECTING;
	ConnectionListener muxListener = new ConnectionListener (){
		// we cannot use "this" as ConnectionListener : indeed muxClosed collides with MuxHandler.muxClosed
		// muxConnected is called in Q-exec
		public void muxConnected(MuxConnection cnx, Throwable error){
		    if (_logger.isDebugEnabled ()) _logger.debug (RemoteIOHEngine.this+" : muxConnected : "+error+"/"+_state);
		    boolean success = (error == null);
		    switch (_state){
		    case CONNECTING : break;
		    case CLOSED_CONNECTING:
			if (!success){
			    _state = State.CLOSED;
			    return;
			}
			getChannel ().close ();
			return;
		    }
		    if (success){
			_muxConnection = (AgentSideMuxConnection) cnx;
			RemoteIOHEngine.this.connected ();
		    } else {
			if (_connListener != null) _connListener.muxConnected (cnx, error);
			if (_retry > 0){
			    _state = State.SCHEDULED;
			    _reconnectTask = _exec.schedule (RemoteIOHEngine.this, _retry, java.util.concurrent.TimeUnit.MILLISECONDS);
			} else {
			    _state = State.CLOSED;
			}
		    }
		}
		public void muxAccepted(MuxConnection cnx, Throwable error){
		    // not used
		}
		// muxClosed is called in Q-exec
		public void muxClosed(MuxConnection cnx){
		    if (_logger.isInfoEnabled ()) _logger.info (RemoteIOHEngine.this+" : muxClosed : "+_state);
		    switch (_state){
		    case OPEN :
			_state = _retry > 0 ? State.SCHEDULED : State.CLOSED;
			break;
		    case CLOSED:
			break;
		    case CLOSED_CONNECTING:
			_state = State.CLOSED;
			return;
		    }
		    // disconnected() must be called when isOpen return false
		    RemoteIOHEngine.this.disconnected ();
		    if (_connListener != null) _connListener.muxClosed (cnx);
		    _muxConnection = null;
		    if (_state == State.CLOSED)
			return;
		    // we know _state = State.SCHEDULED;
		    _reconnectTask = _exec.schedule (RemoteIOHEngine.this, _retry, java.util.concurrent.TimeUnit.MILLISECONDS);
		}
	    };
	AgentSideMuxConnection muxConnection = new AgentSideMuxConnection (this, muxListener, _logger);
	Map<ReactorProvider.TcpClientOption, Object> opts = new HashMap<> ();
	opts.put(TcpClientOption.TIMEOUT, 3000L);
	opts.put(TcpClientOption.TCP_NO_DELAY, true);
	opts.put(TcpClientOption.INPUT_EXECUTOR, _exec);
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : tcpConnect : "+_dest);
	_localEngine.getIOHServices ().getReactorProvider ().tcpConnect(_localEngine.getReactor (), _dest, muxConnection, opts);
    }

    // called in Q-exec
    public void connected (){
	getChannel ().disableReading ();
	Runnable goToIOHEngine = new Runnable (){
		public void run (){
		    // note : registerRemoteIOHEngine requires exec to be set before
		    _id = _localEngine.registerRemoteIOHEngine (RemoteIOHEngine.this);
		    _toString = new StringBuilder ().append ("RemoteIOHEngine[").append (_id).append (']').toString ();
		    _agentsList = _localEngine.copyMuxClientList ();
		    Runnable returnToRemoteIOHEngine = new Runnable (){
			    public void run (){
				if (_state == State.CLOSED_CONNECTING){
				    disconnected ();
				    getChannel ().close ();
				    return;
				}
				_state = State.OPEN;
				if (_connListener != null) _connListener.muxConnected (_muxConnection, null);
				if (_logger.isInfoEnabled ()) _logger.info (RemoteIOHEngine.this+" : muxConnected : id="+_id);
				if (_logger.isInfoEnabled ()) _logger.info (RemoteIOHEngine.this+" : sendMuxIdentification : "+_muxId);
				_muxConnection.sendMuxIdentification (_muxId);
				_sentMuxStart = false;
				MuxClientList.Iterator it = new MuxClientList.Iterator (){
					public Object next (MuxClient agent, Object ctx){
					    agentConnected (agent, new MuxClientState ()); //TODO BUG : cannot tell here is the agent is stopped / TBD
					    return null;
					}};
				_agentsList.iterate (it, null);
				getChannel ().enableReading ();
				_flowController = new FlowController (getChannel(), 100, 1000, _exec);
			    }};
		    RemoteIOHEngine.this.schedule (returnToRemoteIOHEngine);
		}
	    };
	_localEngine.schedule (goToIOHEngine);
    }
    // called in Q-exec
    public void disconnected (){
	_localEngine.unregisterRemoteIOHEngine (this);
	// the following iterations must be called when isOpen = false
	for (IOHChannel channel : _tcpChannelsByRemoteId.values ()){
	    channel.connectionClosed ();
	}
	for (IOHChannel channel : _sctpChannelsByRemoteId.values ()){
	    channel.connectionClosed ();
	}
	for (IOHChannel channel : _udpChannelsByRemoteId.values ()){
	    channel.connectionClosed ();
	}
	for (IOHChannel server: _tcpServerChannelsByRemoteId.values ())
	    server.connectionClosed ();
	for (IOHChannel server: _sctpServerChannelsByRemoteId.values ())
	    server.connectionClosed ();
	reset ();
    }

    public int id (){ return _id;}
    public String toString (){ return _toString;}
    public IOHEngine getIOHEngine (){ return _localEngine;}
    public PlatformExecutor getPlatformExecutor (){ return _exec;}
    public FlowController getFlowController (){ return _flowController;}
    
    public Map<Integer, IOHChannel> getTcpChannelsByLocalId (){ return _tcpChannelsByLocalId;}
    public Map<Integer, IOHChannel> getSctpChannelsByLocalId (){ return _sctpChannelsByLocalId;}
    public Map<Integer, IOHChannel> getUdpChannelsByLocalId (){ return _udpChannelsByLocalId;}
    public Map<Integer, IOHChannel> getTcpChannelsByRemoteId (){ return _tcpChannelsByRemoteId;}
    public Map<Integer, IOHChannel> getSctpChannelsByRemoteId (){ return _sctpChannelsByRemoteId;}
    public Map<Integer, IOHChannel> getUdpChannelsByRemoteId (){ return _udpChannelsByRemoteId;}
    public Map<Integer, IOHChannel> getTcpServerChannelsByRemoteId (){ return _tcpServerChannelsByRemoteId;}
    public Map<Integer, IOHChannel> getSctpServerChannelsByRemoteId (){ return _sctpServerChannelsByRemoteId;}
    protected TcpChannel getChannel (){ return _muxConnection.getChannel ();}    
    protected MuxConnection getMuxConnection (){ return _muxConnection;}
    
    // called in Q-exec
    public boolean agentConnected (final MuxClient agent, final MuxClientState state){
	if (_state != State.OPEN) return false;
	if (_logger.isInfoEnabled ()) _logger.info (this+" : agentConnected : "+agent);
	if (!_agentsList.iterating ()) _agentsList.add (agent, state);
	if (!_sentMuxStart){
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : sendMuxStart");
	    _muxConnection.sendMuxStart ();
	    _sentMuxStart = true;
	}
	for (final IOHChannel channel : _tcpChannelsByRemoteId.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentConnected (agent, state);
		    }};
	    channel.schedule (r);
	}
	for (final IOHChannel channel : _sctpChannelsByRemoteId.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentConnected (agent, state);
		    }};
	    channel.schedule (r);
	}
	for (final IOHChannel channel : _udpChannelsByRemoteId.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentConnected (agent, state);
		    }};
	    channel.schedule (r);
	}
	for (IOHChannel server: _tcpServerChannelsByRemoteId.values ())
	    server.agentConnected (agent, state);
	for (IOHChannel server: _sctpServerChannelsByRemoteId.values ())
	    server.agentConnected (agent, state);
	return true;
    }
    // called in Q-exec
    public boolean agentJoined (MuxClient agent, MuxClientState state){
	// not used
	return false;
    }
    // called in Q-exec
    public boolean agentClosed (final MuxClient agent){
	if (_state != State.OPEN) return false;
	if (_logger.isInfoEnabled ()) _logger.info (this+" : agentClosed : "+agent);
	_agentsList.remove (agent); // dont need to check the return value : assume true
	for (final IOHChannel channel : _tcpChannelsByRemoteId.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentClosed (agent);
		    }};
	    channel.schedule (r);
	}
	for (final IOHChannel channel : _sctpChannelsByRemoteId.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentClosed (agent);
		    }};
	    channel.schedule (r);
	}
	for (final IOHChannel channel : _udpChannelsByRemoteId.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentClosed (agent);
		    }};
	    channel.schedule (r);
	}
	for (IOHChannel server: _tcpServerChannelsByRemoteId.values ())
	    server.agentClosed (agent);
	for (IOHChannel server: _sctpServerChannelsByRemoteId.values ())
	    server.agentClosed (agent);
	if (_agentsList.size () == 0){
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : no more agents : disconnect");
	    getChannel ().close ();
	}
	return true;
    }
    // called in Q-exec
    public boolean agentStopped (final MuxClient agent){
	if (_state != State.OPEN) return false;
	if (_logger.isInfoEnabled ()) _logger.info (this+" : agentStopped : "+agent);
	_agentsList.deactivate (agent); // dont need to check the return value : assume true
	for (final IOHChannel channel : _tcpChannelsByRemoteId.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentStopped (agent);
		    }};
	    channel.schedule (r);
	}
	for (final IOHChannel channel : _sctpChannelsByRemoteId.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentStopped (agent);
		    }};
	    channel.schedule (r);
	}
	for (final IOHChannel channel : _udpChannelsByRemoteId.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentStopped (agent);
		    }};
	    channel.schedule (r);
	}
	for (IOHChannel server: _tcpServerChannelsByRemoteId.values ())
	    server.agentStopped (agent);
	for (IOHChannel server: _sctpServerChannelsByRemoteId.values ())
	    server.agentStopped (agent);
	// TBD send MuxStop if all agents are stopped ?
	return true;
    }
    // called in Q-exec
    public boolean agentUnStopped (final MuxClient agent){
	if (_state != State.OPEN) return false;
	if (_logger.isInfoEnabled ()) _logger.info (this+" : agentUnStopped : "+agent);
	_agentsList.reactivate (agent); // dont need to check the return value : assume true
	for (final IOHChannel channel : _tcpChannelsByRemoteId.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentUnStopped (agent);
		    }};
	    channel.schedule (r);
	}
	for (final IOHChannel channel : _sctpChannelsByRemoteId.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentUnStopped (agent);
		    }};
	    channel.schedule (r);
	}
	for (final IOHChannel channel : _udpChannelsByRemoteId.values ()){
	    Runnable r = new Runnable (){
		    public void run (){
			channel.agentUnStopped (agent);
		    }};
	    channel.schedule (r);
	}
	for (IOHChannel server: _tcpServerChannelsByRemoteId.values ())
	    server.agentUnStopped (agent);
	for (IOHChannel server: _sctpServerChannelsByRemoteId.values ())
	    server.agentUnStopped (agent);
	// TBD send MuxStart if a MuxStop was sent
	return true;
    }
    // not used
    public int[] getCounters(){return null;}
    // not used
    public int getMajorVersion(){return -1;}
    // not used
    public int getMinorVersion(){return -1;}

    /************************* tcp socket mgmt *************************/

    protected Map<Integer, Integer> _tcpIds = new HashMap<> ();
    @Override
    public void tcpSocketListening(MuxConnection connection, int sockId, String localIP, int localPort,
				   boolean secure, long listenId, int errno)
    {
	new RemoteIOHTcpServerChannel (this).listening (sockId, localIP, localPort, secure, listenId);
    }
    @Override
    public void tcpSocketConnected(MuxConnection connection, int sockId, String remoteIP, int remotePort,
				   String localIP, int localPort, String virtualIP, int virtualPort,
				   boolean secure, boolean clientSocket, long connectionId, int errno)
    {
	new RemoteIOHTcpChannel (this).connected (sockId, remoteIP, remotePort, localIP, localPort, secure, clientSocket, connectionId);
    }
    @Override
    public void tcpSocketClosed(MuxConnection connection, int sockId)
    {
	IOHChannel tcp = _tcpChannelsByRemoteId.get (sockId);
	if (tcp != null){
	    tcp.connectionClosed ();
	    return;
	}
	tcp = _tcpServerChannelsByRemoteId.get (sockId);
	if (tcp != null){
	    tcp.connectionClosed ();
	}
    }
    @Override
    public void tcpSocketAborted(MuxConnection connection, int sockId)
    {
	IOHChannel tcp = _tcpChannelsByRemoteId.get (sockId);
	if (tcp != null){
	    tcp.connectionAborted ();
	}
    }
    @Override
    public void tcpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer data)
    {
	IOHChannel tcp = _tcpChannelsByRemoteId.get (sockId);
	if (tcp != null) ((RemoteIOHTcpChannel)tcp).received (sessionId, data);
    }
    
    /************************* udp socket mgmt *************************/
    
    @Override
    public void udpSocketBound(MuxConnection connection,
			       int sockId,
			       String localIP,
			       int localPort,
			       boolean shared,
			       long bindId,
			       int errno){
	new RemoteIOHUdpChannel (this).bound (sockId, localIP, localPort, bindId);
    }
    @Override
    public void udpSocketClosed (MuxConnection connection, int sockId){
	final IOHChannel udp = _udpChannelsByRemoteId.get (sockId);
	if (udp != null){
	    udp.connectionClosed ();
	}
    }
    @Override
    public void udpSocketData(MuxConnection connection,
			      int sockId,
			      long sessionId,
			      String remoteIP,
			      int remotePort,
			      String virtualIP,
			      int virtualPort,
			      ByteBuffer buff){
	IOHChannel udp = _udpChannelsByRemoteId.get (sockId);
	if (udp != null) ((RemoteIOHUdpChannel)udp).received (sessionId, remoteIP, remotePort, buff);
    }

    /************************* sctp socket mgmt *************************/

    public void sctpSocketListening(MuxConnection connexion, int sockId, long listenerId, String[] localAddrs, int localPort, boolean secure, int errno){
	new RemoteIOHSctpServerChannel (this).listening (sockId, localAddrs, localPort, listenerId, secure);
    }

    public void sctpSocketConnected(MuxConnection connection, int sockId, long connectionId, String[] remoteAddrs,
				    int remotePort, String[] localAddrs, int localPort, int maxOutStreams,
				    int maxInStreams, boolean fromClient, boolean secure, int errno){
	new RemoteIOHSctpChannel (this).connected (sockId, remoteAddrs, remotePort, localAddrs, localPort, maxOutStreams, maxInStreams, fromClient, connectionId, secure);
    }
	    
    public void sctpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer data, String addr,
			       boolean isUnordered, boolean isComplete, int ploadPID, int streamNumber){
	IOHChannel sctp = _sctpChannelsByRemoteId.get (sockId);
	if (sctp != null) ((RemoteIOHSctpChannel)sctp).received (sessionId, data, addr, isUnordered, isComplete, ploadPID, streamNumber);
    }

    public void sctpSocketClosed(MuxConnection connection, int sockId){
	IOHChannel sctp = _sctpChannelsByRemoteId.get (sockId);
	if (sctp != null){
	    sctp.connectionClosed ();
	    return;
	}
	sctp = _sctpServerChannelsByRemoteId.get (sockId);
	if (sctp != null){
	    sctp.connectionClosed ();
	}
    }

    public void sctpSocketSendFailed(MuxConnection connection, int sockId, String addr, int streamNumber, ByteBuffer buf, int errcode){
	IOHChannel sctp = _sctpChannelsByRemoteId.get (sockId);
	if (sctp != null) ((RemoteIOHSctpChannel)sctp).sendFailed (addr, streamNumber, buf, errcode);
    }
    
    public void sctpPeerAddressChanged(MuxConnection cnx, int sockId, String addr, int port, SctpAddressEvent event) {
	IOHChannel sctp = _sctpChannelsByRemoteId.get (sockId);
	if (sctp != null) ((RemoteIOHSctpChannel)sctp).peerAddressChanged (addr, port, event);
    }
}
