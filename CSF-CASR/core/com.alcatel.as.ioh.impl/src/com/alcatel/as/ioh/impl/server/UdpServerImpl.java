package com.alcatel.as.ioh.impl.server;

import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.atomic.*;

import org.apache.log4j.Logger;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.impl.conf.Property;

import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;

public class UdpServerImpl extends ServerImpl implements UdpServer, UdpChannelListener {

    private UdpChannel _serverChannel;
    private UdpServerProcessor _proc;
    private Map<ReactorProvider.UdpOption, Object> _udpOptions = new HashMap<ReactorProvider.UdpOption, Object> ();
    private PlatformExecutor _inputExec;
    
    public UdpServerImpl (ServerFactoryImpl factory, UdpServerProcessor proc, Map<String, Object> procProps, Map<String, Object> props){
	super (factory, procProps, props);
	_proc = proc;
	setProperty (PROP_SERVER_PROCESSOR, _proc);
	_inputExec = _executors.createQueueExecutor (_executors.getProcessingThreadPoolExecutor ());
	_udpOptions.put (ReactorProvider.UdpOption.INPUT_EXECUTOR, _inputExec);
	_udpOptions.put (ReactorProvider.UdpOption.ENABLE_READ, false);
	setProperty (PROP_READ_EXECUTOR, _inputExec);
    }

    protected String getType (){
	return "udp";
    }

    protected void closeAllConnections (){}
    
    protected void stop (){
	_serverChannel.close ();
    }
    
    protected boolean start (InetSocketAddress addr){
	if (addr != null){
	    try{
		if (_secure && _udpOptions.get (ReactorProvider.UdpOption.SECURITY)==null){
		    // the ks could not be loaded at creation - try again
		    if (updateSecurity (false) == false){
			connectionFailed (null, new BindException ("Invalid security credentials"));
			return false;
		    }
		}
		_serverChannel = _provider.udpBind (_reactor,
						    addr,
						    this,
						    _udpOptions);
		_serverChannel.setSoTimeout (_soTimeout);
		_serverChannel.setPriority (_priority);
		connectionOpened (_serverChannel);
		return true;
	    }catch(final Throwable t){
		connectionFailed (null, t);
		return false;
	    }
	}else{
	    connectionFailed (null, new BindException ("IP is not available on this host"));
	    return false;
	}
    }

    protected void updated (){
	updateSecurity (false);
	_proc.serverUpdated (this);
    }
    protected boolean updateSecurity (boolean watchEvent){
	Security security = initSecurity ("udp.");
	if (security != null){
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : update security credentials");
	    _udpOptions.put(ReactorProvider.UdpOption.SECURITY, security);
	    if (_serverChannel != null){
		_serverChannel.updateSecurity (security);
		if (!watchEvent){
		    stopKeyStoreWatch ();
		    startKeyStoreWatch ("udp.");
		}
	    }
	    return true;
	}
	return false;
    }

    protected void created (){
	_proc.serverCreated (this);
	// the proc may have modified/set some props
	_udpOptions.put (ReactorProvider.UdpOption.SO_RCVBUF, Property.getIntProperty (PROP_READ_BUFFER, _allProps, 0, false));
	_udpOptions.put (ReactorProvider.UdpOption.SO_SNDBUF, Property.getIntProperty (PROP_WRITE_BUFFER, _allProps, 0, false));
	_udpOptions.put (ReactorProvider.UdpOption.USE_DIRECT_BUFFER,
			 Property.getBooleanProperty (PROP_READ_BUFFER_DIRECT, _allProps, false, true));

	Security security = initSecurity ("udp.");
	if (security != null)
	    _udpOptions.put(ReactorProvider.UdpOption.SECURITY, security);
    }

    protected void destroyed (){
	_proc.serverDestroyed (this);
    }

    public UdpChannel getServerChannel (){
	return _serverChannel;
    }

    /************************************************************
     *       Implementation of UdpChannelListener *
     ************************************************************/

    public void connectionOpened(UdpChannel cnx){
	_logger.warn (this+" : opened");
	serverOpened (_serverChannel.getLocalAddress ());
	startHealthCheck ();
	startKeyStoreWatch ("udp.");
	_proc.serverOpened (this);
	// the proc MUST call cnx.enableReading () at some point
    }
    public void connectionFailed(UdpChannel cnx, Throwable err){
	_serverChannel = null;
	_proc.serverFailed (this, new BindException ("address not bindable"));
	serverFailed (err);
    }
    
    public void connectionClosed(UdpChannel cnx){
	_proc.getChannelListener (cnx).connectionClosed (cnx);
	Runnable r = new Runnable (){
		public void run (){
		    stopHealthCheck ();
		    stopKeyStoreWatch ();	
		    _logger.warn (UdpServerImpl.this+" : closed");
		    _proc.serverClosed (UdpServerImpl.this);
		    _serverChannel = null;
		    serverClosed ();
		}};
	execute (r);
    }

    public void receiveTimeout(UdpChannel cnx){
	_proc.getChannelListener (cnx).receiveTimeout (cnx);
    }

    public void messageReceived(final UdpChannel cnx,
				final java.nio.ByteBuffer buffer,
				final InetSocketAddress from){
	// isAllowed is not thread safe
	//if (isAllowed (from.getAddress ()) == false){
	//    _logger.warn (this+" : rejecting incoming message : not allowed : "+from.getAddress ());
	//    return;
	//}
	
	_proc.getChannelListener (cnx).messageReceived (cnx, buffer, from);
    }

    public void writeBlocked(UdpChannel cnx){
	_proc.getChannelListener (cnx).writeBlocked (cnx);
    }

    public void writeUnblocked(UdpChannel cnx){
	_proc.getChannelListener (cnx).writeUnblocked (cnx);
    }
}
