package com.alcatel.as.ioh.impl.server;

import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alcatel.as.ioh.impl.conf.Property;
import com.alcatel.as.ioh.impl.tools.SctpUtils;
import com.alcatel.as.ioh.server.SctpServer;
import com.alcatel.as.ioh.server.SctpServerProcessor;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.util.sctp.SctpSocketOption;
import com.alcatel.as.util.sctp.SctpSocketParam;

import alcatel.tess.hometop.gateways.reactor.AsyncChannel;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.SctpChannel;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannel;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannelListener;
import alcatel.tess.hometop.gateways.reactor.Security;

public class SctpServerImpl extends ServerImpl implements SctpServer, SctpServerChannelListener {

    public static final List<SctpServerImpl> _servers = new ArrayList<SctpServerImpl> ();
    public static final ConcurrentHashMap<SctpChannel, Map<String, Object>> _connections = new ConcurrentHashMap<SctpChannel, Map<String, Object>> ();
    
    private SctpServerChannel _serverChannel;
    private SctpServerProcessor _proc;
    private Map<ReactorProvider.SctpServerOption, Object> _options = new HashMap<ReactorProvider.SctpServerOption, Object> ();
    private long _clientId = 0L;
    private boolean _disableReading;
    
    public SctpServerImpl (ServerFactoryImpl factory, SctpServerProcessor proc, Map<String, Object> procProps, Map<String, Object> props){
	super (factory, procProps, props);
	_proc = proc;
	setProperty (PROP_SERVER_PROCESSOR, _proc);
    }

    public SctpServerProcessor getProcessor (){
	return _proc;
    }

    protected String getType (){
	return "sctp";
    }

