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
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.ioh.client.TcpClient.Destination;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.impl.conf.Property;
import com.alcatel.as.ioh.impl.tools.Constants;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;

import alcatel.tess.hometop.gateways.reactor.*;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.TcpClientOption;

public class TcpClientImpl extends Constants implements TcpClient, Runnable {

    private static final int DEF_CONNECT_TIMEOUT = 1500;
    private static final int DEF_RECONNECT_TIMEOUT = 3000;

    private static final Object DISCARD = new Object ();
    private static final Logger LOGGER = Logger.getLogger("as.ioh.client.tcp");
    private static final AtomicLong ID = new AtomicLong (0);

    public static final HashMap<String, TcpClientImpl> _clients = new HashMap<String, TcpClientImpl> ();
    
    private int SEED = 0;
    private State _state;
    private List<Destination> _destinations = new ArrayList<Destination> ();
    private TcpClientListener _listener;
    private Logger _logger;
    private String _id, _uid;
    private Reactor _reactor;
    private ReactorProvider _reactorProvider;
    private PlatformExecutor _exec;
    private Object _attachment;
    private long _reconnectTimeout;
    private Future _reconnectFuture;
    private Map<String, Object> _clientProps;
    private ClientFactoryImpl _clientFactory;
    private Object _advertTracker;
    private boolean _multiThreaded;
    private boolean _ephemeral;
    
    public TcpClientImpl (ClientFactoryImpl f, String id, Map<String, Object> props){
	_state = _initState;
	_clientFactory = f;
	_id = id;
	_clientProps = props;
	_uid = (String) _clientProps.get (PROP_CLIENT_ID);
	if (_uid == null){
	    _uid = "tcp-o-" + ID.incrementAndGet ();
	    _clientProps.put (PROP_CLIENT_ID, _uid);
	}
	_ephemeral = Property.getBooleanProperty (PROP_CLIENT_EPHEMERAL, _clientProps, false, false);
    }
    private void init (){
	if (!_ephemeral)
	    setSince (true, _clientProps);
	_reactorProvider = (ReactorProvider) _clientProps.get ("system.reactor.provider");
	_reactor = (Reactor) _clientProps.get (PROP_CLIENT_REACTOR);
	if (_reactor == null) {
	    try{
		_reactor = _reactorProvider.getDefaultReactor ();
	    }catch(Exception e){
		throw new RuntimeException (e);
	    }
	}
	_logger = (Logger) Property.getProperty (PROP_CLIENT_LOGGER, _clientProps, LOGGER, true);
	_exec = (PlatformExecutor) _clientProps.get (PROP_READ_EXECUTOR);
	_multiThreaded = Property.getBooleanProperty (PROP_READ_PARALLEL, _clientProps, false, false);
	_reconnectTimeout = (long) Property.getIntProperty (PROP_CONNECT_RETRY, _clientProps, DEF_RECONNECT_TIMEOUT, true);
	String advertModuleId = (String) _clientProps.get (PROP_TRACK_MODULE_ID);
	String advertInstanceName = (String) _clientProps.get (PROP_TRACK_INSTANCE_NAME);
	if (advertModuleId != null) _advertTracker = _clientFactory.addAdvertListener (this, advertModuleId, advertInstanceName);
    }
    
