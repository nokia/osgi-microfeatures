// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.impl.client;

import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.nio.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;
import org.osgi.framework.ServiceReference;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.ioh.client.UdpClient.Destination;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.impl.conf.Property;
import com.alcatel.as.ioh.impl.tools.Constants;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;

import alcatel.tess.hometop.gateways.reactor.*;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.UdpOption;

import com.alcatel.as.service.discovery.*;

public class UdpClientImpl extends Constants implements UdpClient {

    private static final Object DISCARD = new Object ();
    public static final Logger LOGGER = Logger.getLogger("as.ioh.client.udp");
    private static final AtomicLong ID = new AtomicLong (0);

    //public static final HashMap<String, UdpClientImpl> _clients = new HashMap<String, UdpClientImpl> ();
    
    private int SEED = 0;
    private volatile List<Destination> _destinations = new ArrayList<> ();
    private List<DestinationImpl> _destinationsDisabled = new ArrayList<> ();
    private Logger _logger;
    private String _id, _uid;
    private Object _attachment;
    private Map<String, Object> _clientProps;
    private ClientFactoryImpl _clientFactory;
    private ReactorProvider _reactorProvider;
    private Reactor _reactor;
    private PlatformExecutors _execs;
    private long _soTimeout = 0L;
    private InetAddress _bindAddr;
    private AdvertisementTracker _tracker;
    private boolean _closed;
    private PlatformExecutor _clientExec;
    private boolean _transparent;
    
    public UdpClientImpl (ClientFactoryImpl f, String id, Map<String, Object> props){
	_clientFactory = f;
	_id = id;
	_clientProps = props;
	_uid = (String) _clientProps.get (PROP_CLIENT_ID);
	if (_uid == null){
	    _uid = "udp-o-" + ID.incrementAndGet ();
	    _clientProps.put (PROP_CLIENT_ID, _uid);
	}
	init ();
    }
    private boolean init (){
	setSince (true, _clientProps);
	_reactorProvider = (ReactorProvider) _clientProps.get ("system.reactor.provider");
	_reactor = (Reactor) _clientProps.get (PROP_CLIENT_REACTOR);
	_execs = (PlatformExecutors) _clientProps.get ("system.executors");
	_clientExec = _execs.createQueueExecutor (_execs.getProcessingThreadPoolExecutor ());
	if (_reactor == null) {
	    try{
		_reactor = _reactorProvider.getDefaultReactor ();
	    }catch(Exception e){
		throw new RuntimeException (e);
	    }
	}
	_logger = (Logger) Property.getProperty (PROP_CLIENT_LOGGER, _clientProps, LOGGER, true);

	_transparent = Property.getBooleanProperty (PROP_CLIENT_TRANSPARENT, _clientProps, false, false);
	String ip = (String) _clientProps.get (UdpClient.PROP_BIND_IP);
	if (ip == null) ip = "0.0.0.0";
	try{
	    _bindAddr = InetAddress.getByName (ip);
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : bind address : "+_bindAddr);
	}catch(Exception e){
	    _logger.warn (this+" : failed to instanciate address : "+ip, e);
	    throw new RuntimeException (e);
	}
	Object o = _clientProps.get (PROP_READ_TIMEOUT);
	if (o != null)
	    _soTimeout = Long.parseLong (o.toString ());

	startTracking ();
	return true;
    }

    public Endpoint newEndpoint (UdpChannelListener listener, InetSocketAddress addr, Object attachment, Map<String, Object> props){
	try{
	    return new EndpointImpl (listener, addr, attachment, props);
	}catch(Exception e){
	    return null;
	}
    }

    public UdpClient addDestinations (List<InetSocketAddress> destAddresses){
	for (InetSocketAddress destAddress: destAddresses)
	    addDestination (destAddress, null, null);
	return this;
    }
    public Destination addDestination (InetSocketAddress destAddress, Object attachment, Map<String, Object> props){
	if (destAddress == null){
	    if (props == null) throw new IllegalArgumentException ("Missing destination Address");
	    try{
		destAddress = new InetSocketAddress (InetAddress.getByName ((String) props.get (Server.PROP_SERVER_IP)),
						     Property.getIntProperty (Server.PROP_SERVER_PORT, props, 0, false));
	    }catch(Exception e){
		throw new IllegalArgumentException ("Invalid destination Address");
	    }
	}
	if (props == null) props = new HashMap<String, Object> ();
	if (!props.containsKey (Server.PROP_SERVER_NAME))
	    props.put (Server.PROP_SERVER_NAME, destAddress.getAddress().getHostAddress() + ":" + destAddress.getPort());
	if (!props.containsKey (Server.PROP_SERVER_IP))
	    props.put (Server.PROP_SERVER_IP, destAddress.getAddress ().getHostAddress());
	if (!props.containsKey (Server.PROP_SERVER_PORT))
	    props.put (Server.PROP_SERVER_PORT, destAddress.getPort ());
	return addDestinationPrivate (new DestinationImpl (destAddress, attachment, props));
    }
    public Destination addDestination (Destination dest){
	DestinationImpl destination = (DestinationImpl) dest;
	DestinationImpl newDest = new DestinationImpl (destination._address, destination._attachment, destination._props);
	return addDestinationPrivate (newDest);
    }

