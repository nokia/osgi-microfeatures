package com.alcatel.as.ioh.impl.server;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alcatel.as.ioh.impl.conf.Property;
import com.alcatel.as.ioh.server.TcpServer;
import com.alcatel.as.ioh.server.TcpServerProcessor;
import com.alcatel.as.service.concurrent.PlatformExecutor;

import alcatel.tess.hometop.gateways.reactor.AsyncChannel;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;

public class TcpServerImpl extends ServerImpl implements TcpServer, TcpServerChannelListener {

    public static final List<TcpServerImpl> _servers = new ArrayList<TcpServerImpl> ();
    public static final ConcurrentHashMap<TcpChannel, Map<String, Object>> _connections = new ConcurrentHashMap<TcpChannel, Map<String, Object>> ();
    
    private TcpServerChannel _serverChannel;
    private TcpServerProcessor _proc;
    private Map<ReactorProvider.TcpServerOption, Object> _options = new HashMap<ReactorProvider.TcpServerOption, Object> ();
    private long _clientId = 0L;
    private boolean _disableReading;
    
    public TcpServerImpl (ServerFactoryImpl factory, TcpServerProcessor proc, Map<String, Object> procProps, Map<String, Object> props) throws Exception {
	super (factory, procProps, props);
	_proc = proc;
	setProperty (PROP_SERVER_PROCESSOR, _proc);
    }

    public TcpServerProcessor getProcessor (){
	return _proc;
    }

    protected String getType (){
	return "tcp";
    }

    protected void closeAllConnections (){
		_logger.warn(this + " : closeAllConnections");
		String key = getKey();
		for (Map.Entry<TcpChannel, Map<String, Object>> e : _connections.entrySet()) {
			TcpChannel cnx = e.getKey();
			Map<String, Object> props = e.getValue();
			if (key.equals(props.get(PROP_SERVER_KEY))) {
				if (_logger.isDebugEnabled())
					_logger.debug(this + " : closeAllConnections : closing : " + cnx);
				_proc.closeConnection (cnx);
			}
		}
    }

    protected void stop (){
	_serverChannel.close ();
    }
    
    protected boolean start (InetSocketAddress addr){
	if (addr != null){
	    try{
		if (_secure && _options.get (ReactorProvider.TcpServerOption.SECURITY)==null){
		    // the ks could not be loaded at creation - try again
		    if (updateSecurity (false) == false){
			serverConnectionFailed (null, new BindException ("Invalid security credentials"));
			return false;
		    }
		}
		int attempts = 0;
		while (true){
		    try{
			attempts++;
			_serverChannel = _provider.tcpAccept (_reactor,
							      addr,
							      this,
							      _options);
			break;
		    }catch(java.net.BindException se){
			// no way to find why the bind failed
			if (attempts < _portRange){
			    int port = addr.getPort ()+1;
			    if (port <= 0xFFFF){
				addr = new InetSocketAddress (addr.getAddress (), port);
				continue;
			    }
			}
			throw se;
		    }
		}
		serverConnectionOpened (_serverChannel);
		return true;
	    }catch(Throwable t){
		serverConnectionFailed (null, t);
		return false;
	    }
	}else{
	    serverConnectionFailed (null, new BindException ("IP is not available on this host"));
	    return false;
	}
    }
    
    protected void updated (){
	updateSecurity (false);
	_proc.serverUpdated (this);
    }
    protected boolean updateSecurity (boolean watchEvent){
	Security security = initSecurity ("tcp.");
	if (security != null){
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : update security credentials");
	    _options.put(ReactorProvider.TcpServerOption.SECURITY, security);
	    if (_serverChannel != null){
		_serverChannel.updateSecurity (security);
		if (!watchEvent){
		    stopKeyStoreWatch ();
		    startKeyStoreWatch ("tcp.");
		}
	    }
	    return true;
	}
	return false;
    }

