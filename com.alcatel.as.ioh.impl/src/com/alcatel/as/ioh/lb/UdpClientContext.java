package com.alcatel.as.ioh.lb;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.nio.*;
import java.net.InetSocketAddress;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.ioh.client.UdpClient.Destination;
import com.alcatel.as.ioh.impl.conf.Property;
import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.ioh.tools.*;

public class UdpClientContext implements UdpChannelListener {

    protected static final AtomicLong ID_GEN = new AtomicLong (0);

    protected LoadBalancer.MulticastProcessor.UdpChannelListenerImpl _proc;
    protected UdpChannel _channel;
    protected volatile InetSocketAddress _remote;
    protected Map<String, Object> _props;
    protected long _id;
    protected Logger _logger;
    protected Router _router;
    protected Parser _parser;
    protected volatile String _toString;
    protected UdpMeters _meters;
    protected boolean _inactive;

    protected UdpClient _udpClient;
    protected UdpClient.Destination _currentDestination;
    protected UdpClient.Endpoint _endpoint;

    protected Reactor _reactor;
    protected UdpChannel _destChannel;

    protected int _readResponses = 0;
    
    protected UdpClientContext (LoadBalancer.MulticastProcessor.UdpChannelListenerImpl proc, UdpChannel channel, UdpClient client, InetSocketAddress remote, Parser parser, Router router, Logger logger, UdpMeters meters, Map<String, Object> props){
	_id = ID_GEN.getAndIncrement ();
	_proc = proc;
	_channel = channel;
	_udpClient = client;
	_remote = remote;
	_props = props;
	_parser = parser;
	_router = router;
	_logger = logger;
	_meters = meters;
	_readResponses = 0;
	_toString = new StringBuilder ().append ("UdpClientContext[").append (_remote).append ("]").toString ();
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : instanciated");
    }
    public boolean init (){
	boolean checkCollide = false;
	if (_proc.transparent ())
	    _endpoint = _udpClient.newEndpoint (this, _remote, null, _props); // re use client ip + port
	else if (_proc.transparentIP ())
	    _endpoint = _udpClient.newEndpoint (this, new InetSocketAddress (_remote.getAddress (), 0), null, _props); // re-use just client ip
	else { // the usual case
	    _endpoint = _udpClient.newEndpoint (this, null, null, _props);
	    checkCollide = true;
	}
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : endpoint is : "+_endpoint);
	if (_endpoint != null){
	    if (checkCollide && addressCollide ()){ // saw this issue in some load tests where the loader gave up the IP/port : this leads to infinite loop
		_logger.warn (this+" : addressCollide : "+_endpoint+" : abort");
		_endpoint.close (); // dont try to be smarter : this never happens normally
		return false;
	    }
	    return true;
	} else
	    return false;
    }
    public void recycle (InetSocketAddress remote, Parser parser, Router router){
	String oldString = _toString;
	_remote = remote;
	_parser = parser;
	_router = router;
	_toString = new StringBuilder ().append ("UdpClientContext[").append (_remote).append ("]").toString ();
	_readResponses = 0;
	long readTimeout = _proc.getReadTimeout ();
	if (readTimeout > 0L) _endpoint.getChannel ().setSoTimeout (readTimeout);
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : recycle : "+oldString);
	_endpoint.execute (new Runnable (){public void run (){ _inactive = false;}});
    }
    private boolean addressCollide (){
	InetSocketAddress local = _endpoint.getChannel ().getLocalAddress ();
	return (_remote.getPort () == local.getPort () && // start with port for speed
		_remote.getAddress ().equals (local.getAddress ()));
    }
    public void close (){
	_endpoint.close ();
    }
    private boolean checkReadResponses (boolean respReceived){
	// returns true if no response is pending and we are done
	int maxRead = _proc.getReadResponses ();
	if (maxRead == -1) return false; // undefined - never done()
	if (maxRead == 0) return true; // always done()
	if (respReceived) return ++_readResponses == maxRead;
	else return false;
    }
    
    public List<Destination> getDestinations (){ return _udpClient.getDestinations ();}
    public InetSocketAddress getRemoteAddress (){ return _remote;}
    public UdpChannel getChannel (){ return _channel;}
    public UdpClient.Endpoint getEndpoint (){ return _endpoint;}
    public String toString (){ return _toString;}
    public Logger getLogger (){ return _logger;}
    public UdpMeters getMeters (){ return _meters;}

