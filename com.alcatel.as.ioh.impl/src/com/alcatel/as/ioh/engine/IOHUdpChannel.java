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
import com.alcatel.as.service.metering2.*;

import org.apache.log4j.Logger;

import com.nextenso.mux.*;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.mux.impl.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.engine.IOHEngine.*;

public class IOHUdpChannel extends IOHChannel implements UdpChannelListener {
    
    protected volatile long _soTimeout = 0L;
    protected volatile InetSocketAddress _local;
    
    // this one is requested by an agent and is not shared
    // called in the agent thread
    public IOHUdpChannel (MuxClient agent, long bindId, InetSocketAddress local, Map<ReactorProvider.UdpOption, Object> opts){
	super (agent, false);
	_local = local;
	_logger = _engine.getUdpLogger ();
	_connectionId = bindId;
	_soTimeout = (long) IOHEngine.getIntProperty (IOHEngine.PROP_UDP_READ_TIMEOUT, _agent.getProperties (), -1);
	_toString = new StringBuilder ()
	    .append ("UdpChannel[bindId=").append (bindId).append (", local=").append (local).append (']')
	    .toString ();
	_exec = (PlatformExecutor) opts.get (UdpOption.INPUT_EXECUTOR);
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : bind");
    }
    // this one is configure in ioh and remains open all the time
    // this is called in the server thread
    public IOHUdpChannel (IOHEngine engine, Map<String, Object> props){
	super (engine, true);
	_exec = (PlatformExecutor) props.get (Server.PROP_READ_EXECUTOR);
	_logger = _engine.getUdpLogger ();
    }
    // called just after constructor
    public void connectionOpened (final UdpChannel cnx){
	_channel = cnx;
	if (_soTimeout > 0) cnx.setSoTimeout (_soTimeout);
	_sendOutBufferMonitor = _engine.getSendUdpBufferMonitor ();
	_sendMeter = _engine.getIOHMeters ().getSendUdpMeter ();
	_readMeter = _engine.getIOHMeters ().getReadUdpMeter ();
	_sendDroppedMeter = _engine.getIOHMeters ().getSendDroppedUdpMeter ();
	_flowController = new FlowController (_channel, 1000, 10000, _exec);
	cnx.setWriteBlockedPolicy (AsyncChannel.WriteBlockedPolicy.IGNORE);
	if (_shared){
	    _engine.getIOHMeters ().getOpenSharedUdpChannelsMeter ().inc (1);
	    final PlatformExecutor udpServerExec = _engine.getCurrentExecutor ();
	    Runnable r = new Runnable (){
		    public void run (){
			register ();
			Runnable enableReading = new Runnable (){
				public void run (){
				    cnx.enableReading ();
				}
			    };
			// we must make sure that the read is enabled after all the code in the server thread is done
			udpServerExec.execute (enableReading);
		    }
		};
	    _engine.schedule (r);
	} else {
	    _engine.getIOHMeters ().getOpenUnsharedUdpChannelsMeter ().inc (1);
	    makeId ();
	    _toString = new StringBuilder ()
		.append ("UdpChannel[id=").append (_id).append (", local=").append (_channel.getLocalAddress ()).append (']')
		.toString ();
	    _agent.getUdpChannels ().put (_id, this);
	    notifyOpenToAgent (_agent);
	    if (_logger.isInfoEnabled ())
		_logger.info (this+" : connectionOpened");
	    cnx.enableReading ();
	}
    }
    
    // called in the engine thread - applicable to shared only
    protected void register (){
	_id = _engine.registerUdpServer (this);
	_toString = new StringBuilder ()
	    .append ("UdpChannel[id=").append (_id).append (", local=").append (_channel.getLocalAddress ()).append (']')
	    .toString ();
	_agentsList = new MuxClientList ();
	iterateAgentConnected (_engine.getMuxClientList ());
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : connectionOpened");
    }
    // applicable to udp requested by agent only
    // called in agent thread
    public void connectionFailed (UdpChannel channel, Throwable t){
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : connectionFailed : "+t);
	// TODO : check if agent is overloaded --> not useful
	_agent.getMuxHandler ().udpSocketBound (_agent, 0, _local.getAddress ().getHostAddress (), _local.getPort (), _shared, _connectionId, MuxUtils.ERROR_UNDEFINED);
    }
    
    @Override
    public boolean agentConnected (MuxClient agent, MuxClientState state){
	if (_engine.notifyUdpBind () || agent.isRemoteIOHEngine ()) // remoteIOH engine dont request UDP ports - else UDP datagrams are not forwarded to remoteIOH
	    return super.agentConnected (agent, state);
	return false;
    }
    