    protected void created (){
	_proc.serverCreated (this);
	// the proc may have modified/set some props
	_options.put (ReactorProvider.TcpServerOption.TCP_NO_DELAY,
		      Property.getBooleanProperty (PROP_TCP_NO_DELAY, _allProps, true, true));
	_options.put (ReactorProvider.TcpServerOption.SO_RCVBUF, Property.getIntProperty (PROP_READ_BUFFER, _allProps, 0, false));
	_options.put (ReactorProvider.TcpServerOption.SO_SNDBUF, Property.getIntProperty (PROP_WRITE_BUFFER, _allProps, 0, false));
	_options.put (ReactorProvider.TcpServerOption.USE_DIRECT_BUFFER,
		      Property.getBooleanProperty (PROP_READ_BUFFER_DIRECT, _allProps, false, true));
	_options.put (ReactorProvider.TcpServerOption.DISABLE_ACCEPT_TIMEOUT,
		      Property.getLongProperty (PROP_SERVER_ACCEPT_THROTTLING, _allProps, 0L, false));
	long closeTimeout = Property.getLongProperty (PROP_CLOSE_TIMEOUT, _allProps, -1L, false);
	if (closeTimeout >= 0L) _options.put (ReactorProvider.TcpServerOption.LINGER, closeTimeout);

	Security security = initSecurity ("tcp.");
	if (security != null)
	    _options.put(ReactorProvider.TcpServerOption.SECURITY, security);
	_options.put (ReactorProvider.TcpServerOption.SECURE, _secure); // legacy stuff - kept for precaution

	_disableReading = !Property.getBooleanProperty (PROP_READ_ENABLED, _allProps, false, true);
    }

    protected void destroyed (){
	_proc.serverDestroyed (this);
    }

    public TcpServerChannel getServerChannel (){
	return _serverChannel;
    }

    /************************************************************
     *       Implementation of TcpServerChannelListener *
     ************************************************************/

    public void serverConnectionOpened(TcpServerChannel channel){
	_logger.warn (this+" : opened");
	serverOpened (_serverChannel.getLocalAddress ());
	_clientId = 0L;
	synchronized (_servers){
	    _servers.add (this);
	}
	startHealthCheck ();
	startKeyStoreWatch ("tcp.");
	_proc.serverOpened (this);
    }
    public void serverConnectionFailed(TcpServerChannel channel, Throwable t){
	_serverChannel = null;
	_proc.serverFailed (this, new BindException ("address not bindable"));
	serverFailed (t);
    }
    public void serverConnectionClosed(TcpServerChannel server){
	_logger.warn (this+" : closed");
	stopHealthCheck ();
	stopKeyStoreWatch ();
	_proc.serverClosed (this);
	_serverChannel = null;
	synchronized (_servers){
	    _servers.remove (this);
	}
	serverClosed ();
    }

    public void connectionFailed(TcpServerChannel serverChannel,
				 java.lang.Throwable err){
	_logger.warn (this+" : connectionFailed", err);
    }
    
    public void connectionAccepted(final TcpServerChannel serverChannel,
				   final TcpChannel acceptedChannel){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : connectionAccepted : "+acceptedChannel);

	_acceptedConnections++;
	if (isAllowed (acceptedChannel.getRemoteAddress ().getAddress ()) == false){
	    _logger.warn (this+" : rejecting incoming connection : not allowed : "+acceptedChannel.getRemoteAddress ().getAddress ());
	    reject (acceptedChannel);
	    return;
	}

	Map<String, Object> props = super.connectionAccepted (acceptedChannel);
	if (props == null) return; // was rejected

	if (_disableReading)
	    acceptedChannel.disableReading ();
	
	PlatformExecutor inputExec = _executors.createQueueExecutor (_executors.getProcessingThreadPoolExecutor ());
	props.put (PROP_READ_EXECUTOR, inputExec);
	acceptedChannel.setInputExecutor (inputExec);

	// call setSoTimeout AFTER setInputExecutor
	if (_soTimeout > 0) acceptedChannel.setSoTimeout (_soTimeout, !_soTimeoutReadWrite);
	
	props.put (PROP_CLIENT_ID, new StringBuilder ().append (getId ()).append ('-').append (++_clientId).toString ());
	setSince (true, props);
	_connections.put (acceptedChannel, props);
	_proc.connectionAccepted (TcpServerImpl.this, acceptedChannel, props);
    }

    @Override
    protected void reject (AsyncChannel channel){
	super.reject (channel);
	channel.shutdown ();
    }

    /********* the following extends TcpChannelListener ************/
    
    public void receiveTimeout(TcpChannel cnx){
	if (_soTimeoutAutoClose)
	    cnx.shutdown ();
	else
	    _proc.getChannelListener (cnx).receiveTimeout (cnx);
    }

    public int messageReceived(TcpChannel cnx,
			       java.nio.ByteBuffer buffer){
	return _proc.getChannelListener (cnx).messageReceived (cnx, buffer);
    }

    public void writeBlocked(TcpChannel cnx){
	_proc.getChannelListener (cnx).writeBlocked (cnx);
    }

    public void writeUnblocked(TcpChannel cnx){
	_proc.getChannelListener (cnx).writeUnblocked (cnx);
    }

    public void connectionClosed(TcpChannel cnx){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : connectionClosed : "+cnx);
	if (super.connectionClosed (cnx)){
		_connections.remove (cnx);
	    _proc.getChannelListener (cnx).connectionClosed (cnx);
	}
    }
}