    private Destination addDestinationPrivate (DestinationImpl destination){
	Runnable r = new Runnable (){
		public void run (){
		    if (_closed) return;
		    if (_logger.isDebugEnabled ())
			_logger.debug (UdpClientImpl.this+" : addDestination : "+destination);
		    List<Destination> newList = new ArrayList<> (_destinations.size () + 1);
		    newList.addAll (_destinations);
		    newList.add (destination);
		    _destinations = newList;
		    destination.start ();
		}
	    };
	_clientExec.execute (r);
	return destination;
    }
    public UdpClient removeDestination (Destination dest){
	final DestinationImpl destination = (DestinationImpl) dest;
	Runnable r = new Runnable (){
		public void run (){
		    if (_closed) return;
		    if (_logger.isDebugEnabled ())
			_logger.debug (UdpClientImpl.this+" : removeDestination : "+destination);
		    destination.stop ();
		    if (_destinationsDisabled.remove (destination)){
			// the destination was already among the disabled --> just stop the HC
			return;
		    }
		    List<Destination> newList = new ArrayList<> (_destinations.size ());
		    newList.addAll (_destinations);
		    newList.remove (destination);
		    _destinations = newList;
		}
	    };
	_clientExec.execute (r);
	return this;
    }
    // called in _clientExec when message received
    private void enableDestination (DestinationImpl destination){
	if (_closed) return;
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : enableDestination : "+destination);
	if (_destinationsDisabled.remove (destination) == false){
	    // the destination was removed in the meantime
	    return;
	}
	List<Destination> newList = new ArrayList<> (_destinations.size () + 1);
	newList.addAll (_destinations);
	newList.add (destination);
	_destinations = newList;
    }
    // called in _clientExec upon timeout
    private void disableDestination (DestinationImpl destination){
	if (_closed) return;
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : disableDestination : "+destination);
	if (!_destinations.contains (destination)) return; // the destination was removed in the meantime
	_destinationsDisabled.add (destination);
	List<Destination> newList = new ArrayList<> (_destinations.size ());
	newList.addAll (_destinations);
	newList.remove (destination);
	_destinations = newList;
    }	
    
    public String toString (){
	return _id;
    }
    public Map<String, Object> getProperties (){
	return _clientProps;
    }
    public String getId (){ // definition may have been a better choice. too late
	return _id;
    }
    public String getUniqueId (){
	return _uid;
    }
    public <T> T attachment (){
	return (T) _attachment;
    }
    public <T> T attach (Object attachment){
 	try{
	    return (T) _attachment;
	}finally{_attachment = attachment;}
    }
    public List<Destination> getDestinations (){
	return _destinations;
    }
    public void close (){
	Runnable r = new Runnable (){
		public void run (){
		    _closed = true;
		    if (_logger.isDebugEnabled ())
			_logger.debug (UdpClientImpl.this+" : close");
		    stopTracking ();
		    for (Destination dest : _destinations){
			((DestinationImpl)dest).stop ();
		    }
		    for (DestinationImpl dest : _destinationsDisabled){
			dest.stop ();
		    }
		    // dont clean _destinations & _destinationsDisabled : useless
		}
	    };
	_clientExec.execute (r);
    }
    
    private class EndpointImpl implements Endpoint, UdpChannelListener {
	private UdpChannelListener _listener;
	private UdpChannel _channel;
	private Object _attachment;
	private Map<String, Object> _props;
	private PlatformExecutor _exec;
	private String _toString;
	
