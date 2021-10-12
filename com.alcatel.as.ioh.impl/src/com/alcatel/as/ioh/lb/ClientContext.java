// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.nio.*;
import java.net.*;

import org.apache.log4j.*;

import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.ioh.client.TcpClient.Destination;
import com.alcatel.as.ioh.impl.conf.Property;
import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.ioh.tools.*;

public class ClientContext implements Client, TcpClientListener {

    protected static final AtomicLong ID_GEN = new AtomicLong (0);

    protected LoadBalancer.MulticastProcessor _mproc;
    protected LoadBalancer.UnicastProcessor _uproc;
    protected AsyncChannel _channel;
    protected Map<String, Object> _props;
    protected long _id;
    protected Logger _logger;
    protected PlatformExecutor _exec;
    protected Object[] _attachment;
    protected List<Destination> _destinations;
    protected Router _mrouter;
    protected UnicastRouter _urouter;
    protected String _toString;
    protected TcpClient _tcpClient;
    protected Set<InetSocketAddress> _adverts;
    protected State _state = STATE_INIT;
    protected int _destinationSize;
    protected int _maxClientBuffer;
    protected Meters _meters;
    protected Destination _currentDestination;
    protected UnicastContext _uctx;
    protected Level _logMappingLevel;
    protected Logger _logMappingLogger;
    protected String _logMappingMsgOpen, _logMappingMsgClose;
    protected Level _logTrafficLevel;
    protected Logger _logTrafficLogger;
    protected Parser _serverParser;
    
    private ClientContext (AsyncChannel channel, Logger logger, Meters meters, Map<String, Object> props){
	_id = ID_GEN.getAndIncrement ();
	_channel = channel;
	_props = props;
	_logger = logger;
	_meters = meters;
	_exec = (PlatformExecutor) props.get (Server.PROP_READ_EXECUTOR);
	_maxClientBuffer = Property.getIntProperty (LoadBalancer.PROP_CLIENT_WRITE_BUFFER_MAX, props, Integer.MAX_VALUE, true);
	_logMappingLevel = (Level) _props.get (LoadBalancer.PROP_CLIENT_LOG_MAPPING_LEVEL);
	_logMappingLogger = (Logger) _props.get (LoadBalancer.PROP_CLIENT_LOG_MAPPING_LOGGER);
	if (_logMappingLogger == null) _logMappingLogger = _logger;
	_logMappingMsgOpen = (String) Property.getProperty (LoadBalancer.PROP_CLIENT_LOG_MAPPING_MSG_OPEN, props, "MAPPING-OPEN", true);
	_logMappingMsgClose = (String) Property.getProperty (LoadBalancer.PROP_CLIENT_LOG_MAPPING_MSG_CLOSE, props, "MAPPING-CLOSE", true);
	_logTrafficLevel = (Level) _props.get (LoadBalancer.PROP_CLIENT_LOG_TRAFFIC_LEVEL);
	_logTrafficLogger = (Logger) _props.get (LoadBalancer.PROP_CLIENT_LOG_TRAFFIC_LOGGER);
	if (_logTrafficLogger == null) _logTrafficLogger = _logger;
    } 
    
