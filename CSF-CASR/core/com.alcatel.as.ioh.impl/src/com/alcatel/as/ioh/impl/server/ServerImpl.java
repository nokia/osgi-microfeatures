package com.alcatel.as.ioh.impl.server;

import java.util.*;
import java.net.*;
import java.text.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.nio.file.*;

import org.apache.log4j.Logger;
import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.impl.conf.Property;
import com.alcatel.as.ioh.impl.tools.Constants;

import alcatel.tess.hometop.gateways.reactor.*;
import alcatel.tess.hometop.gateways.reactor.AsyncChannel.WriteBlockedPolicy;

import com.alcatel.as.service.concurrent.*;

public abstract class ServerImpl extends Constants implements Server {

    private static final AtomicLong ID = new AtomicLong (0);

    protected static final Object DISCARD = new Object ();
    
    protected ServerFactoryImpl _factory;
    protected Logger _logger;
    protected String _toString;
    protected InetSocketAddress _address;
    protected Map<String, Object> _props;
    protected Map<String, Object> _allProps = new HashMap<String, Object> ();
    protected int _maxConnections, _priority, _acceptedConnections;
    protected AtomicInteger _openConnections = new AtomicInteger (0); // modified in many threads
    protected long _soTimeout, _retry;
    protected List<Mask> _allowMasks, _rejectMasks;
    protected Map<InetAddress, Boolean> _allowAddresses, _rejectAddresses;
    protected PlatformExecutors _executors;
    protected ReactorProvider _provider;
    protected Reactor _reactor;
    protected Object _attachment;
    protected String _advertId, _advertName = null;
    protected int _portRange = 1;
    protected boolean _secure;
    protected long _healthCheckDelay = 0L;
    protected Future _checkRunnable;
    protected boolean _soTimeoutAutoClose, _soTimeoutReadWrite;
    protected boolean _checkIPAddress;
    protected boolean _closeAllOnClose;
    
