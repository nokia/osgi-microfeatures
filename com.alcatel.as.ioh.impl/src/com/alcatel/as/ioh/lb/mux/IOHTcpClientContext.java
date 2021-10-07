package com.alcatel.as.ioh.lb.mux;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.nio.*;
import java.net.InetSocketAddress;

import org.apache.log4j.*;

import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;

import com.alcatel.as.ioh.lb.*;
import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.service.metering2.*;

public class IOHTcpClientContext extends IOHTcpChannel implements IOHClient {

    protected static final AtomicLong ID_GEN = new AtomicLong (0);

    public static Logger LOGGER = Logger.getLogger ("as.ioh.lb.mux.tcp");

    private long _id;
    private IOHRouter _router;
    private boolean _mcast; 
    private Logger _logger;
    private Parser _clientParser, _serverParser;
    private boolean _ignoreData = false;
    private Map<String, Object> _props;
    private Object[] _attachment;
    private MuxClient _currentDestination;
    private boolean _currentDestinationOK = true;
    
    protected IOHTcpClientContext (LbIOHEngine engine, TcpChannel channel, Parser clientParser, Parser serverParser, IOHRouter router, Logger logger, Map<String, Object> props){
	super (engine, channel, props);
	_isText = false;
	_id = ID_GEN.getAndIncrement ();
	_clientParser = clientParser;
	_serverParser = serverParser;
	_router = router;
	_logger = logger;
	_mcast = true;
	_props = props;
    }
    public void start (){
	Runnable r = new Runnable (){
		public void run (){
		    _router.init (IOHTcpClientContext.this);
		}};
	schedule (r);
    }
    public LbIOHEngine getLbIOHEngine (){ return (LbIOHEngine) _engine;}

    // may be called in any thread
    public void sendToClient (boolean copy, java.nio.ByteBuffer msg){
	getLbIOHEngine ().logTraffic (" : SEND [", msg);
	super.sendOut (null, null, true, copy, msg);
    }
    // may be called in any thread
    public void sendToClient (MuxClient from, Chunk chunk){
	try{
	    int remaining = chunk.size ();
	    if (remaining == 0) return; // this is only to compact the buffer in the finally clause below
	    getLbIOHEngine ().logTraffic (" : SEND [", chunk.getData ());
	    super.sendOut (from, null, true, !chunk.isCopy (), chunk.getData ());
	} finally {
	    if (!chunk.isCopy ()) chunk.getData ().compact (); // make it available for write operations
	}
    }
    
    // send from _exec
    public void sendToDestination (MuxClient agent, Chunk chunk, boolean checkBuffer){
	if (chunk.newMessage ()){
	    _currentDestination = agent; // we cache for subsequent chunks
	    _currentDestinationOK = true;
	}
	boolean ok = true;
	try{
	    ByteBuffer data = chunk.getData ();
	    int remaining = data.remaining ();
	    if (agent == null){
		// drop message		
		if (remaining == 0) return; // this is only to compact the buffer in the finally clause below
		if (_logger.isDebugEnabled ())
		    _logger.debug (this+" : drop : "+remaining+" bytes");
		data.position (data.limit ()); // exhaust (necessary for compact below)
		return;
	    }
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : sendToDestination : "+agent+" : "+chunk);
	    ok = _currentDestinationOK && super.sendAgent (agent, null, checkBuffer, 0L, !chunk.isCopy (), data);
	    if (!ok){
		_logger.warn (this+" : sendToDestinationFailed : "+agent+" : "+chunk);
		_router.sendToDestinationFailed (this, agent, chunk);
	    }
	} finally {
	    if (ok && !chunk.isCopy ()) chunk.getData ().compact (); // make it available for write operations
	}
    }
    public void sendToDestination (MuxClient agent, boolean copy, ByteBuffer data){
	// this method does not update _currentDestination : it is meant for specific data to send
	// just a utility
	// no checkBuffer
	if (agent == null) // not expected...
	    return;
	int remaining = data.remaining ();
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : sendToDestination : "+agent+" : "+remaining+" bytes");
	sendAgent (agent, null, false, 0L, copy, data);
    }
    
    /************************************************
     ** TcpChannelListener for client connections **
     ************************************************/

    @Override
    public int messageReceived (TcpChannel channel, ByteBuffer data){
	if (_ignoreData){
	    data.position (data.limit ());
	    return 0;
	}
	_readMeter.inc (data.remaining ());
	try{
	    getLbIOHEngine ().logTraffic (" : RECV [", data);
	    Chunk chunk;
	    while ((chunk = _clientParser.parse (data)) != null){
		if (_logger.isDebugEnabled ())
		    _logger.debug (this+" : messageReceived from client : "+chunk);
		if (chunk.newMessage ())
		    getLbIOHEngine ().getReadTcpMessageMeter ().inc (1);
		clientMessage (chunk);
	    }
	}catch(Exception e){
	    _logger.warn (this+" : exception while handling client data", e);
	    data.position (data.limit ());
	    channel.close ();
	    _ignoreData = true; // disable future reads until closed is called back
	}
	return 0;
    }

    @Override
    protected void notifyOpenToAgent (MuxClient agent, long connectionId){
	if (_mcast){
	    super.notifyOpenToAgent (agent, connectionId);
	    _router.init (this, agent);
	}
    }

    @Override
    // called in agent thread --> mthreaded
    public boolean sendOut (final MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	if (_serverParser == null)
	    return super.sendOut (agent, to, checkBuffer, copy, buffs);
	for (ByteBuffer data : buffs){
	    try{
		Chunk chunk;
		while ((chunk = _serverParser.parse (data)) != null){
		    if (_logger.isDebugEnabled ())
			_logger.debug (this+" : messageReceived from "+agent+" : "+chunk);
		    _router.route (this, agent, chunk);
		}
	    }catch(Exception e){
		_logger.warn (this+" : exception while handling server data", e);
		data.position (data.limit ());
		close ();
		return true;
	    }
	}
	return true;
    }
    
    private void clientMessage (Chunk chunk){
	if (chunk.newMessage ()){
	    _currentDestination = null;
	    _router.route (this, chunk);
	} else {
	    sendToDestination (_currentDestination, chunk, false); // _currentDestination may be null --> ok
	}
    }

    @Override
    public boolean agentClosed (MuxClient agent){
	if (_currentDestination == agent)
	    _currentDestinationOK = false;
	return super.agentClosed (agent);
    }

    /**********************************************/
    /**** implem. of IOHClient **********/
    /**********************************************/

    public long getId (){ return _id;}
    public Logger getLogger (){ return _logger;}    
    public SimpleMonitorable getMeters (){ return getLbIOHEngine ().getIOHMeters ();}
    public IOHChannel getIOHChannel (){ return this;}
    public java.util.Map<String, Object> getProperties (){ return _props;}
    public void attach (Object[] attachment){ _attachment = attachment;}    
    public Object[] attachment () { return _attachment;}
    public <T> T attachment (int index){ return (T) (_attachment[index]);}
    public PlatformExecutor getExecutor (){ return getPlatformExecutor ();}
    
}