	private EndpointImpl (UdpChannelListener listener, InetSocketAddress addr, Object attachment, Map<String, Object> props){
	    _listener = listener;
	    _attachment = attachment;
	    _props = props != null ? props : new HashMap<> ();
	    Object o = _props.get (UdpClient.PROP_READ_EXECUTOR);
	    if (o == null) _props.put (UdpClient.PROP_READ_EXECUTOR, o = _execs.createQueueExecutor (_execs.getProcessingThreadPoolExecutor ()));
	    _exec = (PlatformExecutor) o;
	    Map<UdpOption, Object> udpOptions = new HashMap<ReactorProvider.UdpOption, Object> ();
	    udpOptions.put (ReactorProvider.UdpOption.INPUT_EXECUTOR, _exec);
	    udpOptions.put (ReactorProvider.UdpOption.ENABLE_READ, true);

	    if (addr == null) addr = new InetSocketAddress (_bindAddr, 0);

	    if (_transparent) udpOptions.put (ReactorProvider.UdpOption.IP_TRANSPARENT, Boolean.TRUE);
	    
	    try{
		_channel = _reactorProvider.udpBind (_reactor,
						     addr,
						     this,
						     udpOptions);
		long soTimeout = (long) Property.getIntProperty (UdpClient.PROP_READ_TIMEOUT, _props, (int) _soTimeout, false);
		if (soTimeout > 0)
		    _channel.setSoTimeout (soTimeout);
		_toString = new StringBuilder ().append ("Endpoint[").append (_channel.getLocalAddress ()).append ("]").toString ();
		if (_logger.isDebugEnabled ())
		    _logger.debug (this+" : bound");
	    }catch(Exception e){
		_toString = new StringBuilder ().append ("Endpoint[").append (addr).append ("]").toString ();
		if (_logger.isInfoEnabled ()) _logger.info (this+" : failed to bind", e);
		throw new RuntimeException (e);
	    }
	}
	public String toString (){ return _toString;}
	public UdpClient getUdpClient (){ return UdpClientImpl.this;}	
	public UdpChannel getChannel (){ return _channel;}
	public UdpChannelListener getListener (){ return _listener;}
	public Map<String, Object> getProperties (){ return _props;}	
	public <T> T attachment (){
	    return (T) _attachment;
	}
	public <T> T attach (Object attachment){
	    try{
		return (T) _attachment;
	    }finally{_attachment = attachment;}
	}
	public boolean send (Destination dest, ByteBuffer data, boolean copy){
	    return ((DestinationImpl)dest).send (this, data, copy);
	}

	public void close (){
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : close");
	    _channel.close ();
	}

	public void execute (Runnable r){
	    _exec.execute (r);
	}
	