    public ServerImpl (ServerFactoryImpl factory, Map<String, Object> procProps, Map<String, Object> props){
	_factory = factory;
	_props = props;
	Property.fillDefaultProperties (_allProps, _props);
	if (procProps != null) Property.fillDefaultProperties (_allProps, procProps);
	//system
	_executors = (PlatformExecutors) _allProps.get ("system.executors");
	_provider = (ReactorProvider) _allProps.get ("system.reactor.provider");
	if (_allProps.get (PROP_SERVER_IP) != null){
	    _address = new InetSocketAddress ((String)_allProps.get (PROP_SERVER_IP),
					      (Integer)_allProps.get (PROP_SERVER_PORT));
	} else if (_allProps.get (PROP_SERVER_IF) != null){
	    InetAddress ia = getAddressFromIf ((String) _allProps.get (PROP_SERVER_IF));
	    if (ia == null) throw new IllegalArgumentException ("Invalid interface : cannot resolve the address");
	    _address = new InetSocketAddress (ia,
					      (Integer)_allProps.get (PROP_SERVER_PORT));
	} else throw new IllegalArgumentException ("Missing IP parameters");
	_toString = new StringBuilder ().append ("Server[").append (getType ()).append (_address).append (']').toString ();
	_logger = ServerFactoryImpl.LOGGER;
    }
    public ServerImpl init (){
	// this init is not called in the constructor : hence superclass can finish the constructor prior to created()
	created ();
	// resume - may have been modified in created()
	_reactor = _factory.setReactor (_allProps);
	// advertizing
	_advertId = (String) _allProps.get (PROP_PROCESSOR_ADVERTIZE_ID);
	_advertName = (String) _allProps.get (PROP_PROCESSOR_ADVERTIZE_NAME);
	if (_advertName == null) _advertName = (String) _allProps.get (PROP_PROCESSOR_ID);
	//properties : they are dynamic
	applyProperties ();
	return this;
    }
    public void update (final Map<String, Object> props){
	Runnable r = new Runnable (){
		public void run (){
		    for (String key: _props.keySet ()){
			_allProps.remove (key);
		    }
		    for (String key: props.keySet ()){
			_allProps.put (key, props.get (key));
		    }
		    _props = props;
		    applyProperties ();
		    updated ();
		}
	    };
	execute (r);
    }
    protected void applyProperties (){
	_toString = (String) _allProps.get (PROP_SERVER_NAME); // can be null
	if (_toString == null){
	    _toString = new StringBuilder (getType ()).append ('[').append (_address).append (']').toString ();
	    _logger = Logger.getLogger (new StringBuilder (ServerFactoryImpl.PREFIX_DOT).append (getType ()).toString ());
	} else
	    _logger = Logger.getLogger (new StringBuilder (ServerFactoryImpl.PREFIX_DOT).append (_toString).toString ());
	setProperty (PROP_SERVER_LOGGER, _logger);
	_allowMasks = null;
	List<String> allow = Property.getStringListProperty (PROP_CLIENT_ALLOW_MASK, _allProps);
	List<String> allowSubnet = Property.getStringListProperty (PROP_CLIENT_ALLOW_SUBNET, _allProps);
	if (allowSubnet != null){
	    if (allow == null) allow = allowSubnet;
	    else allow.addAll (allowSubnet);
	}
	if (allow != null){
	    _allowMasks = new ArrayList<Mask> (allow.size ());
	    for (String mask : allow){
		try{
		    _allowMasks.add (getMask (mask));
		}catch(Exception e){
		    _logger.error ("Invalid mask : "+mask);
		}
	    }
	}
	_allowAddresses = null;
	allow = Property.getStringListProperty (PROP_CLIENT_ALLOW_IP, _allProps);
	if (allow != null){
	    _allowAddresses = new HashMap<> ();
	    for (String addr : allow){
		try{
		    _allowAddresses.put (InetAddress.getByName (addr), Boolean.TRUE);
		}catch(Exception e){
		    _logger.error ("Invalid ip address : "+addr);
		}
	    }
	}
	_rejectMasks = null;
	List<String> reject = Property.getStringListProperty (PROP_CLIENT_REJECT_MASK, _allProps);
	List<String> rejectSubnet = Property.getStringListProperty (PROP_CLIENT_REJECT_SUBNET, _allProps);
	if (rejectSubnet != null){
	    if (reject == null) reject = rejectSubnet;
	    else reject.addAll (rejectSubnet);
	}
	if (reject != null){
	    _rejectMasks = new ArrayList<Mask> (reject.size ());
	    for (String mask : reject){
		try{
		    _rejectMasks.add (getMask (mask));
		}catch(Exception e){
		    _logger.error ("Invalid mask : "+mask);
		}
	    }
	}
	_rejectAddresses = null;
	reject = Property.getStringListProperty (PROP_CLIENT_REJECT_IP, _allProps);
	if (reject != null){
	    _rejectAddresses = new HashMap<> ();
	    for (String addr : reject){
		try{
		    _rejectAddresses.put (InetAddress.getByName (addr), Boolean.TRUE);
		}catch(Exception e){
		    _logger.error ("Invalid ip address : "+addr);
		}
	    }
	}
	_maxConnections = Property.getIntProperty (PROP_CLIENT_MAX, _allProps, Integer.MAX_VALUE, false);
	_soTimeout = (long) Property.getIntProperty (PROP_READ_TIMEOUT, _allProps, -1, false);
	if (_soTimeout == -1L){ // not set - lets check read.timeout.close
	    long soTimeoutAutoCloseValue = (long) Property.getIntProperty (PROP_READ_TIMEOUT+".close", _allProps, -1, false);
	    if (soTimeoutAutoCloseValue == -1L){ // not set - lets check idle timeout (which means read & write)
		long idleTimeout = (long) Property.getIntProperty ("idle.timeout", _allProps, -1, false);
		if (idleTimeout == -1L){ // not set - lets check idle timeout autoclose (which means read & write)
		    long idleAutoCloseTimeout = (long) Property.getIntProperty ("idle.timeout.close", _allProps, -1, false);
		    if (idleAutoCloseTimeout == -1L){
			// nothing set
			_soTimeout = 0L;
		    } else {
			// use idle.timeout.close
			_soTimeoutReadWrite = true;
			_soTimeoutAutoClose = true;
			_soTimeout = idleAutoCloseTimeout;
		    }
		} else {
		    // use idle.timeout
		    _soTimeoutReadWrite = true;
		    _soTimeout = idleTimeout;
		}
	    } else {
		// use read.timeout.close
		_soTimeoutAutoClose = true;
		_soTimeout = soTimeoutAutoCloseValue;
	    }
	} // else use read.timeout directly
	
	_retry = (long) Property.getIntProperty (PROP_SERVER_RETRY, _allProps, 3000, false);
	_healthCheckDelay = (long) Property.getIntProperty (PROP_SERVER_CHECK, _allProps, 3000, false);
	_checkIPAddress = Property.getBooleanProperty ("server.ip.check", _allProps, true, false); // this is a private prop to disable check if needed
	_closeAllOnClose = Property.getBooleanProperty ("server.close.all", _allProps, false, false); // this is a prop to closeAllConnections on close
	switch (Property.getIntProperty (PROP_CLIENT_PRIORITY, _allProps, 1, false)){
	case 0: _priority = AsyncChannel.MIN_PRIORITY; break;
	case 1: _priority = AsyncChannel.MAX_PRIORITY; break;
	}
	_portRange = 1;
	String tmp = (String) _allProps.get (PROP_SERVER_PORT_RANGE);
	if (tmp != null){
	    try {
		_portRange = Integer.parseInt (tmp);
		if (_portRange <= 0) throw new Exception ("Invalid port range : "+tmp);
	    }
	    catch(Exception e){
		_logger.error ("Invalid port range : "+tmp);
		_portRange = 1;
	    }
	}
    }