    protected ClientContext (LoadBalancer.MulticastProcessor proc, AsyncChannel channel, Router router, Logger logger, Meters meters, Map<String, Object> props){
	this (channel, logger, meters, props);
	_mproc = proc;
	_mrouter = router;
    }
    protected ClientContext (LoadBalancer.UnicastProcessor proc, AsyncChannel channel, UnicastRouter router, Logger logger, Meters meters, Set<InetSocketAddress> adverts, Map<String, Object> props){
	this (channel, logger, meters, props);
	_uproc = proc;
	_urouter = router;
	_adverts = adverts;
	_uctx = new UnicastContext ();
    }
    public void start (){
	Runnable r = new Runnable (){
		public void run (){
		    if (_uctx != null){
			Map<String, Object> props = new HashMap<> ();
			props.put (TcpClient.PROP_READ_EXECUTOR, _exec);
			props.put (TcpClient.PROP_READ_PARALLEL, "true");
			props.put (TcpClient.PROP_CLIENT_LOGGER, _logger);
			props.put ("client.ephemeral", true);
			_tcpClient = _uproc.getLoadBalancer ().getClientFactory ().newTcpClient ((String) _props.get (LoadBalancer.PROP_DEST_ID), props);
			boolean transparent = Property.getBooleanProperty (TcpClient.PROP_CLIENT_TRANSPARENT, _tcpClient.getProperties (), false, false);
			if (transparent)
			    _tcpClient.getProperties ().put (TcpClient.PROP_CONNECT_FROM,
							     new InetSocketAddress (((TcpChannel)_channel).getRemoteAddress ().getAddress (), 0));
			if (_adverts != null){
			    for (InetSocketAddress addr : _adverts)
				_tcpClient.addDestination (addr, null, null);
			}
			_urouter.init (ClientContext.this);
			_tcpClient.disableConnect ();
			_tcpClient.open (ClientContext.this);
			if (_urouter.neededBuffer () == 0){
			    // we can open the destination now
			    newUnicastConnect (null);
			} else {
			    USTATE_INIT_READING.enter (ClientContext.this, "Read Init Chunk");
			}
		    } else {
			MSTATE_OPENING.enter (ClientContext.this, "Start");
			Map<String, Object> props = new HashMap<> ();
			props.put (TcpClient.PROP_READ_EXECUTOR, _exec);
			props.put (TcpClient.PROP_READ_PARALLEL, "true");
			props.put (TcpClient.PROP_CLIENT_LOGGER, _logger);
			_tcpClient = _mproc.getLoadBalancer ().getClientFactory ().newTcpClient ((String) _props.get (LoadBalancer.PROP_DEST_ID), props);
			boolean transparent = Property.getBooleanProperty (TcpClient.PROP_CLIENT_TRANSPARENT, _tcpClient.getProperties (), false, false);
			if (transparent)
			    _tcpClient.getProperties ().put (TcpClient.PROP_CONNECT_FROM,
							     new InetSocketAddress (((TcpChannel)_channel).getRemoteAddress ().getAddress (), 0));
			_destinationSize = _tcpClient.getDestinations ().size ();
			_destinations = new ArrayList<> (_destinationSize);
			_mrouter.init (ClientContext.this);
			_tcpClient.open (ClientContext.this);
			_tcpClient.disableConnect ();
			if (_destinationSize == 0){
			    _channel.close ();
			    return;
			}
		    }
		}
	    };
	execute (r);
    }
    public void execute (Runnable r){ _exec.execute (r);}
    
    public String toString (){ return _toString;}

    /*******************************************/
    /**** all client callbacks are in _exec ****/
    /*******************************************/
    
    public void clientMessage (Chunk chunk){
	_state.clientData (this, chunk);
    }

    public void clientTimeout (){
	_logger.warn (this+" : clientTimeout");
	_channel.shutdown ();
    }

    public void clientBlocked (){}
    public void clientUnblocked (){}
    public void clientClosed (){
	_state.clientClosed (this);
    }

    /**********************************************/
    /**** dest callbacks - multithreaded **********/
    /**********************************************/

    public TcpChannelListener connectionEstablished(TcpClient client,
						    final TcpClient.Destination destination){
	_meters.getOpenTcpChannelsDestMeter ().inc (1);
	Runnable r = new Runnable (){
		public void run (){
		    if (_logger.isInfoEnabled ())
			_logger.info (ClientContext.this+" : "+destination+" : connectionEstablished");
		    if (_logMappingLogger.isEnabledFor (_logMappingLevel)){
			StringBuilder sb = new StringBuilder ();
			sb.append (ClientContext.this.toString ())
			    .append (" : ").append (_logMappingMsgOpen).append (" : CLIENT=").append (((TcpChannel)_channel).getRemoteAddress ())
			    .append (" LOCAL=").append (destination.getChannel ().getLocalAddress ())
			    .append (" SERVER=").append (destination.getChannel ().getRemoteAddress ());
			_logMappingLogger.log (_logMappingLevel, sb.toString ());
		    }
		    _state.serverOpened (ClientContext.this, destination);
		}};
	execute (r);
	return new TcpChannelListener (){
	    public int messageReceived (TcpChannel channel, ByteBuffer data){
		_meters.getReadTcpDestMeter ().inc (data.remaining ());
		serverData (destination, data);
		return 0;
	    }
	    public void receiveTimeout (TcpChannel channel){
		if (_logger.isInfoEnabled ())
		    _logger.info (ClientContext.this+" : "+destination+" : receiveTimeout");
		channel.shutdown ();
	    }
	    public void writeBlocked (TcpChannel channel){}
	    public void writeUnblocked (TcpChannel channel){}
	    public void connectionClosed(final TcpChannel cnx){
		_meters.getOpenTcpChannelsDestMeter ().inc (-1);
		Runnable r = new Runnable (){
			public void run (){
			    if (_logger.isInfoEnabled ())
				_logger.info (ClientContext.this+" : "+destination+" : connectionClosed");
			    if (_logMappingLogger.isEnabledFor (_logMappingLevel)){
				StringBuilder sb = new StringBuilder ();
				sb.append (ClientContext.this.toString ())
				    .append (" : ").append (_logMappingMsgClose).append (" : CLIENT=").append (((TcpChannel)_channel).getRemoteAddress ())
				    .append (" LOCAL=").append (cnx.getLocalAddress ())
				    .append (" SERVER=").append (cnx.getRemoteAddress ());
				_logMappingLogger.log (_logMappingLevel, sb.toString ());
			    }
			    _state.serverClosed (ClientContext.this, destination);
			}};
		execute (r);
	    }
	};
    }
    // may be overridden
    protected void serverData (Destination server, ByteBuffer data){
	if (_serverParser == null){
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : messageReceived from "+server+" : "+data.remaining ()+" bytes");
	    sendToClient (true, data);
	} else {
	    try{
		Chunk chunk;
		while ((chunk = _serverParser.parse (data)) != null){
		    if (_logger.isDebugEnabled ())
			_logger.debug (this+" : messageReceived from "+server+" : "+chunk);
		    _state.serverData (this, server, chunk);
		}
	    }catch(Exception e){
		_logger.warn (this+" : exception while handling server data", e);
		data.position (data.limit ());
		server.close ();
	    }
	}
    }
    public void connectionFailed(TcpClient client,
				 TcpClient.Destination destination){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : "+destination+" : connectionFailed");
	_meters.getFailedTcpChannelsDestMeter ().inc (1);
	_state.serverFailed (this, destination);
    }