    public TcpClient addDestinations (List<InetSocketAddress> destAddresses){
	for (InetSocketAddress destAddress: destAddresses)
	    addDestination (destAddress, null, null);
	return this;
    }
    public TcpClient addDestination (InetSocketAddress destAddress, Object attachment, Map<String, Object> props){
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
	DestinationImpl dest = new DestinationImpl (destAddress, attachment, props);
	_destinations.add (dest);
	_state.destinationAdded (dest);
	return this;
    }
    private DestinationImpl initDestination (DestinationImpl dest){
	Property.fillDefaultProperties (dest._props, _clientProps, PROP_READ_TIMEOUT, PROP_CLIENT_PRIORITY, PROP_CONNECT_TIMEOUT,PROP_CONNECT_FROM, PROP_TCP_NO_DELAY, PROP_CLIENT_TRANSPARENT);
	dest.init ();
	return dest;
    }
    public String toString (){
	return _id;
    }
    public Map<String, Object> getProperties (){
	return _clientProps;
    }
    public TcpClientListener getListener (){
	return _listener;
    }
    public String getId (){ // definition may have been a better choice. too late
	return _id;
    }
    public String getUniqueId (){
	return _uid;
    }
    public void execute (Runnable r){
	if (_exec != null)
	    _exec.execute (r);
	else
	    _reactor.schedule (r);
    }
    public <T> Future<T> execute (Callable<T> r){
	if (_exec != null)
	    return _exec.submit (r);
	else
	    return _reactor.schedule (r);
    }
    public TcpClient open (TcpClientListener listener){
	_listener = listener;
	if (!_ephemeral){
	    synchronized (_clients){ _clients.put (_uid, this);}
	}
	_state.open ();
	return this;
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

    private void connect (){
	for (Destination dest : _destinations){
	    ((DestinationImpl)dest).scheduleConnect ();
	}
    }
    
    public void close (){
	if (!_ephemeral){
	    synchronized (_clients){ _clients.remove (_uid);}
	}
	_state.close ();
    }

    public void disableConnect (){
	_state.disableConnect ();
    }

    public void enableConnect (){
	_state.enableConnect ();
    }

    private void scheduleReconnect (final boolean now){
	if (_multiThreaded)
	    execute (new Runnable (){public void run (){reconnect (now);}});
	else
	    reconnect (now);
    }
    protected void reconnect (boolean now){
	_state.reconnect (now);
    }

    private static class State {
	protected void open (){}
	protected void close (){}
	protected void disableConnect (){}
	protected void enableConnect (){}
	protected void reconnect (boolean now){}
	protected void destinationAdded (DestinationImpl dest){}
    }
    private State _initState = new State (){
	    private boolean _enableConnect = true;
	    protected void open (){
		init ();
		for (Destination dest : _destinations)
		    initDestination ((DestinationImpl) dest);
		if (_enableConnect) _state = _enabledState;
		else _state = _disabledState;
		_state.reconnect (true);
	    }
	    protected void disableConnect (){
		_enableConnect = false;
	    }
	    protected void enableConnect (){
		_enableConnect = true;
	    }
	};
    private static State _closedState = new State (); // Note : it is static
    private State _disabledState = new State (){
	    protected void enableConnect (){
		_state = _enabledState;
		connect ();
	    }
	    protected void close (){
		_state = _closedState;
		for (Destination dest : _destinations)
		    ((DestinationImpl)dest).scheduleClose ();
		if (_advertTracker != null) _clientFactory.removeAdvertListener (_advertTracker);
	    }
	    protected void destinationAdded (DestinationImpl dest){
		initDestination (dest);
	    }
	};
    private State _enabledState = new State (){
	    protected void disableConnect (){
		_state = _disabledState;
		if (_reconnectFuture != null){
		    _reconnectFuture.cancel (false);
		    _reconnectFuture = null;
		}
	    }
	    protected void close (){
		_state = _closedState;
		for (Destination dest : _destinations)
		    ((DestinationImpl)dest).scheduleClose ();
		if (_reconnectFuture != null){
		    _reconnectFuture.cancel (false);
		    _reconnectFuture = null;
		}
		if (_advertTracker != null) _clientFactory.removeAdvertListener (_advertTracker);
	    }
	    protected void reconnect (boolean now){
		if (now){
		    if (_reconnectFuture != null){
			_reconnectFuture.cancel (false);
			_reconnectFuture = null;
		    }
		    connect ();
		} else {
		    if (_reconnectFuture != null) return;
		    if (_exec != null)
			_reconnectFuture = _exec.schedule (TcpClientImpl.this, _reconnectTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
		    else
			_reconnectFuture = _reactor.schedule (TcpClientImpl.this, _reconnectTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
		}
	    }
	    protected void destinationAdded (DestinationImpl dest){
		initDestination (dest);
		reconnect (true);
	    }
	};
    
    public void run (){
	_reconnectFuture = null;
	connect ();
    }

    public int sendAll (byte[] data, boolean availableOnly){
	int i = 0;
	for (Destination dest: _destinations){
	    i += dest.send (data, availableOnly);
	}
	return i;
    }
    
    private class DestinationImpl implements TcpClient.Destination, TcpClientChannelListener {
	private InetSocketAddress _address;
	private volatile TcpChannel _channel;
	private Object _attachment;
	private volatile State _state;
	private volatile boolean _available;
	private int[] _history = new int[]{0, 0, 0};
	private TcpChannelListener _messageProcessor;
	private Map<String, Object> _props;
	private PlatformExecutor _exec;
	private Map<TcpClientOption, Object> _connectOptions;
	private long _soTimeout;
	private DestinationImpl (InetSocketAddress address, Object attachment, Map<String, Object> props){
	    _address = address;
	    _attachment = attachment;
	    _props = props != null ? props : new HashMap<String, Object> ();
	    _state = _waitingState;
	    _props.put (PROP_CLIENT_ID, new StringBuilder ().append (_uid).append ('-').append (SEED++).toString ());
	    if (!_ephemeral) setSince (true, _props);
	}
	private void init (){
	    if (_multiThreaded){
		PlatformExecutors execs = _clientFactory.getPlatformExecutors ();
		_exec = execs.createQueueExecutor (execs.getProcessingThreadPoolExecutor ());
	    } else
		_exec = TcpClientImpl.this._exec;
	}
	private void reset (){
	    _connectOptions = new HashMap<TcpClientOption, Object> ();
	    if (_exec != null) _connectOptions.put (TcpClientOption.INPUT_EXECUTOR, _exec);
	    _soTimeout = (long) Property.getIntProperty (PROP_READ_TIMEOUT, _props, 0, true);
	    switch (Property.getIntProperty (PROP_CLIENT_PRIORITY, _props, -1, false)){
	    case 0: _connectOptions.put (TcpClientOption.PRIORITY, AsyncChannel.MIN_PRIORITY); break;
	    case 1: _connectOptions.put (TcpClientOption.PRIORITY, AsyncChannel.MAX_PRIORITY); break;
	    }
	    int connectTimeout = Property.getIntProperty (PROP_CONNECT_TIMEOUT, _props, DEF_CONNECT_TIMEOUT, true);
	    _connectOptions.put (TcpClientOption.TIMEOUT, (long) connectTimeout);
	    Object o = _props.get (PROP_CONNECT_FROM);
	    if (o != null){
		if (o instanceof String){
		    try{
			o = new InetSocketAddress (InetAddress.getByName ((String) o), 0);
			_props.put (TcpClient.PROP_CONNECT_FROM, o);
		    }catch(Exception e){
			LOGGER.error ("Failed to parse connect.from : "+o);
			o = null;
		    }
		}
		if (o != null) _connectOptions.put (TcpClientOption.FROM_ADDR, o);
	    }
	    boolean tcpNoDelay = Property.getBooleanProperty (PROP_TCP_NO_DELAY, _props, true, true);
	    _connectOptions.put (TcpClientOption.TCP_NO_DELAY, tcpNoDelay);
	    boolean transparent = Property.getBooleanProperty (PROP_CLIENT_TRANSPARENT, _props, false, false);
	    if (transparent) _connectOptions.put (TcpClientOption.IP_TRANSPARENT, Boolean.TRUE);
	}
	public void execute (Runnable r){
	    if (_multiThreaded) _exec.execute (r);
	    else TcpClientImpl.this.execute (r);
	}
	public <T> Future<T> execute (Callable<T> r){
	    if (_multiThreaded)	return _exec.submit (r);
	    else return TcpClientImpl.this.execute (r);
	}
	public String toString (){
	    return new StringBuilder ().append (_id).append ('/').append (_props.get (Server.PROP_SERVER_NAME)).append (_state.toString ()).toString ();
	}
	public TcpClient getTcpClient (){
	    return TcpClientImpl.this;
	}
	public InetSocketAddress getRemoteAddress (){
	    return _address;
	}
	public TcpChannel getChannel (){
	    return _channel;
	}
	public int[] getHistory (){
	    return _history;
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
	public boolean isAvailable (){
	    return isOpen () && _available;
	}
	public boolean isOpen (){
	    return _state == _openState;
	}
	public int send (byte[] data, boolean availableOnly){
	    if (isOpen ()){
		if (availableOnly && !_available) return 0;
		if (_logger.isDebugEnabled ())
		    _logger.debug (this+" : sending message to server : "+data.length);
		_channel.send (data, false);
		return 1;
	    }
	    return 0;
	}

	public void open (){
	    scheduleConnect ();
	}

	private void scheduleConnect (){
	    if (_multiThreaded)
		execute (new Runnable (){public void run (){_state.connect ();}});
	    else
		_state.connect ();
	}

	private void scheduleClose (){
	    if (_multiThreaded)
		execute (new Runnable (){public void run (){close ();}});
	    else
		close ();
	}

	public void close (){
	    _state.close ();
	}

	public void connectionEstablished (TcpChannel channel){
	    _state.opened (channel);
	}

	public void connectionFailed (TcpChannel channel, Throwable error){
	    _state.failed ();
	}

	public void connectionClosed (TcpChannel channel){
	    if (channel.attachment () == DISCARD) return;
	    _state.closed ();
	}

	public void writeBlocked (TcpChannel channel){
	    _available = false;
	    _messageProcessor.writeBlocked (channel);
	}
	public void writeUnblocked (TcpChannel channel){
	    _available = true;
	    _messageProcessor.writeUnblocked (channel);
	}

	public int messageReceived (TcpChannel channel, ByteBuffer buffer){
	    return _messageProcessor.messageReceived (channel, buffer);
	}
	
	public void receiveTimeout (TcpChannel channel){
	    _messageProcessor.receiveTimeout (channel);
	}
	
	private class State {
	    protected void connect (){};
	    protected void close (){}
	    protected void opened (TcpChannel channel){}
	    protected void closed (){}
	    protected void failed (){}
	}
	private State _openingState = new State (){
		protected void close (){
		    if (_logger.isInfoEnabled ())
			_logger.info (DestinationImpl.this+" : disabling server connection");
		    _state = _terminatedState;
		}
		protected void opened (TcpChannel channel){
		    if (_logger.isInfoEnabled ())
			_logger.info (DestinationImpl.this+" : server connection opened");
		    if (!_ephemeral) setSince (true, _props);
		    _history[0]++;
		    _history[2] = 0;
		    _state = _openState;
		    _channel = channel;
		    _available = true;
		    _channel.setSoTimeout (_soTimeout);
		    _messageProcessor = _listener.connectionEstablished (TcpClientImpl.this, DestinationImpl.this);
		    _props.put (PROP_CLIENT_PROCESSOR, _messageProcessor);
		}
		protected void failed (){
		    if (_logger.isInfoEnabled ())
			_logger.info (DestinationImpl.this+" : server connection failed");
		    _history[1]++;
		    _history[2]++;
		    _state = _waitingState;
		    scheduleReconnect (false);
		    _listener.connectionFailed (TcpClientImpl.this, DestinationImpl.this);
		}
		public String toString (){
		    return "[Opening]";
		}
	    };
	private State _waitingState = new State (){
		protected void connect (){
		    if (_logger.isDebugEnabled ())
			_logger.debug (DestinationImpl.this+" : connecting to server : "+_props);
		    _state = _openingState;
		    reset ();
		    _reactorProvider.tcpConnect (_reactor, _address, DestinationImpl.this, _connectOptions);
		}
		protected void close (){
		    if (_logger.isInfoEnabled ())
			_logger.info (DestinationImpl.this+" : cancelling server reconnection");
		    _state = _terminatedState;
		}
		public String toString (){
		    return "[Waiting]";
		}
	    };
	private State _openState = new State (){
		protected void close (){
		    if (_logger.isInfoEnabled ())
			_logger.info (DestinationImpl.this+" : closing server connection");
		    _state = _terminatedState;
		    _channel.close ();
		}
		protected void closed (){
		    if (_logger.isInfoEnabled ())
			_logger.info (DestinationImpl.this+" : server connection closed");
		    if (!_ephemeral) setSince (true, _props);
		    _state = _waitingState;
		    _messageProcessor.connectionClosed (_channel);
		    _channel = null;
		    scheduleReconnect (false);
		}
		public String toString (){
		    return "[Open]";
		}
	    };
	private State _terminatedState = new State (){
		protected void opened (TcpChannel channel){
		    if (_logger.isDebugEnabled ())
			_logger.debug (DestinationImpl.this+" : ignoring disabled server connection");
		    channel.attach (DISCARD);
		    channel.close ();
		}
		protected void closed (){
		    if (_logger.isInfoEnabled ())
			_logger.info (DestinationImpl.this+" : server connection closed");
		    if (!_ephemeral) setSince (true, _props);
		    _messageProcessor.connectionClosed (_channel);
		    _channel = null;
		}
		public String toString (){
		    return "[Terminated]";
		}
	    };
    }
}