    protected Security initSecurity (String prefix){
	_secure = Property.getBooleanProperty (PROP_SERVER_SECURE, _allProps, false, true);
	boolean standby = Property.getBooleanProperty (Server.PROP_SERVER_STANDBY, _allProps, false, false);
	// if we are in standby, we dont try to load the security for now (in case it is supposed to be loaded later - CSD case)
	if (_secure && !standby){
	    try{
		Security security = new Security();
		List<String> alpn = Property.getStringListProperty (TcpServer.PROP_TCP_SECURE_ALPN_PROTOCOL.replace ("tcp.", prefix), _allProps);
		if (alpn != null) for (String protocol : alpn) security.addApplicationProtocols (protocol);
		List<String> protocols = Property.getStringListProperty (TcpServer.PROP_TCP_SECURE_PROTOCOL.replace ("tcp.", prefix), _allProps);
		if (protocols != null) for (String protocol : protocols) security.addProtocol (protocol);
		List<String> ciphers = Property.getStringListProperty (TcpServer.PROP_TCP_SECURE_CIPHER.replace ("tcp.", prefix), _allProps);
		if (ciphers != null) for (String cipher : ciphers) security.addCipher (cipher);
		String ksfile = (String) _allProps.get (TcpServer.PROP_TCP_SECURE_KEYSTORE_FILE.replace ("tcp.", prefix));
		if (ksfile != null) security.keyStore (new FileInputStream(ksfile));
		String kspwd = (String) _allProps.get (TcpServer.PROP_TCP_SECURE_KEYSTORE_PWD.replace ("tcp.", prefix));
		if (kspwd != null) security.keyStorePassword (kspwd);
		String kstype = (String) _allProps.get (TcpServer.PROP_TCP_SECURE_KEYSTORE_TYPE.replace ("tcp.", prefix));
		if (kstype != null) security.keyStoreType (kstype);
		String ksalgo = (String) _allProps.get (TcpServer.PROP_TCP_SECURE_KEYSTORE_ALGO.replace ("tcp.", prefix));
		if (ksalgo != null) security.keyStoreAlgorithm (ksalgo);
		String epIdalgo = (String) _allProps.get (TcpServer.PROP_TCP_SECURE_ENDPOINT_IDENTITY_ALGO.replace ("tcp.", prefix));
		if (epIdalgo != null) security.endpointIdentificationAlgorithm (epIdalgo);
		boolean auth = Property.getBooleanProperty (TcpServer.PROP_TCP_SECURE_CLIENT_AUTHENTICATE.replace ("tcp.", prefix), _allProps, false, true);
		security.authenticateClients (auth);
		boolean order = Property.getBooleanProperty (TcpServer.PROP_TCP_SECURE_CIPHER_SUITES_ORDER.replace ("tcp.", prefix), _allProps, false, true);
		security.useCipherSuitesOrder (order);
		boolean delayed = Property.getBooleanProperty (TcpServer.PROP_TCP_SECURE_DELAYED.replace ("tcp.", prefix), _allProps, false, true);
		if (delayed) security.delayed ();
		List<String> snis = Property.getStringListProperty (TcpServer.PROP_TCP_SECURE_SNI.replace ("tcp.", prefix), _allProps);
		if (snis != null) security.setSNIMatcher (snis.toArray (new String[snis.size ()]));
		return security.build ();
	    }catch(Exception e){
		_logger.error (this+" : Failed to apply the security configuration", e);
		return null;
	    }
	} else {
	    return null;
	}
    }

    public Map<String, Object> getProperties (){
	return _allProps;
    }

    protected void setProperty (String key, Object value){
	_allProps.put (key, value);
    }

    public int getOpenConnections (){ return _openConnections.get ();}
    public int getAcceptedConnections (){ return _acceptedConnections;}

    public void execute (Runnable r){
	_reactor.schedule (r);
    }
    public <T> Future<T> execute (Callable<T> r){
	return _reactor.schedule (r);
    }
    
    public String toString (){
	return _toString;
    }

    public InetSocketAddress getAddress (){
	return _address;
    }
    public InetSocketAddress makeAddress (){
	return makeAddress ((Integer)_allProps.get (PROP_SERVER_PORT));
    }
    public InetSocketAddress makeAddress (int port){
	// CSFAR-318 - cannot re-use same InetSocketAddress object when re-trying to bind
	return new InetSocketAddress ((String)_allProps.get (PROP_SERVER_IP),
				      port);
    }

