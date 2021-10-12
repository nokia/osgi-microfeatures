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

import com.alcatel.as.service.concurrent.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;

import alcatel.tess.hometop.gateways.reactor.*;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.*;
import alcatel.tess.hometop.gateways.reactor.util.FlowController;

import org.apache.log4j.Logger;

import com.nextenso.mux.*;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.mux.impl.*;
import com.alcatel.as.ioh.tools.*;

import com.alcatel.as.ioh.engine.IOHEngine.*;

public class IOHSctpClientChannel extends IOHChannel implements SctpClientChannelListener {
    
    protected long _soTimeout, _createdTime;
    protected Map<MuxClient, Long> _connectingAgents;
    protected boolean _connecting = true, _failed = false;
    protected InetSocketAddress _remote;
    protected SctpChannel _sctpChannel;
    protected List<Runnable> _reconnect;
    
    // called in the agent thread or in the engine thread (if uniqueSctpConnect)
    public IOHSctpClientChannel (MuxClient agent, long connectionId, InetSocketAddress dest, Map<ReactorProvider.SctpClientOption, Object> opts){
	super (agent, agent.getIOHEngine ().sharedSctpConnect ());
	_connectionId = connectionId;
	_soTimeout = IOHEngine.getLongProperty (IOHEngine.PROP_SCTP_CONNECT_READ_TIMEOUT, agent.getProperties (), 0L);
	_secure = opts.get (ReactorProvider.SctpClientOption.SECURITY) != null;
	_remote = dest;
	_toString = new StringBuilder ()
	    .append ("SctpClientChannel[connectionId=").append (connectionId).append (", remote=").append (dest).append (", secure=").append (_secure).append (']')
	    .toString ();
	if (_engine.uniqueSctpConnect ()){
	    _exec = _engine.getPlatformExecutor (); // agentJoined may be scheduled
	    _connectingAgents = new HashMap<> ();
	    _connectingAgents.put (agent, connectionId);
	}
	_logger = _engine.getSctpLogger ();
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : connect");
    }

    public InetSocketAddress getRemoteAddress (){ return _remote; }
    