    public void messageReceived (ByteBuffer data){
	int remaining = data.remaining ();
	try{
	    Chunk chunk;
	    while ((chunk = _parser.parse (data)) != null){
		if (chunk.newMessage ()){
		    if (_logger.isDebugEnabled ())
			_logger.debug (this+" : messageReceived from client : newMessage : "+remaining+" bytes");
		    _meters.getReadUdpMessageMeter ().inc (1);
		} else {
		    if (_logger.isDebugEnabled ())
			_logger.debug (this+" : messageReceived from client : message continuation : "+remaining+" bytes");
		}
		clientMessage (chunk);
	    }
	}catch(Exception e){
	    _meters.getFailedUdpMeter ().inc (remaining);
	    _logger.warn (this+" : exception", e);
	    data.position (data.limit ());
	    if (checkReadResponses (true)) // pretend we got a response
		_endpoint.execute (() -> {done ("Failed request / No more responses expected");});
	}
	return;
    }

    public void clientMessage (Chunk chunk){
	if (chunk.newMessage ()){
	    _currentDestination = null;
	    _router.route (this, chunk);
	} else {
	    sendToDestination (_currentDestination, chunk); // _currentDestination may be null --> ok
	}
    }

    public void sendToClient (boolean copy, ByteBuffer buffer){
	int remaining = buffer.remaining ();
	int size = _channel.getSendBufferSize ();
	if (size > _proc.getMaxClientBuffer ()){
	    if (_logger.isInfoEnabled ())
		_logger.info (this+" : client overloaded : dropping");
	    _meters.getOverloadClientMeter ().inc (1);
	    buffer.position (buffer.limit ()); // exhaust
	    return;
	}
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : sendToClient : "+remaining+" bytes");
	_meters.getSendUdpMeter ().inc (remaining);
	_channel.send (_remote, buffer, copy);
    }

    public void sendToDestination (Destination toDestination, Chunk chunk){
	_currentDestination = toDestination; // we cache for subsequent chunks
	try{
	    if (toDestination == null){
		// drop message
		int remaining = chunk.size ();
		if (_logger.isDebugEnabled ())
		    _logger.debug (this+" : drop : "+remaining+" bytes");
		chunk.getData ().position (chunk.getData ().limit ()); // exhaust (necessary for compact below)
		//_meters.getSendUdpDroppedDestMeter ().inc (remaining);
		return;
	    }
	    int remaining = chunk.size ();
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : sendToDestination : "+toDestination+" : "+remaining+" bytes");
	    _meters.getSendUdpDestMeter ().inc (remaining);
	    toDestination.send (_endpoint, chunk.getData (), !chunk.isCopy ());
	} finally {
	    if (!chunk.isCopy ()) chunk.getData ().compact (); // make it available for write operations
	}
	if (checkReadResponses (false)){
	    // it will not work if we send many messages to the destination... so it works in certain cases (but usually ok)
	    _endpoint.execute (() -> {done ("No response expected");});
	}
    }


    /**************************************
     *         UDP events from servers    *
     **************************************/

    public void connectionOpened(UdpChannel cnx){ // not called
    }
    public void connectionFailed(UdpChannel cnx,
				 java.lang.Throwable err){ // not called
    }
    public void connectionClosed(UdpChannel cnx){
    }
    public void messageReceived(UdpChannel cnx,
				java.nio.ByteBuffer msg,
				java.net.InetSocketAddress addr){
	int remaining = msg.remaining ();
	if (_inactive){
	    if (_logger.isInfoEnabled ())
		_logger.info (this+" : messageReceived while inactive from : "+addr+" : dropping "+remaining+" bytes");
	    msg.position (msg.limit ());
	    return;
	}
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : messageReceived from server : "+addr+" : "+remaining+" bytes");
	_meters.getReadUdpDestMeter ().inc (remaining);
	sendToClient (true, msg);
	if (checkReadResponses (true))
	    done ("All responses received");
    }
    public void receiveTimeout(UdpChannel cnx){
	if (_inactive){ // should not happen - precaution
	    _logger.warn (this+" : receiveTimeout while inactive");
	    return;
	}
	done ("receiveTimeout");
    }
    protected void done (String reason){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : done : "+reason);
	_inactive = true;
	Runnable r = new Runnable (){
		public void run (){
		    if (_proc.done (UdpClientContext.this))
			close ();
		    else
			_endpoint.getChannel ().setSoTimeout (0L);
		}};
	_proc.execute (r);
    }
    public void writeBlocked(UdpChannel cnx){
    }
    public void writeUnblocked(UdpChannel cnx){
    }
}