    public String getKey (){
	return (String) _allProps.get (PROP_SERVER_KEY);
    }
    public String getId (){
	return (String) _allProps.get (PROP_SERVER_ID);
    }

    public <T> T attachment (){
	return (T) _attachment;
    }

    public <T> T attach (Object attachment){
	try{ return (T) _attachment; }
	finally {_attachment = attachment;}
    }

    //********** State mgmt ***********//

    protected State _state = null;
    protected boolean _logFailed;
    protected Throwable _lastFailed;
    
    
    private class State {
	protected State (){}
	protected void enter (Object arg){
	    if (_logger.isDebugEnabled ())
	    	_logger.debug (ServerImpl.this+" : changing state from "+_state+" to "+this);
	    _state = this;
	}
	protected boolean isOpen (){ return false;}
	protected void open (){ throw new IllegalStateException ();};
	protected void close (boolean closeAllConnections){ throw new IllegalStateException ();};
	protected void stopListening (boolean closeAllConnections){ throw new IllegalStateException ();};
	protected void resumeListening (){ throw new IllegalStateException ();};
	protected void serverOpened (InetSocketAddress addr){
	    throw new IllegalStateException ();
	};
	protected void serverFailed (Throwable t){
	    _logger.warn (ServerImpl.this+" : state="+_state+" : unexpected serverFailed", t);
	    throw new IllegalStateException ();
	}
	protected void serverClosed (){
	    IllegalStateException t = new IllegalStateException ("serverClosed");
	    _logger.warn (ServerImpl.this+" : state="+_state+" : unexpected serverClosed", t);
	    throw t;
	}
    }
    private State STATE_INIT = new State (){
	    public String toString (){ return "[STATE_INIT]";}
	    @Override
	    protected void enter (Object arg){
		super.enter (arg);
		init ();
	    }
	    @Override
	    protected void open (){
		_logFailed = true;
		 STATE_OPENING.enter (null);
	    }
	};
    private State STATE_OPENING = new State (){
	    public String toString (){ return "[STATE_OPENING]";}
	    @Override
	    protected void enter (Object arg){
		super.enter (arg);
		boolean standby = Property.getBooleanProperty (Server.PROP_SERVER_STANDBY, _allProps, false, false);
		if (standby){
		    if (_logger.isTraceEnabled ()) _logger.trace (ServerImpl.this+" : in standby : no attempt to bind");
		    STATE_PENDING.enter (null);
		    return;
		}
		InetSocketAddress address = makeAddress ();
		if (address != null &&
		    _checkIPAddress &&
		    !checkIPAddress (address, _logger)){
		    if (_logger.isTraceEnabled ()) _logger.trace (ServerImpl.this+" : checkAddress failed : no attempt to bind");
		    address = null;
		}
		start (address);
	    }
	    @Override
	    protected void serverOpened (InetSocketAddress addr){
		_lastFailed = null;
		STATE_OPEN.enter (addr);
	    };
	    @Override
	    protected void serverFailed (Throwable t){
		if (t != null){
		    if (_lastFailed != null){
			// log if new error is different
			if (!t.getClass ().equals (_lastFailed.getClass ())){
			    _logFailed = true;
			} else if (!t.getMessage ().equals (_lastFailed.getMessage ())){
			    _logFailed = true;
			}
		    }
		    _lastFailed = t;
		    if (_logFailed){
			_logger.warn (ServerImpl.this+" : serverFailed : "+_lastFailed);
			_logFailed = false;
		    } else {
			if (_logger.isDebugEnabled ())
			    _logger.debug (ServerImpl.this+" : serverFailed : "+_lastFailed);
		    }
		}
		STATE_PENDING.enter (null);
	    }
	};
    private State STATE_OPEN = new State (){
	    public String toString (){ return "[STATE_OPEN]";}
	    private ServiceRegistration _advertisement;
	    @Override
	    protected void enter (Object arg){
		super.enter (arg);
		setSince (false, _allProps);
		String id = getType () + "-" + ID.incrementAndGet ();
		setProperty (PROP_SERVER_ID, id);
		_advertisement = _factory.advertize (_advertId, _advertName, (InetSocketAddress) arg, _allProps);
	    }
	    @Override
	    protected boolean isOpen (){ return true;}
	    @Override
	    protected void serverClosed (){
		unadvertize ();
		_logFailed = true;
		STATE_PENDING.enter (null);
	    }
	    @Override
	    protected void stopListening (boolean closeAllConnections){
		unadvertize ();
		stop ();
		STATE_SUSPENDED.enter (closeAllConnections);
	    }
	    @Override
	    protected void close (boolean closeAllConnections){
		unadvertize ();
		stop ();
		STATE_CLOSING.enter (closeAllConnections);
	    }
	    private void unadvertize (){
		_factory.unadvertize (_advertisement);
		_advertisement = null;
	    }
	};
    private State STATE_SUSPENDED = new State () {
	    public String toString (){ return "[STATE_SUSPENDED]";}
	    private boolean _closeAllConnections;
	    @Override
	    protected void enter (Object arg){
		super.enter (arg);
		_closeAllConnections = (Boolean) arg;
	    }
	    @Override
	    protected void serverClosed (){
		if (_closeAllConnections) closeAllConnections ();
	    }
	    @Override
	    protected void stopListening (boolean closeAllConnections){} // make idempotent here
	    @Override
	    protected void resumeListening (){
		_logFailed = true;
		STATE_OPENING.enter (null);
	    }
	    @Override
	    protected void close (boolean closeAllConnections){
		if (closeAllConnections) closeAllConnections ();
		STATE_CLOSED.enter (null);
	    }
	};
    private State STATE_PENDING = new State () {
	    public String toString (){ return "[STATE_PENDING]";}
	    private Future<?> _futureListen;
	    private Runnable _runnable = new Runnable (){
		    public void run (){ open ();}
		};
	    @Override
	    protected void enter (Object arg){
		super.enter (arg);
		if (_retry > 0)
		    _futureListen = _reactor.schedule (_runnable, _retry, java.util.concurrent.TimeUnit.MILLISECONDS);
		else
		    STATE_CLOSED.enter (null);
	    }
	    @Override
	    protected void stopListening (boolean closeAllConnections){
		_futureListen.cancel (false);
		_futureListen = null;
		if (closeAllConnections) closeAllConnections ();
		STATE_SUSPENDED.enter (false);
	    }
	    @Override
	    protected void open (){
		_futureListen = null;
		STATE_OPENING.enter (null);
	    }
	    @Override
	    protected void close (boolean closeAllConnections){
		_futureListen.cancel (false);
		_futureListen = null;
		if (closeAllConnections) closeAllConnections ();
		STATE_CLOSED.enter (null);
	    }
	};
    private State STATE_CLOSING = new State (){
	    public String toString (){ return "[STATE_CLOSING]";}
	    private Boolean _closeAllConnections;
	    @Override
	    protected void enter (Object arg){
		super.enter (arg);
		_closeAllConnections = (Boolean) arg;
	    }
	    @Override
	    protected void serverClosed (){
		if (_closeAllConnections) closeAllConnections ();
		STATE_CLOSED.enter (null);
	    }
	    @Override
	    protected void close (boolean closeAllConnections){}
	};
    private State STATE_CLOSED = new State (){
	    public String toString (){ return "[STATE_CLOSED]";}
	    @Override
	    protected void enter (Object arg){
		super.enter (arg);
		_factory.closed (ServerImpl.this);
		destroyed ();
	    }
	    @Override
	    protected void close (boolean closeAllConnections){}
	 };
    