    // if shared : called in engine thread, else in agent thread
    public void connectionEstablished(SctpChannel cnx){
	history ("connectionEstablished");
	_connecting = false;
	_createdTime = System.currentTimeMillis ();
	makeId ();
	_channel = _sctpChannel = cnx;
	_channel.setSoTimeout (_soTimeout);
	_sendOutBufferMonitor = _engine.getSendSctpBufferMonitor ();
	_sendMeter = _engine.getIOHMeters ().getSendSctpMeter ();
	_readMeter = _engine.getIOHMeters ().getReadSctpMeter ();
	_sendDroppedMeter = _engine.getIOHMeters ().getSendDroppedSctpMeter ();
	cnx.setWriteBlockedPolicy (AsyncChannel.WriteBlockedPolicy.IGNORE);
	_toString = new StringBuilder ()
	    .append ("SctpClientChannel[id=").append (_id).append (", remote=").append (_remote).append (", secure=").append (_secure).append (']')
	    .toString ();
	if (_logger.isEnabledFor (_engine.sctpConnectedLogLevel ()))
	    _logger.log (_engine.sctpConnectedLogLevel (), _engine+" : connected : "+this);
	if (_shared){
	    _engine.getIOHMeters ().getSctpChannelsConnectedMeter ().inc (1);
	    _engine.getIOHMeters ().getOpenSctpChannelsConnectedMeter ().inc (1);
	    _engine.getSctpChannels ().put (_id, this);
	    if (_engine.uniqueSctpConnect ()){
		// we check that all pending agents are still there
		List<MuxClient> removed = new ArrayList<> (_connectingAgents.size ());
		for (MuxClient agent : _connectingAgents.keySet ()){
		    if (_engine.getMuxClientList ().contains (agent) == false)
			removed.add (agent);
		}
		for (MuxClient agent : removed){
		    if (_logger.isDebugEnabled ()) _logger.debug (this+" : agentClosed while connecting : "+agent);
		    agent.getMuxHandler ().sctpSocketConnected (agent, _id, _connectingAgents.get (agent), null, 0, null, 0, 0, 0, false, _secure, MuxUtils.ERROR_UNDEFINED);
		    agent.getIOHMeters ().getFailedSctpChannelsConnectMeter ().inc (1);
		    _connectingAgents.remove (agent);
		}
		if (_connectingAgents.size () == 0){
		    if (_logger.isDebugEnabled ()) _logger.debug (this+" : closing : no agent");
		    close (false, true); // the socket input exec will remain the engine thread --> monothreaded
		    return;
		}
		_agentsList = new MuxClientList ();
		for (MuxClient agent : _connectingAgents.keySet ()){
		    MuxClientState state = new MuxClientState ()
			.stopped (_engine.getMuxClientList ().isDeactivated (agent))
			.connectionId (_connectingAgents.get (agent));
		    agentJoined (agent, state); // consider one day scheduling it in _exec to avoid inlining in connectionEstablished
		}
		_connectingAgents = null; // clean
	    } else {
		if (_engine.getMuxClientList ().size () == 0){ // checking for _agent to see if it is open is too costly
		    if (_logger.isDebugEnabled ()) _logger.debug (this+" : agentClosed while connecting : "+_agent);
		    if (_logger.isDebugEnabled ()) _logger.debug (this+" : closing : no agent");
		    close (false, true); // the socket input exec will remain the engine thread --> monothreaded
		    return;
		}
		_agentsList = _engine.copyMuxClientList ();
		iterateAgentConnected (_agentsList); // consider one day scheduling it in _exec to avoid inlining in connectionEstablished
	    }
	    _agent = null; // clean
	} else {
	    _agent.getIOHMeters ().getSctpChannelsConnectedMeter ().inc (1);
	    _agent.getIOHMeters ().getOpenSctpChannelsConnectedMeter ().inc (1);
	    if (_agent.isOpened () == false){
		close (false, true); // the socket input exec will remain the agent thread --> monothreaded
		return;
	    }
	    _agent.getSctpChannels ().put (_id, this);
	    notifyOpenToAgent (_agent);
	}
	setChannelInputExecutor ();
	_flowController = new FlowController (_channel, 1000, 10000, _exec); // _exec is set in setChannelInputExecutor

	if (_reconnect != null)
	    for (Runnable r: _reconnect) r.run ();
    }

    public void connectionFailed(SctpChannel cnx, java.lang.Throwable error){
	connectionFailed (error);
    }
    public void connectionFailed(java.lang.Throwable error){
	_connecting = false;
	_failed = true;
	_closed = true; // useful : agentJoined callback can already be scheduled and must return false
	if (_shared)
	    _engine.getIOHMeters ().getFailedSctpChannelsConnectMeter ().inc (1);
	else
	    _agent.getIOHMeters ().getFailedSctpChannelsConnectMeter ().inc (1);
	if (_logger.isEnabledFor (_engine.sctpFailedLogLevel ()))
	    _logger.log (_engine.sctpFailedLogLevel (), _engine+" : connectionFailed : remote="+_remote+" : "+error);
	// TODO : check if agent is overloaded --> not useful for now
	if (_engine.uniqueSctpConnect ()){
	    for (MuxClient agent : _connectingAgents.keySet ()){
		agent.getMuxHandler ().sctpSocketConnected (agent, _id, _connectingAgents.get (agent), null, 0, null, 0, 0, 0, false, _secure, MuxUtils.ERROR_UNDEFINED);
		agent.getIOHMeters ().getFailedSctpChannelsConnectMeter ().inc (1);
	    }
	    _engine.unregisterUniqueSctpClientChannel (this);
	} else
	    _agent.getMuxHandler ().sctpSocketConnected (_agent, _id, _connectionId, null, 0, null, 0, 0, 0, false, _secure, MuxUtils.ERROR_UNDEFINED);

	if (_reconnect != null)
	    for (Runnable r: _reconnect) r.run ();
    }