    protected void closeAllConnections (){
    	_logger.warn (this+" : closeAllConnections");
    	String key = getKey ();
    	for (Map.Entry<SctpChannel, Map<String, Object>> e : _connections.entrySet()) {
    		SctpChannel cnx = e.getKey();
    		Map<String, Object> props = e.getValue();
    		if (key.equals (props.get (PROP_SERVER_KEY))){
    			if (_logger.isDebugEnabled ()) _logger.debug (this+" : closeAllConnections : closing : "+cnx);
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
		if (_secure && _options.get (ReactorProvider.SctpServerOption.SECURITY)==null){
		    // the ks could not be loaded at creation - try again
		    if (updateSecurity (false) == false){
			serverConnectionFailed (null, new BindException ("Invalid security credentials"));
			return false;
		    }
		}
		_serverChannel = _provider.sctpAccept (_reactor,
						       addr,
						       this,
						       _options);
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
	Security security = initSecurity ("sctp.");
	if (security != null){
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : update security credentials");
	    _options.put(ReactorProvider.SctpServerOption.SECURITY, security);
	    if (_serverChannel != null){
		_serverChannel.updateSecurity (security);
		if (!watchEvent){
		    stopKeyStoreWatch ();
		    startKeyStoreWatch ("sctp.");
		}
	    }
	    return true;
	}
	return false;
    }

    protected void created (){
	_proc.serverCreated (this);
	// the proc may have modified/set some props
	_options.put (ReactorProvider.SctpServerOption.MAX_IN_STREAMS,
		      Property.getIntProperty (PROP_STREAM_IN, _allProps, 1, true));//TODO check default
	_options.put (ReactorProvider.SctpServerOption.MAX_OUT_STREAMS,
		      Property.getIntProperty (PROP_STREAM_OUT, _allProps, 1, true));//TODO check default
	List<String> list = Property.getStringListProperty (PROP_SERVER_IP_SECONDARY, _allProps);
	if (list != null){
	    InetAddress[] addrs = new InetAddress[list.size ()];
	    int i=0;
	    try{
		for (String s : list) addrs[i++] = InetAddress.getByName (s);
		_options.put (ReactorProvider.SctpServerOption.SECONDARY_LOCAL_ADDRS, addrs);
	    }catch(Exception e){
		_logger.error ("Failed to set secondary addresses : "+list, e);
	    }
	}
	_options.put (ReactorProvider.SctpServerOption.SO_RCVBUF, Property.getIntProperty (PROP_READ_BUFFER, _allProps, 0, false));
	_options.put (ReactorProvider.SctpServerOption.SO_SNDBUF, Property.getIntProperty (PROP_WRITE_BUFFER, _allProps, 0, false));
	_options.put (ReactorProvider.SctpServerOption.USE_DIRECT_BUFFER,
		      Property.getBooleanProperty (PROP_READ_BUFFER_DIRECT, _allProps, false, true));
	_options.put (ReactorProvider.SctpServerOption.DISABLE_ACCEPT_TIMEOUT,
		      Property.getLongProperty (PROP_SERVER_ACCEPT_THROTTLING, _allProps, 0L, false));
	long closeTimeout = Property.getLongProperty (PROP_CLOSE_TIMEOUT, _allProps, -1L, false);
	if (closeTimeout >= 0L) _options.put (ReactorProvider.SctpServerOption.LINGER, closeTimeout);

	Security security = initSecurity ("sctp.");
	if (security != null)
	    _options.put(ReactorProvider.SctpServerOption.SECURITY, security);

	Map<SctpSocketOption, SctpSocketParam> sctpOptions = SctpUtils.createSctpOptions(_allProps);
	if (sctpOptions.size () > 0) _options.put(ReactorProvider.SctpServerOption.SOCKET_OPTIONS, sctpOptions);

	_disableReading = !Property.getBooleanProperty (PROP_READ_ENABLED, _allProps, false, true);
    }

    protected void destroyed (){
	_proc.serverDestroyed (this);
    }
    
    public SctpServerChannel getServerChannel (){
	return _serverChannel;
    }
    
    /************************************************************
     *       Implementation of SctpServerChannelListener *
     ************************************************************/

    public void serverConnectionOpened(SctpServerChannel server){
	_logger.warn (this+" : opened");
	try{
	    serverOpened ((InetSocketAddress)_serverChannel.getAllLocalAddresses ().iterator ().next ());
	}catch(Throwable t){
	    _logger.warn (this+" : Exception while calling getAllLocalAddresses", t);
	}
	_clientId = 0L;
	synchronized (_servers){
	    _servers.add (this);
	}
	startHealthCheck ();
	startKeyStoreWatch ("sctp.");
	_proc.serverOpened (this);
    }
    public void serverConnectionFailed(SctpServerChannel server, Throwable err){
	_serverChannel = null;
	_proc.serverFailed (this, new BindException ("address not bindable"));
	serverFailed (err);
    }    
    public void serverConnectionClosed(SctpServerChannel server, Throwable t){
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

    public void connectionAccepted(final SctpServerChannel serverChannel,
				   final SctpChannel acceptedChannel){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : connectionAccepted : "+acceptedChannel);
	_acceptedConnections++;
	try{
	    for (SocketAddress addr : acceptedChannel.getRemoteAddresses ()){
		InetSocketAddress inet = (InetSocketAddress) addr;
		if (isAllowed (inet.getAddress ()) == false){
		    _logger.warn (this+" : rejecting incoming connection : not allowed : "+inet.getAddress ());
		    reject (acceptedChannel);
		    return;
		}
	    }
	}catch(Exception e){
	    _logger.warn (this+" : exception while checking if connection was allowed", e);
	    reject (acceptedChannel);
	    return;
	}

	final Map<String, Object> props = super.connectionAccepted (acceptedChannel);
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
	_proc.connectionAccepted (SctpServerImpl.this, acceptedChannel, props);
    }

    @Override
    protected void reject (AsyncChannel channel){
	super.reject (channel);
	channel.shutdown (); // for sctp we dont do close : data could still be read
    }

    /********* the following extend SctpChannelListener ************/
    
    public void receiveTimeout(SctpChannel cnx){
	if (_soTimeoutAutoClose)
	    cnx.shutdown ();
	else
	    _proc.getChannelListener (cnx).receiveTimeout (cnx);
    }

    public void writeBlocked(SctpChannel cnx){
	_proc.getChannelListener (cnx).writeBlocked (cnx);
    }

    public void writeUnblocked(SctpChannel cnx){
	_proc.getChannelListener (cnx).writeUnblocked (cnx);
    }

    public void connectionClosed(SctpChannel cnx, Throwable t){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : connectionClosed : "+cnx);
	if (super.connectionClosed (cnx)){
		_connections.remove (cnx);
	    _proc.getChannelListener (cnx).connectionClosed (cnx, t);
	}
    }

    public void messageReceived(SctpChannel cnx,
				java.nio.ByteBuffer buffer,
				java.net.SocketAddress addr,
				int bytes,
				boolean isComplete,
				boolean isUnordered,
				int ploadPID,
				int streamNumber){
	_proc.getChannelListener (cnx).messageReceived (cnx, buffer, addr, bytes, isComplete, isUnordered, ploadPID, streamNumber);
    }

    public void sendFailed(SctpChannel cnx,
			   java.net.SocketAddress addr,
			   java.nio.ByteBuffer buf,
			   int errcode,
			   int streamNumber){
	_proc.getChannelListener (cnx).sendFailed (cnx, addr, buf, errcode, streamNumber);
    }

    @Override
    public void peerAddressChanged(SctpChannel cnx, SocketAddress addr, AddressEvent event) {
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : peerAddressChanged : "+cnx+" : "+addr+" : "+event);
	_proc.getChannelListener(cnx).peerAddressChanged(cnx, addr, event);      
    }   
}
