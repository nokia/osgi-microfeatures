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

public class IOHTcpClientChannel extends IOHChannel implements TcpClientChannelListener {
    
    protected long _soTimeout, _createdTime;
    protected Map<MuxClient, Long> _connectingAgents;
    protected boolean _connecting = true, _failed = false;
    protected InetSocketAddress _remote, _local;
    
    // called in the agent thread or in the engine thread (if uniqueTcpConnect)
    public IOHTcpClientChannel (MuxClient agent, long connectionId, InetSocketAddress dest, Map<ReactorProvider.TcpClientOption, Object> opts){
	super (agent, agent.getIOHEngine ().sharedTcpConnect ());
	_connectionId = connectionId;
	_soTimeout = IOHEngine.getLongProperty (IOHEngine.PROP_TCP_CONNECT_READ_TIMEOUT, agent.getProperties (), 0L);
	
	if(opts.containsKey(TcpClientOption.SECURE)) {
		_secure = (Boolean) opts.get (TcpClientOption.SECURE);
	} else {
		_secure = opts.containsKey(TcpClientOption.SECURITY);
	}
	_local = (InetSocketAddress) opts.get (TcpClientOption.FROM_ADDR); // used in join when connecting
	_remote = dest;
	_toString = new StringBuilder ()
	    .append ("TcpClientChannel[connectionId=").append (connectionId).append (", remote=").append (dest).append (']')
	    .toString ();
	if (_engine.uniqueTcpConnect ()){
	    _exec = _engine.getPlatformExecutor (); // agentJoined may be scheduled
	    _connectingAgents = new HashMap<> ();
	    _connectingAgents.put (agent, connectionId);
	}
	_logger = _engine.getTcpLogger ();
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : connect");
    }

    public InetSocketAddress getRemoteAddress (){ return _remote; }

    @Override
    public InetSocketAddress getLocalAddress (){ return _local;}
    