	public void connectionOpened(UdpChannel cnx){} // not called
	public void connectionFailed(UdpChannel cnx, java.lang.Throwable err){} // not called
	public void connectionClosed(UdpChannel cnx){
	    _listener.connectionClosed (cnx);
	}
	public void messageReceived(UdpChannel cnx,
				    java.nio.ByteBuffer msg,
				    java.net.InetSocketAddress addr){
	    _listener.messageReceived (cnx, msg, addr);
	}
	public void receiveTimeout(UdpChannel cnx){
	    _listener.receiveTimeout (cnx);
	}
	public void writeBlocked(UdpChannel cnx){
	    _listener.writeBlocked (cnx);
	}
	public void writeUnblocked(UdpChannel cnx){
	    _listener.writeUnblocked (cnx);
	}
    }
    
    
    private class DestinationImpl implements UdpClient.Destination, Runnable, UdpChannelListener {
	private InetSocketAddress _address, _pingAddress;
	private Object _attachment;
	private Map<String, Object> _props;
	private String _toString;
	private boolean _inactive, _stopped;
	private long _pingDelay, _pingTimeout;
	private byte[] _pingData;
	private byte[] _pingOK;
	private Endpoint _pingEndpoint;
	private DestinationImpl (InetSocketAddress address, Object attachment, Map<String, Object> props){
	    _address = address;
	    _attachment = attachment;
	    _props = props != null ? props : new HashMap<String, Object> ();
	    _props.put (PROP_CLIENT_ID, new StringBuilder ().append (_uid).append ('-').append (SEED++).toString ());
	    setSince (true, _props);
	    _toString = new StringBuilder ().append (_id).append ('/').append (_props.get (Server.PROP_SERVER_NAME)).toString ();
	}
	public String toString (){
	    return _toString;
	}
	public UdpClient getUdpClient (){
	    return UdpClientImpl.this;
	}
	public InetSocketAddress getRemoteAddress (){
	    return _address;
	}
	public <T> T attachment (){
	    return (T) _attachment;
	}
	public <T> T attach (Object attachment){
	    try{return (T)_attachment;}
	    finally{_attachment = attachment;}
	}
	public Map<String, Object> getProperties (){
	    return _props;
	}
	public boolean send (Endpoint endpoint, ByteBuffer data, boolean copy){
	    if (_logger.isTraceEnabled ())
		_logger.trace (this+" : sending message to server : "+data.remaining ()+" bytes via : "+endpoint);
	    ((EndpointImpl)endpoint)._channel.send (_address, data, copy);
	    return true;
	}
	// called in client._clientExec
	private void start (){
	    Object o = getProperty (UdpClient.PROP_HEALTHCHECK_DELAY, "1000");
	    _pingDelay = Long.parseLong (o.toString ());
	    o = getProperty (UdpClient.PROP_HEALTHCHECK_TIMEOUT, "3100");
	    _pingTimeout = Long.parseLong (o.toString ());
	    o = getProperty (UdpClient.PROP_HEALTHCHECK_DATA, null);
	    if (o != null)
		_pingData = parseData (o.toString ());
	    o = getProperty (UdpClient.PROP_HEALTHCHECK_DATA_SUCCESS, null);
	    if (o != null)
		_pingOK = parseData (o.toString ());
	    if (_inactive = (_pingDelay <= 0 || _pingTimeout <= 0 || _pingData == null)){
		if (_logger.isInfoEnabled ())
		    _logger.info (this+" : no healthcheck configured");
		return;
	    }
	    o = getProperty (UdpClient.PROP_HEALTHCHECK_PORT, null);
	    if (o != null){
		int port = Integer.parseInt (o.toString ());
		_pingAddress = new InetSocketAddress (_address.getAddress (), port);
	    } else {
		o = getProperty (UdpClient.PROP_HEALTHCHECK_PORT_DIFF, null);
		if (o != null){
		    int port = _address.getPort () + Integer.parseInt (o.toString ());
		    _pingAddress = new InetSocketAddress (_address.getAddress (), port);
		}
	    }
	    Map<String, Object> pingProps = new HashMap<> ();
	    pingProps.put (UdpClient.PROP_READ_EXECUTOR, _clientExec);
	    _pingEndpoint = newEndpoint (this, null, null, pingProps);
	    _pingEndpoint.getChannel ().setSoTimeout (_pingTimeout);
	    schedule ();
	}
	// called in _clientExec
	private void schedule (){
	    _clientExec.schedule (this, _pingDelay, java.util.concurrent.TimeUnit.MILLISECONDS);
	}
	// called in client._clientExec
	public void run (){
	    if (_stopped) return;
	    if (_logger.isTraceEnabled ())
		_logger.trace (this+" : ping");
	    if (_pingAddress == null)
		send (_pingEndpoint, ByteBuffer.wrap (_pingData), true);
	    else
		_pingEndpoint.getChannel ().send (_pingAddress, ByteBuffer.wrap (_pingData), true);
	    schedule ();
	}
	public void connectionOpened(UdpChannel cnx){} // not called
	public void connectionFailed(UdpChannel cnx, java.lang.Throwable err){} // not called
	public void connectionClosed(UdpChannel cnx){}
	// called in _clientExec
	public void messageReceived(UdpChannel cnx,
				    java.nio.ByteBuffer msg,
				    java.net.InetSocketAddress addr){
	    if (_stopped) return;
	    if (_inactive){
		if (checkOK (msg) == false) return;
		_logger.warn (this+" : re-enable");
		enableDestination (this);
		_inactive = false;
	    } else {
		if (checkOK (msg) == false){
		    _inactive = true;
		    _logger.warn (this+" : ping KO received : disable");
		    disableDestination (this);
		}
	    }
	}
	private boolean checkOK (ByteBuffer msg){
	    if (_pingOK == null) return true;
	    if (msg.remaining () != _pingOK.length) return false;
	    for (int i = 0; i<_pingOK.length; i++){
		if (_pingOK[i] != msg.get ()) return false;
	    }
	    return true;
	}
	// called in _clientExec
	public void receiveTimeout(UdpChannel cnx){
	    if (_stopped) return;
	    if (_inactive) return;
	    _inactive = true;
	    _logger.warn (this+" : inactivity detected : disable");
	    disableDestination (this);
	}
	public void writeBlocked(UdpChannel cnx){}
	public void writeUnblocked(UdpChannel cnx){}
	// called in client._exec
	private void stop (){
	    _stopped = true;
	    if (_pingEndpoint != null)
		_pingEndpoint.close ();
	}
	public void close (){
	    removeDestination (this);
	}
	private Object getProperty (String name, Object def){
	    Object o = _props.get (name);
	    if (o != null) return o;
	    o = _clientProps.get (name);
	    return o != null ? o : def;
	}
    }

