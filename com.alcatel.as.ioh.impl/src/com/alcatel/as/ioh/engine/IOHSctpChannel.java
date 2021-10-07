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

public class IOHSctpChannel extends IOHChannel implements SctpChannelListener, Runnable {
    
    protected PlatformExecutor _serverExec;
    protected long _createdTime;
    protected SctpChannel _sctpChannel;
    protected boolean _rejected;

    // called in the server thread
    public IOHSctpChannel (IOHEngine engine, SctpChannel channel, Map<String, Object> props){
	super (engine, engine.sharedSctpAccept ());
	_createdTime = System.currentTimeMillis ();
	_exec = (PlatformExecutor) props.get (Server.PROP_READ_EXECUTOR);
	_serverExec = engine.getCurrentExecutor ();
	_logger = _engine.getSctpLogger ();
	_channel = _sctpChannel = channel;
	_secure = channel.isSecure ();
	_sendOutBufferMonitor = _engine.getSendSctpBufferMonitor ();
	_sendMeter = _engine.getIOHMeters ().getSendSctpMeter ();
	_readMeter = _engine.getIOHMeters ().getReadSctpMeter ();
	_sendDroppedMeter = _engine.getIOHMeters ().getSendDroppedSctpMeter ();
	_flowController = new FlowController (_channel, 1000, 10000, _exec);
	channel.setWriteBlockedPolicy (AsyncChannel.WriteBlockedPolicy.IGNORE);
    }
    // called just after the constructor (which has time to finish)
    public IOHSctpChannel accepted (){
	if (_rejected) return this; // it was rejected
	_engine.getIOHMeters ().getSctpChannelsAcceptedMeter ().inc (1);
	_engine.getIOHMeters ().getOpenSctpChannelsAcceptedMeter ().inc (1);
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
	try{
	    _toString = new StringBuilder ()
		.append ("SctpChannel[id=").append (_id).append (", remote=").append (_sctpChannel.getRemoteAddresses ()).append (']')
		.toString ();
	}catch(Throwable t){
	    _logger.warn (this+" : exception in toString", t);
	    defaultCloseReason ("Exception while accepting");
	    _channel.close (); // dont return
	}
	if (_logger.isEnabledFor (_engine.sctpAcceptedLogLevel ()))
	    // note : we dont log when the socket is rejected, but a specific log is expected in this case
	    _logger.log (_engine.sctpAcceptedLogLevel (), _engine+" : accepted : "+this);	
	if (checkAgentsSize () == false)
	    return;
	// NOTE : we could limit to agents which were notified of the SctpServer on which this socket was accepted
	// we dont do it for now since it seems useless --> TODO is it ???
	if (_shared){
	    _engine.getSctpChannels ().put (_id, this);
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
		_logger.info (this+" : closing : no active agent connected");
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
		    _agent.getSctpChannels ().put (_id, IOHSctpChannel.this);
		    _agent.getIOHMeters ().getSctpChannelsAcceptedMeter ().inc (1);
		    _agent.getIOHMeters ().getOpenSctpChannelsAcceptedMeter ().inc (1);
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

    static public String[] getAddresses (Set<SocketAddress> addrs){
	String[] ret = new String[addrs.size ()];
	int i = 0;
	for (SocketAddress addr : addrs) ret[i++] = ((InetSocketAddress)addr).getHostString ();
	return ret;
    }
    
    protected void notifyOpenToAgent (MuxClient agent, long connectionId){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyOpenToAgent : "+agent);
	// TODO : check if agent is overloaded --> not useful for now
	try{
	    if (_engine.historyChannels ())
		history ("notifyOpenToAgent : "+agent+" : "+connectionId);
	    agent.getMuxHandler ().sctpSocketConnected (agent, _id, connectionId, getAddresses (_sctpChannel.getRemoteAddresses ()), _sctpChannel.getRemotePort(), getAddresses (_sctpChannel.getLocalAddresses ()), _sctpChannel.getLocalAddress ().getPort (), _sctpChannel.getAssociation ().maxOutboundStreams (), _sctpChannel.getAssociation ().maxInboundStreams (), true, _secure, 0);
	}catch(Throwable t){
	    _logger.warn (this+" : exception while notifyOpenToAgent : "+t);
	    defaultCloseReason ("Exception while notifying agent");
	    _channel.close ();
	}
    }
    
    protected void notifyCloseToAgent (MuxClient agent){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyCloseToAgent : "+agent);
	// TODO : check if agent is overloaded --> not useful for now
	if (_engine.historyChannels ())
	    history ("notifyCloseToAgent : "+agent);
	agent.getMuxHandler ().sctpSocketClosed (agent, _id);
    }

    // called in agent thread
    @Override
    public boolean close (final MuxClient agent){
	if (_shared == false ||
	    _engine.sharedCloseSctpAccept ()){
	    return super.close (agent); // we close the channel
	} else {
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			notifyCloseToAgent (agent);
			if (agentClosed (agent)){
			    long renotify = _engine.renotifySctpAccept ();
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
	return;
    }
    // to be implemented by superclass if it is possible to target an agent....
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
	if (_closed) return;
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
	if (_rejected) return;
	if (_logger.isEnabledFor (_engine.sctpClosedLogLevel ())){
	    defaultCloseReason ("Undefined, assumed from client");
	    _logger.log (_engine.sctpClosedLogLevel (), _engine+" : closed : "+this+" : reason : "+_closeReason);
	}
	_engine.getIOHMeters ().getSctpChannelsAcceptedDurationMeter ().set (System.currentTimeMillis () - _createdTime);
	_engine.getIOHMeters ().getOpenSctpChannelsAcceptedMeter ().inc (-1);
	_engine.getIOHMeters ().getClosedSctpChannelsAcceptedMeter ().inc (1);
	if (_shared == false && _agent != null){
	    _agent.getIOHMeters ().getOpenSctpChannelsAcceptedMeter ().inc (-1);
	    _agent.getIOHMeters ().getClosedSctpChannelsAcceptedMeter ().inc (1);
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
		    }};
	    _engine.schedule (r);
	} else {
	    if (notifyAgent == false){
		_engine.releaseSocketId (_id);
	    }
	}
    }
    
}