    protected void notifyOpenToAgent (MuxClient agent, long bindId){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyOpenToAgent : "+agent);
	// TODO : check if agent is overloaded --> not useful !
	agent.getMuxHandler ().udpSocketBound (agent, _id, _channel.getLocalAddress().getAddress ().getHostAddress(), _channel.getLocalAddress ().getPort(), _shared, bindId, 0);
    }
    protected void notifyCloseToAgent (MuxClient agent){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyCloseToAgent : "+agent);
	// TODO : check if agent is overloaded --> not useful !
	agent.getMuxHandler ().udpSocketClosed (agent, _id);
    }
    
    // called in Q-exec of udp channel
    public void close (boolean notifyAgent, boolean closeSocket){
	if (_closed) return;
	_closed = true;
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : close :"+notifyAgent+"/"+closeSocket);
	if (closeSocket){
	    // only for not shared and called when agent is closed
	    _channel.close ();
	}
	if (notifyAgent){
	    // called when channel is closed
	    // may be shared (xml is changed) or not (agent sent close command)
	    if (_shared){
		iterateNotifyClose ();
		_agentsList.clear ();
	    } else {
		Runnable r = new Runnable (){
			public void run (){
			    if (_agent.isOpened ()){
				_agent.getUdpChannels ().remove (_id);
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
			_engine.unregisterUdpServer (IOHUdpChannel.this);
		    }};
	    _engine.schedule (r);
	} else {
	    if (notifyAgent == false){
		_engine.releaseSocketId (_id);
	    }
	}
    }
    
    // called in Q-exec
    public void messageReceived(UdpChannel cnx,
				ByteBuffer buff,
				InetSocketAddress addr){
	if (disabled (buff))
	    return;
	
	_readMeter.inc (buff.remaining ());
	
	MuxClient agent = _agent;
	if (_shared){
	    agent = _agentsList.pick (null);
	    if (agent == null){
		// we dont check it in disabled() since we may want in the case of SIP for ex to return a response 503
		buff.position (buff.limit ());
		return;
	    }
	}
	if (_logger.isDebugEnabled ())
	    logReceived ("RECEIVED FROM "+addr, buff);
		
	ByteBuffer copied = ByteBuffer.allocate (buff.remaining ());
	copied.put (buff);
	copied.flip ();
	sendAgent (agent, addr, true, 0L, false, copied);
    }
    
    public void connectionClosed (UdpChannel cnx){
	if (_shared) _engine.getIOHMeters ().getOpenSharedUdpChannelsMeter ().inc (-1);
	else _engine.getIOHMeters ().getOpenUnsharedUdpChannelsMeter ().inc (-1);
	connectionClosed ();
    }
    public void receiveTimeout(UdpChannel cnx){
	receiveTimeout ();
    }
    public void writeBlocked (UdpChannel cnx){}
    public void writeUnblocked (UdpChannel cnx){}
    
    public boolean sendOut (MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	if (checkBuffer){
	    if (checkSendBufferOut (null) == false){
		_sendDroppedMeter.inc (ByteBufferUtils.remaining (buffs));
		return false; // we dont close the channel
	    }
	}
	logSendOut (agent, to, buffs);
	_sendMeter.inc (ByteBufferUtils.remaining (buffs));
	((UdpChannel) _channel).send (to, copy, buffs);
	return true;
    }
    public boolean sendAgent (MuxClient agent, InetSocketAddress from, boolean checkBuffer, long sessionId, boolean copy, ByteBuffer... buffs){
	if (checkBuffer){
	    if (checkSendBufferAgent (agent, null) == false){
		return false;
	    }
	}
	if (_logger.isDebugEnabled ())
	    logSendAgent (agent, "FORWARD from "+from, buffs);
	if (copy)
	    agent.getMuxHandler ().udpSocketData (agent, _id, sessionId, from.getAddress ().getHostAddress (), from.getPort (), null, 0, ByteBufferUtils.aggregate (true, true, buffs));
	else
	    agent.getExtendedMuxHandler ().udpSocketData (agent, _id, sessionId, from.getAddress ().getHostAddress (), from.getPort (), null, 0, buffs);
	return true;
    }
    
    @Override
    // arg is the remoteAddr
    public void logSendOut (MuxClient agent, Object arg, ByteBuffer... buffs){
	if (_logger.isDebugEnabled ())
	    super.logSendOut (agent, "SEND TO "+arg, buffs);
    }
}