    private boolean startTracking (){
	String namespace = (String) _clientProps.get (PROP_TRACK_NAMESPACE);
	String podName = (String) _clientProps.get (PROP_TRACK_POD_NAME);
	String containerName = (String) _clientProps.get (PROP_TRACK_CONTAINER_NAME);
	String containerPortName = (String) _clientProps.get (PROP_TRACK_CONTAINER_PORT_NAME);
	Object label = _clientProps.get (PROP_TRACK_POD_LABEL);
	if (namespace != null ||
	    podName != null ||
	    containerName != null ||
	    containerPortName != null ||
	    label != null){
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : startTracking : "+namespace+"/"+podName+"/"+containerName+"/"+containerPortName+"/"+label);
	    AdvertisementTracker.Listener listener = new AdvertisementTracker.Listener (){
		    public Object up (AdvertisementTracker tracker, InetSocketAddress addr, ServiceReference ref){
			String name = new StringBuilder ()
			    .append (ref.getProperty ("namespace")).append ('.')
			    .append (ref.getProperty ("pod.name")).append ('.')
			    .append (ref.getProperty ("container.name")).append ('.')
			    .append (ref.getProperty ("container.port.name"))
			    .toString ();
			if (_logger.isInfoEnabled ())
			    _logger.info (UdpClientImpl.this+" : advert UP : "+addr+" : "+name);
			Map<String, Object> props = new HashMap<> ();
			props.put (Server.PROP_SERVER_NAME, name);
			Destination dest = addDestination (addr, null, props);
			return dest;
		    }
		    public void down (AdvertisementTracker tracker, ServiceReference ref, Object ctx){
			if (_logger.isInfoEnabled ())
			    _logger.info (UdpClientImpl.this+" : advert DOWN : "+ctx);
			removeDestination ((Destination) ctx);
		    }
		};
	    _tracker = new AdvertisementTracker (listener);
	    if (namespace != null) _tracker.addFilter ("namespace", namespace, true);
	    if (podName != null) _tracker.addFilter ("pod.name", podName, true);
	    if (containerName != null) _tracker.addFilter ("container.name", containerName, true);
	    if (containerPortName != null) _tracker.addFilter ("container.port.name", containerPortName, true);
	    if (label != null){
		// <property name="track.label">com.nokia.casr.loadbalancer:echo</property>
		if (label instanceof String){
		    String labelS = (String) label;
		    int index = labelS.indexOf (':');
		    if (index != -1 && index != 0 && index != (labelS.length () - 1)){
			_tracker.addFilter ("pod.label."+labelS.substring (0, index), labelS.substring (index+1), true);
		    }
		} else {
		    List<String> list = (List<String>) label;
		    for (String labelS : list){
			int index = labelS.indexOf (':');
			if (index != -1 && index != 0 && index != (labelS.length () - 1)){
			    _tracker.addFilter ("pod.label."+labelS.substring (0, index), labelS.substring (index+1), true);
			}
		    }
		}
	    }
	    _tracker.addFilter ("container.port.protocol", "UDP", true);
	    _tracker.open (_clientFactory.getBundleContext ());
	    return true;
	} else {
	    return false;
	}
    }
    private void stopTracking (){
	if (_tracker != null) _tracker.close ();
	_tracker = null;
    }


    private static byte[] parseData (String in) {
	try{
	    ByteArrayOutputStream baos = new ByteArrayOutputStream ();
	    StringTokenizer st = new StringTokenizer (in, " ");
	    while (st.hasMoreTokens ()){
		String s = st.nextToken ();
		int x = 10;
		if (s.startsWith ("0x")){
		    s = s.substring (2);
		    x = 16;
		}
		int i = Integer.parseInt (s, x);
		baos.write ((byte) i);
	    }
	    return baos.toByteArray ();
	}catch(Exception e){
	    LOGGER.warn ("Exception while parsing healthcheck data : "+in, e);
	    return null;
	}
    }

}