    public ServerImpl open (){
	STATE_INIT.enter (null);
	// added a memory barrier to flush the init()
	synchronized (this){
	    attach (attachment ());
	}
	Runnable r = new Runnable (){
		public void run (){
		    synchronized (ServerImpl.this){
			attach (attachment ());
		    }
		    _state.open ();
		}
	    };
	execute (r);
	return this;
    }
    
    public boolean isOpen (){
	return _state.isOpen ();
    }

    protected void serverOpened (InetSocketAddress addr){
	_state.serverOpened (addr);
    }
    protected void serverFailed (Throwable t){
	_state.serverFailed (t);
    }
    protected void serverClosed (){
	_state.serverClosed ();
    }
    
    //****** implemented by sub-servers *****//
    protected abstract boolean start (InetSocketAddress address);
    protected abstract void stop ();
    protected abstract void closeAllConnections ();
    protected abstract void updated ();
    protected abstract String getType ();
    protected abstract void created ();
    protected abstract void destroyed ();

    public void close (boolean schedule){
	if (schedule){
	    Runnable r = new Runnable (){
		    public void run (){
			_state.close (_closeAllOnClose);
		    }
		};
	    execute (r);
	} else
	    close ();
    }

    public void stopListening (final boolean closeAllConnections){
	Runnable r = new Runnable (){
		    public void run (){
			_state.stopListening (closeAllConnections);
		    }
		};
	_reactor.getPlatformExecutor ().submit (r, ExecutorPolicy.INLINE);
    }
    public void resumeListening (){
	Runnable r = new Runnable (){
		    public void run (){
			_state.resumeListening ();
		    }
		};
	_reactor.getPlatformExecutor ().submit (r, ExecutorPolicy.INLINE);
    }
    public void closeAll (){
	Runnable r = new Runnable (){
		    public void run (){
			_state.close (true);
		    }
		};
	_reactor.getPlatformExecutor ().submit (r, ExecutorPolicy.INLINE);
    }
    public void close (){
	Runnable r = new Runnable (){
		    public void run (){
			_state.close (false);
		    }
		};
	_reactor.getPlatformExecutor ().submit (r, ExecutorPolicy.INLINE);
    }
    