    @Override
    public boolean agentConnected (MuxClient agent, MuxClientState state){
	if (_engine.uniqueSctpConnect ()) return false; // agents must join to be added
	return super.agentConnected (agent, state);
    }
    @Override
    // only applicable to uniqueSctpConnect
    public boolean join (MuxClient agent, MuxClientState state){
	if (_connecting){
	    if (_connectingAgents.get (agent) != null){
		_logger.error (this+" : duplicate agentJoined on connecting socket : "+agent);
		agent.getMuxHandler ().sctpSocketConnected (agent, 0, state.connectionId (), null, 0, null, 0, 0, 0, false, _secure, MuxUtils.ERROR_UNDEFINED);
		agent.getIOHMeters ().getFailedSctpChannelsConnectMeter ().inc (1);
		return true;
	    }
	    if (_logger.isDebugEnabled ()) _logger.debug (this+" : agentJoined while connecting : "+agent+" / "+state.connectionId ());
	    _connectingAgents.put (agent, state.connectionId ());
	    return true;
	} else {
	    if (_failed) return false; // cannot rely on _closed if we are not in the right Exec
	    if (_engine.getPlatformExecutor () == _engine.getCurrentExecutor ()){
		if (_logger.isInfoEnabled ())
		    _logger.info (this+" : join : called in engine exec after connectionEstablished : need to re-schedule");
		throw new IllegalStateException ();
	    }
	    return super.join (agent, state);
	}
    }
    public void joinMismatch (MuxClient agent, MuxClientState state, InetSocketAddress remote, Runnable reconnect){
	if (_connecting){
	    if (_reconnect == null) _reconnect = new ArrayList<> ();
	    _reconnect.add (reconnect);
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : joinMismatch while connecting : "+agent+" / "+state.connectionId ()+" / "+remote);
	} else {
	    // keep it simple for now - we'll let the agent retry
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : joinMismatch after connecting : "+agent+" / "+state.connectionId ()+" / "+remote);
	    agent.getMuxHandler ().sctpSocketConnected (agent, 0, state.connectionId (), null, 0, null, 0, 0, 0, false, _secure, MuxUtils.ERROR_UNDEFINED);
	}
    }
    @Override
    // only applicable to uniqueSctpConnect
    public boolean agentJoined (MuxClient agent, MuxClientState state){
	if (_closed) return false;
	if (_ignoreAgents) return false;
	if (_agentsList.contains (agent)){
	    _logger.error (this+" : duplicate agentJoined on connected socket : "+agent);
	    agent.getMuxHandler ().sctpSocketConnected (agent, 0, state.connectionId (), null, 0, null, 0, 0, 0, false, _secure, MuxUtils.ERROR_UNDEFINED);
	    agent.getIOHMeters ().getFailedSctpChannelsConnectMeter ().inc (1);
	    return true;
	}
	return super.agentJoined (agent, state);
    }
    @Override
    public boolean agentClosed (MuxClient agent){
	if (super.agentClosed (agent) == false) return false;
	if (_shared && _agentsList.size () == 0)
	    close (false, true);
	return true;
    }