    /**********************************************/
    /**** implem. of Client **********/
    /**********************************************/

    public long getId (){ return _id;}
    public Logger getLogger (){ return _logger;}    
    public PlatformExecutor getExecutor (){ return _exec;}
    public Meters getMeters (){ return _meters;} //TODO
    public AsyncChannel getChannel (){ return _channel;}
    public java.util.Map<String, Object> getProperties (){ return _props;}
    public void attach (Object[] attachment){ _attachment = attachment;}    
    public Object[] attachment () { return _attachment;}
    public <T> T attachment (int index){ return (T) (_attachment[index]);}
    public TcpClient getTcpClient (){ return _tcpClient;}
    public List<Destination> getOpenDestinations (){ return _destinations;}
    
    // may be called in any thread
    public void sendToClient (boolean copy, java.nio.ByteBuffer msg){
	int size = _channel.getSendBufferSize ();
	if (size > _maxClientBuffer){
	    _logger.warn (this+" : client overloaded : closing");
	    msg.position (msg.limit ()); // exhaust
	    _channel.shutdown ();
	} else {
	    int remaining = msg.remaining ();
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : sendToClient : "+remaining+" bytes");
	    _meters.getSendTcpMeter ().inc (remaining);
	    if (_logTrafficLogger.isEnabledFor (_logTrafficLevel)){
		StringBuilder sb = new StringBuilder ()
		    .append (this+" : SEND [");
		LoadBalancer.dumpData (sb, msg);
		sb.append (']');
		_logTrafficLogger.log (_logTrafficLevel, sb.toString ());
	    }
	    _channel.send (msg, copy);
	}
    }
    // may be called in any thread
    public void sendToClient (Destination from, Chunk chunk){
	try{
	    int remaining = chunk.size ();
	    if (remaining == 0) return; // this is only to compact the buffer in the finally clause below
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : sendToClient : from="+from+" : "+chunk);
	    sendToClient (!chunk.isCopy (), chunk.getData ());
	} finally {
	    if (!chunk.isCopy ()) chunk.getData ().compact (); // make it available for write operations
	}
    }
    // send from _exec
    public void sendToDestination (Destination toDestination, Chunk chunk){
	_currentDestination = toDestination; // we cache for subsequent chunks
	try{
	    if (toDestination == null){
		// drop message
		int remaining = chunk.size ();
		if (remaining == 0) return; // this is only to compact the buffer in the finally clause below
		if (_logger.isDebugEnabled ())
		    _logger.debug (this+" : drop : "+remaining+" bytes");
		chunk.getData ().position (chunk.getData ().limit ()); // exhaust (necessary for compact below)
		_meters.getSendTcpDroppedDestMeter ().inc (remaining);
		return;
	    }
	    int remaining = chunk.size ();
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : sendToDestination : "+toDestination+" : "+chunk);
	    _meters.getSendTcpDestMeter ().inc (remaining);
	    toDestination.getChannel ().send (chunk.getData (), !chunk.isCopy ());
	} finally {
	    if (!chunk.isCopy ()) chunk.getData ().compact (); // make it available for write operations
	}
    }