    // if shared : called in engine thread, else in agent thread
    public void connectionEstablished(TcpChannel cnx){
	history ("connectionEstablished");
	_connecting = false;
	_createdTime = System.currentTimeMillis ();
	makeId ();
	_channel = cnx;
	_local = cnx.getLocalAddress();
	_channel.setSoTimeout (_soTimeout);
	_sendOutBufferMonitor = _engine.getSendTcpBufferMonitor ();
	_sendMeter = _engine.getIOHMeters ().getSendTcpMeter ();
	_readMeter = _engine.getIOHMeters ().getReadTcpMeter ();
	_sendDroppedMeter = _engine.getIOHMeters ().getSendDroppedTcpMeter ();
	cnx.setWriteBlockedPolicy (AsyncChannel.WriteBlockedPolicy.IGNORE);
	_toString = new StringBuilder ()
	    .append ("TcpClientChannel[id=").append (_id).append (", remote=").append (_remote).append (']')
	    .toString ();
	if (_logger.isEnabledFor (_engine.tcpConnectedLogLevel ()))
	    _logger.log (_engine.tcpConnectedLogLevel (), _engine+" : connected : "+this);
	if (_shared){
	    _engine.getIOHMeters ().getTcpChannelsConnectedMeter ().inc (1);
	    _engine.getIOHMeters ().getOpenTcpChannelsConnectedMeter ().inc (1);
	    _engine.getTcpChannels ().put (_id, this);
	    if (_engine.uniqueTcpConnect ()){
		// we check that all pending agents are still there
		// NOTE : this implies that an agent requesting a connection has already been registered by the IOH
		// --> means that the agent sent a MuxStart before requesting a connection !
		// FYI : diameter agent bug fixed : sent a connection request before sendMuxStart : so the connecting agent looked closed !
		List<MuxClient> removed = new ArrayList<> (_connectingAgents.size ());
		for (MuxClient agent : _connectingAgents.keySet ()){
		    if (_engine.getMuxClientList ().contains (agent) == false)
			removed.add (agent);
		}
		for (MuxClient agent : removed){
		    if (_logger.isDebugEnabled ()) _logger.debug (this+" : agentClosed while connecting : "+agent);
		    agent.getMuxHandler ().tcpSocketConnected (agent, 0, _remote.getAddress().getHostAddress(), _remote.getPort(), _local.getAddress ().getHostAddress (), _local.getPort (), null, 0, _secure, false, _connectingAgents.get (agent), MuxUtils.ERROR_UNDEFINED);
		    agent.getIOHMeters ().getFailedTcpChannelsConnectMeter ().inc (1);
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
	    _agent.getIOHMeters ().getTcpChannelsConnectedMeter ().inc (1);
	    _agent.getIOHMeters ().getOpenTcpChannelsConnectedMeter ().inc (1);
	    if (_agent.isOpened () == false){
		close (false, true); // the socket input exec will remain the agent thread --> monothreaded
		return;
	    }
	    _agent.getTcpChannels ().put (_id, this);
	    notifyOpenToAgent (_agent);
	}
	setChannelInputExecutor ();
	_flowController = new FlowController (_channel, 1000, 10000, _exec); // _exec is set in setChannelInputExecutor
    }

    public void connectionFailed(TcpChannel cnx, java.lang.Throwable error){
	_local = cnx.getLocalAddress(); // we update (may be set by linux)
	connectionFailed (error);
    }
    public void connectionFailed(java.lang.Throwable error){
	_connecting = false;
	_failed = true;
	_closed = true; // useful : agentJoined callback can already be scheduled and must return false
	if (_shared)
	    _engine.getIOHMeters ().getFailedTcpChannelsConnectMeter ().inc (1);
	else
	    _agent.getIOHMeters ().getFailedTcpChannelsConnectMeter ().inc (1);	
	if (_logger.isEnabledFor (_engine.tcpFailedLogLevel ()))
	    _logger.log (_engine.tcpFailedLogLevel (), _engine+" : connectionFailed : local="+_local+", remote="+_remote+" : "+error);
	// TODO : check if agent is overloaded --> not useful for now
	if (_engine.uniqueTcpConnect ()){
	    for (MuxClient agent : _connectingAgents.keySet ()){
		agent.getMuxHandler ().tcpSocketConnected (agent, 0, _remote.getAddress().getHostAddress(), _remote.getPort(), _local.getAddress().getHostAddress(), _local.getPort (), null, 0, _secure, false, _connectingAgents.get (agent), MuxUtils.ERROR_UNDEFINED);
		agent.getIOHMeters ().getFailedTcpChannelsConnectMeter ().inc (1);
	    }
	    _engine.unregisterUniqueTcpClientChannel (this);
	} else
	    _agent.getMuxHandler ().tcpSocketConnected (_agent, 0, _remote.getAddress().getHostAddress(), _remote.getPort(), _local.getAddress().getHostAddress(), _local.getPort (), null, 0, _secure, false, _connectionId, MuxUtils.ERROR_UNDEFINED);
    }

    @Override
    public boolean agentConnected (MuxClient agent, MuxClientState state){
	if (_engine.uniqueTcpConnect ()) return false; // agents must join to be added
	return super.agentConnected (agent, state);
    }
    @Override
    // only applicable to uniqueTcpConnect
    public boolean join (MuxClient agent, MuxClientState state){
	if (_connecting){
	    if (_connectingAgents.get (agent) != null){
		_logger.error (this+" : duplicate agentJoined on connecting socket : "+agent);
		agent.getMuxHandler ().tcpSocketConnected (agent, 0, _remote.getAddress().getHostAddress(), _remote.getPort(), _local.getAddress ().getHostAddress (), _local.getPort (), null, 0, _secure, false, state.connectionId (), MuxUtils.ERROR_UNDEFINED);
		agent.getIOHMeters ().getFailedTcpChannelsConnectMeter ().inc (1);
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
    @Override
    // only applicable to uniqueTcpConnect
    public boolean agentJoined (MuxClient agent, MuxClientState state){
	if (_closed) return false;
	if (_ignoreAgents) return false;
	if (_agentsList.contains (agent)){
	    _logger.error (this+" : duplicate agentJoined on connected socket : "+agent);
	    agent.getMuxHandler ().tcpSocketConnected (agent, 0, _remote.getAddress().getHostAddress(), _remote.getPort(), "0.0.0.0", 0, null, 0, _secure, false, state.connectionId (), MuxUtils.ERROR_UNDEFINED);
	    agent.getIOHMeters ().getFailedTcpChannelsConnectMeter ().inc (1);
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
	// TODO : check if agent is overloaded --> not useful for now
	if (_engine.historyChannels ())
	    history ("notifyOpenToAgent : "+agent+" : "+connectionId);
	agent.getMuxHandler ().tcpSocketConnected (agent, _id, _remote.getAddress().getHostAddress(), _remote.getPort(), _local.getAddress().getHostAddress(), _local.getPort (), null, 0, _secure, false, connectionId, 0);
	if (_engine.uniqueTcpConnect ()){
	    agent.getIOHMeters ().getTcpChannelsConnectedMeter ().inc (1);
	    agent.getIOHMeters ().getOpenTcpChannelsConnectedMeter ().inc (1);
	}
    }
    protected void notifyCloseToAgent (MuxClient agent){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyCloseToAgent : "+agent);
	// TODO : check if agent is overloaded --> not useful for now
	if (_engine.historyChannels ())
	    history ("notifyCloseToAgent : "+agent);
	agent.getMuxHandler ().tcpSocketClosed (agent, _id);
	if (_engine.uniqueTcpConnect ()){
	    agent.getIOHMeters ().getOpenTcpChannelsConnectedMeter ().inc (-1);
	    agent.getIOHMeters ().getClosedTcpChannelsConnectedMeter ().inc (1);
	}
    }

    // called in agent thread
    @Override
    public boolean close (final MuxClient agent){
	if (_shared == false ||
	    _engine.sharedCloseTcpConnect ()){
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
    
    public int messageReceived(TcpChannel cnx,
			       ByteBuffer buff){
	if (disabled (buff))
	    return 0;
	
	_readMeter.inc (buff.remaining ());
	
	MuxClient agent = _agent;
	if (_shared){
	    // should be overriden
	    agent = _agentsList.pick (null); // not sure if meaningful without parsing...
	    if (agent == null){
		buff.position (buff.limit ());
		close ();
		return 0;
	    }
	}

	logReceived (null, buff);
	
	ByteBuffer copied = ByteBuffer.allocate (buff.remaining ());
	copied.put (buff);
	copied.flip ();
	if (sendAgent (agent, null, true, 0L, false, copied) == false)
	    close ();
	return 0;
    }
    
    public void connectionClosed (TcpChannel cnx){
	if (_logger.isEnabledFor (_engine.tcpClosedLogLevel ())){
	    defaultCloseReason ("Undefined, assumed from server");
	    _logger.log (_engine.tcpClosedLogLevel (), _engine+" : closed : "+this+" : reason : "+_closeReason);
	}
	_engine.getIOHMeters ().getTcpChannelsConnectedDurationMeter ().set (System.currentTimeMillis () - _createdTime);
	if (_shared){
	    _engine.getIOHMeters ().getOpenTcpChannelsConnectedMeter ().inc (-1);
	    _engine.getIOHMeters ().getClosedTcpChannelsConnectedMeter ().inc (1);
	}else{
	    _agent.getIOHMeters ().getOpenTcpChannelsConnectedMeter ().inc (-1);
	    _agent.getIOHMeters ().getClosedTcpChannelsConnectedMeter ().inc (1);
	}
	connectionClosed ();
    }
    public void receiveTimeout(TcpChannel cnx){
	receiveTimeout ();
    }
    public void writeBlocked (TcpChannel cnx){}
    public void writeUnblocked (TcpChannel cnx){}
    
    public boolean sendOut (MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	if (checkBuffer){
	    if (checkSendBufferOut (null) == false){
		_sendDroppedMeter.inc (1);
		_channel.shutdown (); // when remote is overloaded --> we close the socket
		return false;
	    }
	}
	logSendOut (agent, null, buffs);
	_sendMeter.inc (ByteBufferUtils.remaining (buffs));
	_channel.send (buffs, copy);
	return true;
    }
    public boolean sendAgent (MuxClient agent, InetSocketAddress from, boolean checkBuffer, long sessionId, boolean copy, ByteBuffer... buffs){
	if (checkBuffer){
	    if (checkSendBufferAgent (agent, null) == false){
		return false;
	    }
	}
	logSendAgent (agent, null, buffs);
	if (copy)
	    agent.getMuxHandler ().tcpSocketData (agent, _id, sessionId, ByteBufferUtils.aggregate (true, true, buffs));
	else
	    agent.getExtendedMuxHandler ().tcpSocketData (agent, _id, sessionId, buffs);
	return true;
    }
    
    private static final String TCP_PARAMS_UPGRADE_SECURE = "tcp.secure.upgrade";

    @Override
    protected void applyParamsNow(Map<String, String> params) {
    	if(params.containsKey(TCP_PARAMS_UPGRADE_SECURE)) {
    		TcpChannel chan = getChannel();
    		try {
				chan.upgradeToSecure();
				if (_logger.isInfoEnabled ())
				    _logger.info (this+" upgraded to secure");
			} catch (Exception e) {
				_logger.warn(this + " upgrade to secure failed!", e);
			}
    	}
    	super.applyParamsNow(params);
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
				_agent.getTcpChannels ().remove (_id);
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
			_engine.getTcpChannels ().remove (_id);
			_engine.releaseSocketId (_id);
			if (_engine.uniqueTcpConnect ()){
			    _engine.unregisterUniqueTcpClientChannel (IOHTcpClientChannel.this);
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