    //********** connections mgmt ***********//

    protected boolean isAllowed (InetAddress remote){
	boolean allowed = true;
	if (_allowMasks != null){
	    allowed = false;
	    byte[] remoteB = remote.getAddress ();
	    for (Mask mask : _allowMasks){
		if (mask.matches (remoteB))
		    return true;
	    }
	}
	if (_allowAddresses != null){
	    allowed = false;
	    if (_allowAddresses.containsKey (remote))
		return true;
	}
	if (allowed == false) return false;
	if (_rejectMasks != null){
	    byte[] remoteB = remote.getAddress ();
	    for (Mask mask : _rejectMasks){
		if (mask.matches (remoteB))
		    return false;
	    }
	}
	if (_rejectAddresses != null){
	    if (_rejectAddresses.containsKey (remote))
		return false;
	}
	return true;
    }

    protected Map<String, Object> connectionAccepted (AsyncChannel channel){
	if (_openConnections.get () >= _maxConnections){ // we use >= in case the _maxConnections config was lowered while reached
	    if (_logger.isInfoEnabled ())
		_logger.info (this+" : rejecting incoming connection : "+channel+" : maximum reached");
	    reject (channel);
	    return null;
	}
	_openConnections.getAndIncrement ();
	channel.setWriteBlockedPolicy(WriteBlockedPolicy.DISABLE_READ);
	channel.setPriority (_priority);
	return getDefaultProperties ();
    }

    protected Map<String, Object> getDefaultProperties (){
	Map<String, Object> props = new HashMap<String, Object> ();
	props.putAll (_allProps);
	return props;
    }

    protected boolean connectionClosed(AsyncChannel cnx){
	if (cnx.attachment () == DISCARD) return false;
	_openConnections.getAndDecrement ();
	return true;
    }
    
    protected void reject (AsyncChannel channel){
	channel.attach (DISCARD);
	// we keep monothreaded else there may be parallelism
	channel.setInputExecutor (_executors.getCurrentThreadContext ().getCurrentExecutor ());
    }

    protected boolean startHealthCheck (){
	if (_healthCheckDelay > 0L){
	    if (getAddress ().getAddress ().isAnyLocalAddress ()){ // no need to check 0.0.0.0, for example in Docker
		if (_logger.isInfoEnabled ()) _logger.info (this+" : address is AnyLocalAddress : disabling healthcheck");
		return false;
	    }
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : starting healthcheck : delay="+_healthCheckDelay);
	    Runnable check = new Runnable (){
		    public void run (){
			try{
			    InetSocketAddress address = makeAddress (0);
			    if (_checkIPAddress && !checkIPAddress (address, _logger)) throw new IOException ("checkIPAddress returned false");
			    UdpChannel channel = _provider.udpBind (_reactor, address, VOID_LISTENER,
								    new HashMap<ReactorProvider.UdpOption,java.lang.Object> ());
			    channel.close ();
			    _checkRunnable = _reactor.schedule (this, _healthCheckDelay, java.util.concurrent.TimeUnit.MILLISECONDS);
			}catch(Exception e){
			    _logger.warn (ServerImpl.this+" : healthcheck failed : closing all", e);
			    stopListening (true);
			    Runnable rs = new Runnable (){
				    public void run (){
					_logger.warn (ServerImpl.this+" : resumeListening : trying to listen");
					resumeListening ();
				    }
				};
			    _reactor.schedule (rs, _healthCheckDelay, java.util.concurrent.TimeUnit.MILLISECONDS);
			}
		    }
		};	
	    _checkRunnable = _reactor.schedule (check, _healthCheckDelay, java.util.concurrent.TimeUnit.MILLISECONDS);
	    return true;
	} else {
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : healthcheck deactivated");
	    return false;
	}
    }
    protected void stopHealthCheck (){
	if (_checkRunnable != null) _checkRunnable.cancel (false);
    }