    protected void notifyOpenToAgent (MuxClient agent, long connectionId){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyOpenToAgent : "+agent);
	try{
	    InetSocketAddress local = ((SctpChannel)_channel).getLocalAddress();
	    agent.getMuxHandler ().sctpSocketConnected (agent, _id, connectionId, IOHSctpChannel.getAddresses (_sctpChannel.getRemoteAddresses ()), _remote.getPort (), IOHSctpChannel.getAddresses (_sctpChannel.getLocalAddresses ()), local.getPort (), _sctpChannel.getAssociation ().maxOutboundStreams (), _sctpChannel.getAssociation ().maxInboundStreams (), false, _secure, 0);
	    if (_engine.historyChannels ()) // history when we know it is ok
		history ("notifyOpenToAgent : "+agent+" : "+connectionId);
	}catch(Throwable t){ // possible if the channel was closed and we are not yet informed : getRemoteAddresses can thrown a ClosedChannelException synchronously
	    _logger.warn (this+" : exception while notifyOpenToAgent : "+t);
	    agent.getMuxHandler ().sctpSocketConnected (agent, 0, connectionId, null, 0, null, 0, 0, 0, false, _secure, MuxUtils.ERROR_UNDEFINED);
	    // note the agent will get the close : thats dirty but harmless -  ideally: generate a return code for notifyOpenToAgent
	    defaultCloseReason ("Exception while notifying agent");
	    _channel.close ();
	}
	if (_engine.uniqueSctpConnect ()){
	    agent.getIOHMeters ().getSctpChannelsConnectedMeter ().inc (1);
	    agent.getIOHMeters ().getOpenSctpChannelsConnectedMeter ().inc (1);
	}
    }
    protected void notifyCloseToAgent (MuxClient agent){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyCloseToAgent : "+agent);
	// TODO : check if agent is overloaded --> not useful for now
	if (_engine.historyChannels ())
	    history ("notifyCloseToAgent : "+agent);
	agent.getMuxHandler ().sctpSocketClosed (agent, _id);
	if (_engine.uniqueSctpConnect ()){
	    agent.getIOHMeters ().getOpenSctpChannelsConnectedMeter ().inc (-1);
	    agent.getIOHMeters ().getClosedSctpChannelsConnectedMeter ().inc (1);
	}
    }