    public void sendToDestination (Destination toDestination, boolean copy, ByteBuffer data){
	// this method does not update _currentDestination : it is meant for specific data to send
	// just a utility
	if (toDestination == null) // not expected...
	    return;
	int remaining = data.remaining ();
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : sendToDestination : "+toDestination+" : "+remaining+" bytes");
	_meters.getSendTcpDestMeter ().inc (remaining);
	toDestination.getChannel ().send (data, copy);
    }
    
    /**********************************************/
    /**** State machine **********/
    /**********************************************/
    
    protected static class State {
	protected void enter (ClientContext ctx, String reason){
	    if (ctx._logger.isDebugEnabled ())
		ctx._logger.debug (ctx+" : change state from : "+ctx._state+" to : "+this+" : "+reason);
	    ctx._state = this;
	}
	protected void clientData (ClientContext ctx, Chunk chunk){throw new IllegalStateException (ctx+" : state="+this+" : event=clientData");}
	protected void clientClosed (ClientContext ctx){throw new IllegalStateException (ctx+" : state="+this+" : event=clientClosed");}
	protected void serverOpened (ClientContext ctx, Destination dest){throw new IllegalStateException (ctx+" : state="+this+" : event=serverOpened");}
	protected void serverFailed (ClientContext ctx, Destination dest){}
	protected void serverClosed (ClientContext ctx, Destination dest){throw new IllegalStateException (ctx+" : state="+this+" : event=serverClosed");}
	protected void serverData (ClientContext ctx, Destination dest, Chunk chunk){
	    //by def : drop the data
	    if (!chunk.isCopy ()) chunk.getData ().compact ();
	}
    }
    protected static State STATE_INIT = new State (){
	    public String toString (){ return "INIT";}
	};

    /**********************************************/
    /**** Multicast State machine **********/
    /**********************************************/
    
    protected static State MSTATE_OPENING = new State (){
	    @Override
	    protected void clientClosed (ClientContext ctx){
		MSTATE_CLOSED.enter (ctx, "Client closed");
	    }
	    @Override
	    protected void serverOpened (ClientContext ctx, Destination dest){
		ctx._destinations.add (dest);
		ctx._mrouter.init (ctx, dest);
		MSTATE_OPEN.enter (ctx, "Destination opened");
	    }
	    @Override
	    protected void serverFailed (ClientContext ctx, Destination dest){
		if (--ctx._destinationSize == 0){
		    ctx._channel.close ();
		    MSTATE_CLOSED.enter (ctx, "No Destination opened");
		}
	    }
	    public String toString (){ return "OPENING";}
	};
    protected static State MSTATE_OPEN = new State (){
	    @Override
	    protected void enter (ClientContext ctx, String reason){
		super.enter (ctx, reason);
		ctx._channel.enableReading ();
		ctx._tcpClient.enableConnect ();
	    }
	    @Override
	    protected void clientData (ClientContext ctx, Chunk chunk){
		if (chunk.newMessage ()){
		    ctx._currentDestination = null;
		    ctx._mrouter.route (ctx, chunk);
		} else {
		    ctx.sendToDestination (ctx._currentDestination, chunk); // _currentDestination may be null --> ok
		}
	    }
	    @Override
	    protected void clientClosed (ClientContext ctx){
		MSTATE_CLOSED.enter (ctx, "Client closed");
	    }
	    @Override
	    protected void serverOpened (ClientContext ctx, Destination dest){
		ctx._destinations.add (dest);
		ctx._mrouter.init (ctx, dest);
	    }
	    @Override
	    protected void serverData (ClientContext ctx, Destination dest, Chunk chunk){
		ctx._mrouter.route (ctx, dest, chunk);
	    }
	    @Override
	    protected void serverClosed (ClientContext ctx, Destination dest){
		ctx._destinations.remove (dest);
		if (ctx._destinations.size () == 0){
		    if (ctx._logger.isInfoEnabled ()) ctx._logger.info (ctx + " : all serverClosed : closing client");
		    ctx._channel.close ();
		    MSTATE_CLOSED.enter (ctx, "All Destinations closed");
		}
	    }
	    public String toString (){ return "OPEN";}
	};
    protected static State MSTATE_CLOSED = new State (){
	    @Override
	    protected void enter (ClientContext ctx, String reason){
		super.enter (ctx, reason);
		ctx._tcpClient.close ();
		ctx._destinations.clear ();
	    }
	    @Override
	    protected void clientData (ClientContext ctx, Chunk chunk){
		ctx.sendToDestination (null, chunk);
	    }
	    @Override
	    protected void clientClosed (ClientContext ctx){}
	    @Override
	    protected void serverOpened (ClientContext ctx, Destination dest){}
	    @Override
	    protected void serverClosed (ClientContext ctx, Destination dest){}
	    public String toString (){ return "CLOSED";}
	};