    protected Thread _ksWatchThread;
    protected void startKeyStoreWatch (String prefix){
	if (!_secure) return;
	boolean watch = Property.getBooleanProperty (TcpServer.PROP_TCP_SECURE_KEYSTORE_WATCH.replace ("tcp.", prefix), _allProps, true, false);
	if (!watch) return;
	Runnable r = new Runnable (){
		public void run (){
		    if (_logger.isInfoEnabled ())
			_logger.info (ServerImpl.this+" : KeyStoreWatch started");
		    try{
			WatchService watchService = FileSystems.getDefault().newWatchService();
			Path path = Paths.get((String) _allProps.get (TcpServer.PROP_TCP_SECURE_KEYSTORE_FILE.replace ("tcp.", prefix))).toAbsolutePath ();
			path.getParent ().register(watchService, 
						   StandardWatchEventKinds.ENTRY_CREATE, 
						   StandardWatchEventKinds.ENTRY_DELETE, 
						   StandardWatchEventKinds.ENTRY_MODIFY);
			whileloop : while (true){
			    WatchKey key = watchService.poll (3000, java.util.concurrent.TimeUnit.MILLISECONDS);
			    if (key == null) continue whileloop;
			    forloop : for (WatchEvent<?> event : key.pollEvents ()) {
				Object o = event.context ();
				if (o == null) continue forloop;
				Path p = (Path) o;
				if (!path.getFileName ().equals (p.getFileName ()))
				    continue forloop;
				WatchEvent.Kind kind = event.kind ();
				if (kind == StandardWatchEventKinds.ENTRY_CREATE){
				    _logger.warn (ServerImpl.this+" KeyStoreWatch : "+path+" : created");
				    execute (() -> {updateSecurity (true);});
				} else if (kind == StandardWatchEventKinds.ENTRY_DELETE){
				    // we assume this is prior to a create : do change the credentials for now
				    _logger.warn (ServerImpl.this+" KeyStoreWatch : "+path+" : deleted");
				} else if (kind == StandardWatchEventKinds.ENTRY_MODIFY){
				    _logger.warn (ServerImpl.this+" KeyStoreWatch : "+path+" : modified");
				    execute (() -> {updateSecurity (true);});
				}
			    }
			    key.reset();
			}
		    }catch(InterruptedException ie){
			_logger.info (ServerImpl.this+" : KeyStoreWatch stopped");
		    }catch(Exception e){
			_logger.error (ServerImpl.this+" : KeyStoreWatch exception / stopped", e);
			execute (() -> {stopKeyStoreWatch ();}); // for cleaning
		    }
		}
	    };
	_ksWatchThread = new Thread (r);
	_ksWatchThread.setName (this+"[KeyStoreWatch]");
	_ksWatchThread.setDaemon(true);
	_ksWatchThread.start ();
    }
    protected void stopKeyStoreWatch (){
	if (_ksWatchThread != null){
	    _ksWatchThread.interrupt ();
	    _ksWatchThread = null;
	}
    }
    
    protected abstract boolean updateSecurity (boolean watchEvent);
    
    /********************* utilities *******************/

    private static boolean checkIPAddress (InetSocketAddress sockAddr, Logger logger){
	try{
	    InetAddress addr = sockAddr.getAddress ();
	    if (addr.isAnyLocalAddress ()) return true; // wildcard
	    // check the if
	    Enumeration<NetworkInterface> enumNI = NetworkInterface.getNetworkInterfaces ();
	    while (enumNI.hasMoreElements ()){
		NetworkInterface ni = enumNI.nextElement ();
		Enumeration<InetAddress> enumAdds = ni.getInetAddresses ();
		while (enumAdds.hasMoreElements ()){
		    if (addr.equals (enumAdds.nextElement ())) return true;
		}
		Enumeration<NetworkInterface> enumSI = ni.getSubInterfaces ();
		while (enumSI.hasMoreElements ()){
		    NetworkInterface si = enumSI.nextElement ();
		    Enumeration<InetAddress> enumSubAdds = si.getInetAddresses ();
		    while (enumSubAdds.hasMoreElements ()){
			if (addr.equals (enumSubAdds.nextElement ())) return true;
		    }
	    
		}
	    }
	}catch (Exception e){
	    // this is risky code --> play it safe
	    if (logger.isDebugEnabled ()) logger.debug ("checkIPAddress exception while checking "+sockAddr, e);
	    return true;
	}
	return false;
    }

    private static InetAddress getAddressFromIf (String name){
	try{
	    boolean v4 = name.endsWith ("/v4");
	    boolean v6 = name.endsWith ("/v6");
	    if (v4 || v6) name = name.substring (0, name.indexOf ('/'));
	    if (!v4 && !v6) v4 = v6 = true;
	    NetworkInterface inter = NetworkInterface.getByName (name);
	    if (inter == null){
		Enumeration<NetworkInterface> e1 = NetworkInterface.getNetworkInterfaces ();
		while (e1.hasMoreElements ()){
		    Enumeration<NetworkInterface> e2 = e1.nextElement ().getSubInterfaces ();
		    while (e2.hasMoreElements ()){
			NetworkInterface sub = e2.nextElement ();
			if (sub.getName ().equals (name))
			    inter = sub;
		    }
		}
	    }
	    if (inter == null) return null;
	    InetAddress addr = null;
	    for (InterfaceAddress ifaddr: inter.getInterfaceAddresses ()){
		InetAddress ia = ifaddr.getAddress ();
		if (v4){
		    if (ia instanceof Inet4Address){
			if ((addr = checkIfBelongsToSub (inter, ia)) != null) break;
		    }
		}
		if (v6){
		    if (ia instanceof Inet6Address){
			if ((addr = checkIfBelongsToSub (inter, ia)) != null) break;
		    }
		}
	    }
	    return addr;
	}catch(Exception e){
	    return null;
	}
    }