    // called in agent thread
    @Override
    public boolean close (final MuxClient agent){
	if (_shared == false ||
	    _engine.sharedCloseSctpConnect ()){
	    return super.close (agent); // we close the channel
	} else {
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			notifyCloseToAgent (agent);
			agentClosed (agent);
		    }
		};
	    schedule (r);
	    return true;
	}
    }
    
    public void messageReceived(SctpChannel cnx,
			       java.nio.ByteBuffer buff,
			       java.net.SocketAddress addr,
			       int bytes,
			       boolean isComplete,
			       boolean isUnordered,
			       int ploadPID,
			       int streamNumber){
	if (disabled (buff))
	    return;
	
	_readMeter.inc (buff.remaining ());
	
	MuxClient agent = _agent;
	if (_shared){
	    // should be overriden
	    agent = _agentsList.pick (null); // not sure if meaningful without parsing...
	    if (agent == null){
		buff.position (buff.limit ());
		close ();
		return;
	    }
	}

	logReceived (null, buff);
	
	ByteBuffer copied = ByteBuffer.allocate (buff.remaining ());
	copied.put (buff);
	copied.flip ();
	if (sendAgent (agent, addr, isComplete, isUnordered, ploadPID, streamNumber, true, 0L, false, copied) == false)
	    close ();
    }
    public void sendFailed(SctpChannel cnx,
			   java.net.SocketAddress addr,
			   java.nio.ByteBuffer buf,
			   int errcode,
			   int streamNumber){
	if (_closed) return;
	_logger.warn (this+" : sendFailed : size="+buf.remaining ()+" / errcode="+errcode);
	close ();
    }
    public void peerAddressChanged(SctpChannel cnx,
				   java.net.SocketAddress addr,
				   final SctpChannelListener.AddressEvent event){
	final String addrS = ((InetSocketAddress)addr).getHostString ();
	final int port = ((InetSocketAddress)addr).getPort ();
	final MuxHandler.SctpAddressEvent muxEvent = MuxHandler.SctpAddressEvent.valueOf (event.toString ());
	_agentsList.iterate (new MuxClientList.Iterator (){
		public Object next (MuxClient agent, Object ctx){
		    agent.getMuxHandler ().sctpPeerAddressChanged (agent, _id, addrS, port, muxEvent);
		    return null;}
	    }, null);
    }
    
    public void connectionClosed (SctpChannel cnx, Throwable t){
	if (_logger.isEnabledFor (_engine.sctpClosedLogLevel ())){
	    defaultCloseReason ("Undefined, assumed from server");
	    _logger.log (_engine.sctpClosedLogLevel (), _engine+" : closed : "+this+" : reason : "+_closeReason);
	}
	_engine.getIOHMeters ().getSctpChannelsConnectedDurationMeter ().set (System.currentTimeMillis () - _createdTime);
	if (_shared){
	    _engine.getIOHMeters ().getOpenSctpChannelsConnectedMeter ().inc (-1);
	    _engine.getIOHMeters ().getClosedSctpChannelsConnectedMeter ().inc (1);
	}else{
	    _agent.getIOHMeters ().getOpenSctpChannelsConnectedMeter ().inc (-1);
	    _agent.getIOHMeters ().getClosedSctpChannelsConnectedMeter ().inc (1);
	}
	connectionClosed ();
    }
    public void receiveTimeout(SctpChannel cnx){
	receiveTimeout ();
    }
    public void writeBlocked (SctpChannel cnx){}
    public void writeUnblocked (SctpChannel cnx){}

    public boolean sendOut (MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	throw new RuntimeException ("Method N/A in SCTP");
    }
    @Override
    public boolean sendSctpOut (MuxClient agent, String addr, boolean unordered, boolean complete, int ploadPID, int streamNumber, long timeToLive, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	if (checkBuffer){
	    if (checkSendBufferOut (null) == false){
		_sendDroppedMeter.inc (1);
		_channel.shutdown (); // when remote is overloaded --> we close the socket
		return false;
	    }
	}
	logSendOut (agent, null, buffs);
	_sendMeter.inc (ByteBufferUtils.remaining (buffs));
	_sctpChannel.send (copy, addr != null && addr.length () > 0 ? new InetSocketAddress (addr, _sctpChannel.getRemotePort ()) : null, complete, ploadPID, streamNumber, timeToLive, unordered, buffs);
	return true;
    }
    @Override
    public boolean sendAgent (MuxClient agent, InetSocketAddress from, boolean checkBuffer, long sessionId, boolean copy, ByteBuffer... buffs){
	throw new RuntimeException ("Method N/A in SCTP");
    }
    public boolean sendAgent (MuxClient agent, SocketAddress from, boolean isComplete, boolean isUnordered, int ploadPID, int streamNumber, boolean checkBuffer, long sessionId, boolean copy, ByteBuffer... buffs){
	if (checkBuffer){
	    if (checkSendBufferAgent (agent, null) == false){
		return false;
	    }
	}
	logSendAgent (agent, null, buffs);
	if (copy)
	    agent.getMuxHandler ().sctpSocketData (agent, _id, sessionId, ByteBufferUtils.aggregate (true, true, buffs), ((InetSocketAddress) from).getHostString (), isUnordered, isComplete, ploadPID, streamNumber);
	else
	    agent.getExtendedMuxHandler ().sctpSocketData (agent, _id, sessionId, buffs, ((InetSocketAddress) from).getHostString (), isUnordered, isComplete, ploadPID, streamNumber);
	return true;
    }
    
    public void close (boolean notifyAgent, boolean closeSocket){
	if (_closed) return;
	_closed = true;
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : close :"+notifyAgent+"/"+closeSocket);
	if (closeSocket){
	    defaultCloseReason ("Connection closed by IOH");
	    _channel.close ();
	}
	if (notifyAgent){
	    if (_shared){
		iterateNotifyClose ();
		_agentsList.clear ();
	    } else {
		Runnable r = new Runnable (){
			public void run (){
			    if (_agent.isOpened ()){
				_agent.getSctpChannels ().remove (_id);
				notifyCloseToAgent (_agent);
			    }
			    _engine.releaseSocketId (_id);
			}};
		_agent.schedule (r);
	    }
	}
	// clean
	if (_shared){
	    Runnable r = new Runnable (){
		    public void run (){
			_engine.getSctpChannels ().remove (_id);
			_engine.releaseSocketId (_id);
			if (_engine.uniqueSctpConnect ()){
			    _engine.unregisterUniqueSctpClientChannel (IOHSctpClientChannel.this);
			}
		    }};
	    _engine.schedule (r);
	} else {
	    if (notifyAgent == false){
		_engine.releaseSocketId (_id);
	    }
	}
    }
}