    /**********************************************/
    /**** Unicast State machine **********/
    /**********************************************/

    protected class UnicastContext {
	protected Chunk _initChunk;
    }
    protected void newUnicastConnect (Destination failedDest){
    	if (failedDest != null) _tcpClient.getDestinations ().remove (failedDest);
    	_currentDestination = _urouter.route (this, _uctx._initChunk);
    	if (_currentDestination == null){
    	    _channel.close (); // this re-enables reading if it was not enabled
    	    USTATE_CLOSED.enter (this, "No Destination found");
    	} else {
    	    if (_uctx._initChunk == null)
    		USTATE_OPENING_NOT_READING.enter (this, "Connect to "+_currentDestination);
    	    else
    		USTATE_OPENING_READING.enter (this, "Connect to "+_currentDestination);
    	    _currentDestination.open ();
    	}
    }
    
    protected static State USTATE_INIT_READING = new State (){
	    public String toString (){ return "[INIT_READING]";}
	    @Override
	    protected void enter (ClientContext ctx, String reason){
		super.enter (ctx, reason);
		ctx._channel.enableReading ();
	    }
	    @Override
	    protected void clientData (ClientContext ctx, Chunk chunk){
		ctx._uctx._initChunk = chunk;
		ctx.newUnicastConnect (null);
	    }
	    @Override
	    protected void clientClosed (ClientContext ctx){
		USTATE_CLOSED.enter (ctx, "Client closed");
	    }
	};
    protected static State USTATE_OPENING_NOT_READING = new State (){
	    public String toString (){ return "[OPENING_NOT_READING]";}
	    @Override
	    protected void serverOpened (ClientContext ctx, Destination dest){
		ctx._channel.enableReading ();
		USTATE_OPEN.enter (ctx, "Destination opened");
	    }
	    @Override
	    protected void serverFailed (ClientContext ctx, Destination dest){
		ctx.newUnicastConnect (dest);
	    }
	    @Override
	    protected void clientClosed (ClientContext ctx){
		USTATE_CLOSED.enter (ctx, "Client closed");
	    }
	};
    protected static State USTATE_OPENING_READING = new State (){
	    public String toString (){ return "[OPENING_READING]";}
	    @Override
	    protected void serverOpened (ClientContext ctx, Destination dest){
		USTATE_OPEN.enter (ctx, "Destination opened");
		ctx.sendToDestination (dest, ctx._uctx._initChunk);
	    }
	    @Override
	    protected void serverFailed (ClientContext ctx, Destination dest){
		ctx.newUnicastConnect (dest);
	    }
	    @Override
	    protected void clientClosed (ClientContext ctx){
		USTATE_CLOSED.enter (ctx, "Client closed");
	    }
	};
    protected static State USTATE_OPEN = new State (){
	    public String toString (){ return "[OPEN]";}
	    @Override
	    protected void enter (ClientContext ctx, String reason){
		super.enter (ctx, reason);
	    }
	    @Override
	    protected void clientClosed (ClientContext ctx){
		USTATE_CLOSED.enter (ctx, "Client closed");
	    }
	    @Override
	    protected void clientData (ClientContext ctx, Chunk chunk){
		ctx.sendToDestination (ctx._currentDestination, chunk);
	    }
	    @Override
	    protected void serverClosed (ClientContext ctx, Destination dest){
		ctx._channel.close ();
		USTATE_CLOSED.enter (ctx, "Destination closed");
	    }
	};
    protected static State USTATE_CLOSED = new State (){
	    @Override
	    protected void enter (ClientContext ctx, String reason){
		super.enter (ctx, reason);
		if (ctx._currentDestination != null)
		    ctx._currentDestination.close ();  // TODO check that if opening, then ok
	    }
	    @Override
	    protected void clientData (ClientContext ctx, Chunk chunk){
		ctx.sendToDestination (null, chunk);
	    }
	    @Override
	    protected void clientClosed (ClientContext ctx){}
	    @Override
	    protected void serverOpened (ClientContext ctx, Destination dest){}
	    @Override
	    protected void serverClosed (ClientContext ctx, Destination dest){}
	    public String toString (){ return "CLOSED";}
	};

    
}