    private static InetAddress checkIfBelongsToSub (NetworkInterface inter, InetAddress addr) throws Exception {
	Enumeration<NetworkInterface> e = inter.getSubInterfaces ();
	while (e.hasMoreElements ()){
	    NetworkInterface sub = e.nextElement ();
	    Enumeration<InetAddress> ads = sub.getInetAddresses ();
	    while (ads.hasMoreElements ()){
		InetAddress ad = ads.nextElement ();
		if (ad.equals (addr)) return null;
	    }
	}
	return addr;
    }
    
    private static Mask getMask (String maskValue) throws Exception {
	int i = maskValue.indexOf ('/');
	if (i == -1 || i == 0) throw new Exception ();
	String ip = maskValue.substring (0, i);
	InetAddress addr = InetAddress.getByName (ip);
	Mask mask = new Mask ();
	mask.subnet = addr.getAddress ();
	mask.mask = new byte[mask.subnet.length];
	int bits = Integer.parseInt (maskValue.substring (i+1));
	if (bits < 1 || bits > mask.subnet.length*8) throw new Exception ();
	int bytes = bits/8;
	for (int k=0; k<bytes; k++)
	    mask.mask[k] = (byte) 0xFF;
	int left = bits%8;
	if (left > 0){
	    i = 0;
	    for (int k=0; k<left; k++){
		i <<= 1;
		i |= 1;
	    }
	    for (int k=0; k<8-left; k++) i <<= 1;
	    mask.mask[bytes] = (byte) i;
	}
	// clean subnet
	for (i=0; i<mask.subnet.length; i++)
	    mask.subnet[i] = (byte) (mask.subnet[i] & mask.mask[i]);
	return mask;
    }

    private static class Mask {
	private byte[] subnet;
	private byte[] mask;
	private boolean matches (InetAddress address){
	    return matches (address.getAddress ());
	}
	private boolean matches (byte[] addressB){
	    for (int i=0; i<addressB.length; i++)
		if ((byte)(addressB[i] & mask[i]) != subnet[i]) return false;
	    return true;
	}
    }
    
    public static void main (String[] s) throws Exception {
	Mask mask = getMask ("205.0.0.0/8");
	System.err.println (mask.matches (InetAddress.getByName ("205.0.0.1")) == true);
	System.err.println (mask.matches (InetAddress.getByName ("127.0.0.1")) == false);
	mask = getMask ("128.0.0.0/1");
	System.err.println (mask.matches (InetAddress.getByName ("128.0.0.1")) == true);
	System.err.println (mask.matches (InetAddress.getByName ("1.0.0.1")) == false);
	mask = getMask ("128.192.0.0/10");
	System.err.println (mask.matches (InetAddress.getByName ("128.195.1.1")) == true);
	System.err.println (mask.matches (InetAddress.getByName ("128.128.0.1")) == false);
	mask = getMask ("127.0.0.1/32");
	System.err.println (mask.matches (InetAddress.getByName ("127.0.0.1")) == true);
	System.err.println (mask.matches (InetAddress.getByName ("1.1.1.1")) == false);
	try{getMask ("127.0.0.1/0");System.err.println ("false");}catch(Exception e){System.err.println ("true");}
	try{getMask ("127.0.0.1/-1");System.err.println ("false");}catch(Exception e){System.err.println ("true");}
	try{getMask ("127.0.0.1/33");System.err.println ("false");}catch(Exception e){System.err.println ("true");}
	try{getMask ("127.0.0.1/a");System.err.println ("false");}catch(Exception e){System.err.println ("true");}
    }

    private static UdpChannelListener VOID_LISTENER = new UdpChannelListener (){
	    public void connectionOpened(UdpChannel cnx){}
	    public void connectionFailed(UdpChannel cnx, Throwable err){}
	    public void connectionClosed(UdpChannel cnx){}
	    public void receiveTimeout(UdpChannel cnx){}
	    public void messageReceived(final UdpChannel cnx,
					final java.nio.ByteBuffer buffer,
					final InetSocketAddress from){}
	    public void writeBlocked(UdpChannel cnx){}
	    public void writeUnblocked(UdpChannel cnx){}
	};
}
