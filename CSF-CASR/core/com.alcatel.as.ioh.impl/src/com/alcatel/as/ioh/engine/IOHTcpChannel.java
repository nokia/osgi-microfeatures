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

public class IOHTcpChannel extends IOHChannel implements TcpChannelListener, Runnable {
    
    protected PlatformExecutor _serverExec;
    protected long _createdTime;
    protected boolean _rejected;

    // called in the server thread
    public IOHTcpChannel (IOHEngine engine, TcpChannel channel, Map<String, Object> props){
	super (engine, engine.sharedTcpAccept ());
	_createdTime = System.currentTimeMillis ();
	_exec = (PlatformExecutor) props.get (Server.PROP_READ_EXECUTOR);
	_serverExec = engine.getCurrentExecutor ();
	_logger = _engine.getTcpLogger ();
	_channel = channel;
	_secure = channel.isSecure ();
	_sendOutBufferMonitor = _engine.getSendTcpBufferMonitor ();
	_sendMeter = _engine.getIOHMeters ().getSendTcpMeter ();
	_readMeter = _engine.getIOHMeters ().getReadTcpMeter ();
	_sendDroppedMeter = _engine.getIOHMeters ().getSendDroppedTcpMeter ();
	_flowController = new FlowController (_channel, 1000, 10000, _exec);
	channel.setWriteBlockedPolicy (AsyncChannel.WriteBlockedPolicy.IGNORE);
    }
    // called just after the constructor (which has time to finish)
    public IOHTcpChannel accepted (){
	if (_rejected) return this;
	_engine.getIOHMeters ().getTcpChannelsAcceptedMeter ().inc (1);
	_engine.getIOHMeters ().getOpenTcpChannelsAcceptedMeter ().inc (1);
	_engine.schedule (this);
	return this;
    }
    public void reject (){
	// used when the socket is rejected in the constructor : so accepted is not called
	_rejected = true;
	_closed = true; // inhibit messageReceived callback
	defaultCloseReason ("Connection rejected");
	_channel.close ();
    }
    public void run (){
	register ();
    }
    // called in the engine thread
    protected void register (){
	makeId ();
	_toString = new StringBuilder ()
	    .append ("TcpChannel[id=").append (_id).append (", remote=").append (((TcpChannel)_channel).getRemoteAddress ()).append (']')
	    .toString ();
	if (_logger.isEnabledFor (_engine.tcpAcceptedLogLevel ()))
	    // note : we dont log when the socket is rejected, but a specific log is expected in this case
	    _logger.log (_engine.tcpAcceptedLogLevel (), _engine+" : accepted : "+this);
	if (checkAgentsSize () == false)
	    return;
	// NOTE : we could limit to agents which were notified of the TcpServer on which this socket was accepted
	// we dont do it for now since it seems useless --> TODO is it ???
	if (_shared){
	    _engine.getTcpChannels ().put (_id, this);
	    _agentsList = _engine.copyMuxClientList ();
	    iterateAgentConnected (_agentsList);
	    Runnable enableReading = new Runnable (){
		    public void run (){
			_channel.enableReading ();
		    }};
	    _serverExec.execute (enableReading);
	    _serverExec = null;
	} else {
	    assignToAgent ();
	}
    }
    // called in the engine thread
    protected boolean checkAgentsSize (){
	// if _shared=false, we need at least 1 active agent
	// if _shared=true, we accept a deactivated agent (override if needed)
	int size = _shared ? _engine.getMuxClientList ().size () : _engine.getMuxClientList ().sizeOfActive ();
	if (size == 0){
	    if (_logger.isInfoEnabled ())
		_logger.info (this+" : closing : no agent connected");
	    _channel.setInputExecutor (_serverExec); // we keep monothreaded
	    close (false, true);
	    return false;
	}
	return true;
    }
    // called in the engine thread
    protected void assignToAgent (){
	_agent = _engine.getMuxClientList ().pick (null);
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : attaching to : "+_agent);
	Runnable r = new Runnable (){
		public void run (){
		    if (_agent.isOpened () == false){
			if (_logger.isInfoEnabled ())
			    _logger.info (this+" : cannot attach to : "+_agent+" : closed - retrying");
			Runnable r = new Runnable (){
				public void run (){
				    _agent = null; // reset to null for meters if close() is called
				    if (checkAgentsSize ())
					assignToAgent ();
				}};
			_engine.schedule (r);
			return;
		    }
		    _agent.getTcpChannels ().put (_id, IOHTcpChannel.this);
		    _agent.getIOHMeters ().getTcpChannelsAcceptedMeter ().inc (1);
		    _agent.getIOHMeters ().getOpenTcpChannelsAcceptedMeter ().inc (1);
		    notifyOpenToAgent (_agent);
		    Runnable enableReading = new Runnable (){
			    public void run (){
				_channel.enableReading ();
			    }};
		    _serverExec.execute (enableReading);
		    _serverExec = null;
		}};
	_agent.schedule (r);
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
	InetSocketAddress remote = ((TcpChannel)_channel).getRemoteAddress();
	InetSocketAddress local = ((TcpChannel)_channel).getLocalAddress();
	// TODO : check if agent is overloaded --> not useful for now
	if (_engine.historyChannels ())
	    history ("notifyOpenToAgent : "+agent+" : "+connectionId);
	agent.getMuxHandler ().tcpSocketConnected (agent, _id, remote.getAddress().getHostAddress(), remote.getPort(), local.getAddress().getHostAddress(), local.getPort (), null, 0, _secure, true, connectionId, 0);
    }
    
    protected void notifyCloseToAgent (MuxClient agent){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyCloseToAgent : "+agent);
	// TODO : check if agent is overloaded --> not useful for now
	if (_engine.historyChannels ())
	    history ("notifyCloseToAgent : "+agent);
	agent.getMuxHandler ().tcpSocketClosed (agent, _id);
    }

    // called in agent thread
    @Override
    public boolean close (final MuxClient agent){
	if (_shared == false ||
	    _engine.sharedCloseTcpAccept ()){
	    return super.close (agent); // we close the channel
	} else {
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			notifyCloseToAgent (agent);
			if (agentClosed (agent)){
			    long renotify = _engine.renotifyTcpAccept ();
			    if (renotify > 0){
				renotifyOpenToAgent (agent, renotify);
			    }
			}
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
	if (_rejected) return;
	if (_logger.isEnabledFor (_engine.tcpClosedLogLevel ())){
	    defaultCloseReason ("Undefined, assumed from client");
	    _logger.log (_engine.tcpClosedLogLevel (), _engine+" : closed : "+this+" : reason : "+_closeReason);
	}
	_engine.getIOHMeters ().getTcpChannelsAcceptedDurationMeter ().set (System.currentTimeMillis () - _createdTime);
	_engine.getIOHMeters ().getOpenTcpChannelsAcceptedMeter ().inc (-1);
	_engine.getIOHMeters ().getClosedTcpChannelsAcceptedMeter ().inc (1);
	if (_shared == false && _agent != null){
	    _agent.getIOHMeters ().getOpenTcpChannelsAcceptedMeter ().inc (-1);
	    _agent.getIOHMeters ().getClosedTcpChannelsAcceptedMeter ().inc (1);
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
		    }};
	    _engine.schedule (r);
	} else {
	    if (notifyAgent == false){
		_engine.releaseSocketId (_id);
	    }
	}
    }
    
}
