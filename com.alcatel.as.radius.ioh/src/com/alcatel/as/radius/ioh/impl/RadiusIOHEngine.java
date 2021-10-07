package com.alcatel.as.radius.ioh.impl;

import com.alcatel.as.radius.parser.*;
import com.alcatel.as.radius.ioh.*;

import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.tools.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.tools.ChannelWriter.SendBufferMonitor;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.*;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;

import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;
import com.alcatel_lucent.as.management.annotation.config.*;
import com.alcatel.as.service.discovery.*;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;
import com.nextenso.mux.*;
import com.nextenso.mux.util.MuxIdentification;
import com.nextenso.mux.MuxFactory.ConnectionListener;

import static com.alcatel.as.radius.ioh.impl.RadiusIOH.getBooleanProperty;
import static com.alcatel.as.radius.ioh.impl.RadiusIOH.getIntProperty;

public class RadiusIOHEngine extends IOHEngine {

    private RadiusIOHRouter _router;
    private BundleContext _osgi;
    
    protected Meter _parserErrorMeter;
    protected RadiusIOHMeters _radiusReadMeters, _radiusWriteMeters;
    
    protected RadiusIOHEngine (String name, IOHServices services, RadiusIOHRouterFactory routerFactory){
	super (name, services);
	_router = routerFactory.newRadiusIOHRouter ();
    }

    public RadiusIOHRouter getRadiusIOHRouter (){ return _router;}
    
    public IOHEngine init (RadiusIOH ioh, TcpServer server, BundleContext osgi, Dictionary<String, String> system){
	_osgi = osgi;
	server.getProperties ().put (PROP_UDP, "true");
	server.getProperties ().put (PROP_TCP, "false");
	server.getProperties ().put (PROP_SCTP, "false");

	server.getProperties ().put (PROP_UDP_BIND_SHARED, "false");
	server.getProperties ().put (PROP_UDP_BIND_NOTIFY, "true");
	
	super.init (server);
	_radiusReadMeters = new RadiusIOHMeters (null, null);
	_radiusReadMeters.init (_services.getMeteringService (), getIOHMeters (), "read", true);
	_radiusWriteMeters = new RadiusIOHMeters (null, null);
	_radiusWriteMeters.init (_services.getMeteringService (), getIOHMeters (), "write", true);
	_parserErrorMeter = getIOHMeters ().createIncrementalMeter ("parser.error", null);
	
	_router.init (this);
	
	return this;
    }
    
    @Override
    public UdpChannelListener serverOpened (UdpServer server){
	UdpChannelListener listener = newUdpChannel (this, server.getProperties ());
	_router.channelOpened ((RadiusIOHChannel) listener);
	listener.connectionOpened (server.getServerChannel ());
	return listener;
    }
    
    @Override
    public void initMuxClient (MuxClient agent){
	super.initMuxClient (agent);
	_router.initMuxClient (agent);
    }

    @Override
    protected TcpChannelListener newMuxClient (IOHEngine engine, TcpChannel channel, Map<String, Object> props, boolean isRemoteIOH){
	return new RemoteRadiusMuxClient (engine, channel, props, isRemoteIOH);
    }

    @Override
    protected IOHLocalMuxFactory.IOHLocalMuxConnection newLocalMuxClient (MuxHandler muxHandler, ConnectionListener listener, Map opts){
	return new LocalRadiusMuxClient (this, muxHandler, listener, opts);
    }
    
    @Override
    protected UdpChannelListener newUdpChannel (MuxClient agent, long bindId, InetSocketAddress local, Map<ReactorProvider.UdpOption, Object> opts){
	return new RadiusIOHUdpClientChannel (agent, bindId, local, opts);
    }
    @Override
    protected UdpChannelListener newUdpChannel (IOHEngine engine, Map<String, Object> props){
	return new RadiusIOHUdpServerChannel (engine, props);
    }
    
    public static interface RadiusMuxClient {
	public RadiusIOHMeters getReadRadiusIOHMeters ();
	public RadiusIOHMeters getWriteRadiusIOHMeters ();
    }

    public static class RemoteRadiusMuxClient extends MuxClient implements RadiusMuxClient {
	RadiusIOHMeters _radiusReadMeters, _radiusWriteMeters;
	private RemoteRadiusMuxClient (IOHEngine engine, TcpChannel channel, Map<String, Object> props, boolean isRemoteIOH){
	    super (engine, channel, props, isRemoteIOH);
	}
	public RadiusIOHMeters getReadRadiusIOHMeters (){
	    return _radiusReadMeters;
	}
	public RadiusIOHMeters getWriteRadiusIOHMeters (){
	    return _radiusWriteMeters;
	}
	protected void createDefaultMeters (){
	    _radiusReadMeters = new RadiusIOHMeters (null, null);
	    _radiusReadMeters.init (getIOHEngine ().getIOHServices ().getMeteringService (), getIOHMeters (), "read", false);
	    _radiusWriteMeters = new RadiusIOHMeters (null, null);
	    _radiusWriteMeters.init (getIOHEngine ().getIOHServices ().getMeteringService (), getIOHMeters (), "write", false);
	    super.createDefaultMeters ();
	}
    }
    public static class LocalRadiusMuxClient extends IOHLocalMuxFactory.IOHLocalMuxConnection implements RadiusMuxClient {
	RadiusIOHMeters _radiusReadMeters, _radiusWriteMeters;
	private LocalRadiusMuxClient (IOHEngine engine, MuxHandler handler, ConnectionListener listener, Map opts){
	    super (engine, handler, listener, opts);
	}
	public RadiusIOHMeters getReadRadiusIOHMeters (){
	    return _radiusReadMeters;
	}
	public RadiusIOHMeters getWriteRadiusIOHMeters (){
	    return _radiusWriteMeters;
	}
	protected void createDefaultMeters (){
	    _radiusReadMeters = new RadiusIOHMeters (null, null);
	    _radiusReadMeters.init (getIOHEngine ().getIOHServices ().getMeteringService (), getIOHMeters (), "read", false);
	    _radiusWriteMeters = new RadiusIOHMeters (null, null);
	    _radiusWriteMeters.init (getIOHEngine ().getIOHServices ().getMeteringService (), getIOHMeters (), "write", false);
	    super.createDefaultMeters ();
	}
    }
    
    
    public static class RadiusIOHUdpServerChannel extends IOHUdpChannel implements RadiusIOHChannel {
	protected RadiusIOHMeters _readRadiusMeter, _writeRadiusMeter;
	protected RadiusIOHRouter _router;
	private RadiusIOHUdpServerChannel (IOHEngine engine, Map<String, Object> props){
	    super (engine, props);
	    _isText = false;
	    _readRadiusMeter = ((RadiusIOHEngine)engine)._radiusReadMeters;
	    _writeRadiusMeter = ((RadiusIOHEngine)engine)._radiusWriteMeters;
	    _router = ((RadiusIOHEngine)engine)._router;
	}
	@Override
	public void messageReceived(UdpChannel cnx,
				    ByteBuffer buff,
				    InetSocketAddress addr){
	    if (disabled (buff))
		return;
	    _readMeter.inc (buff.remaining ());
	    RadiusMessage msg = null;
	    try{
		msg = new RadiusMessage (addr, buff);
	    } catch (Throwable t){
		((RadiusIOHEngine)_engine)._parserErrorMeter.inc (1);
		if (_logger.isDebugEnabled ()) _logger.debug (this+" : parsing exception : "+t);
		buff.position (buff.limit ());
		return;
	    }
	    if (_logger.isDebugEnabled ()) _logger.debug (this+" : RECEIVED :\n["+msg+"]");
	    _readRadiusMeter.getMeter (msg).inc (1);
	    _router.doClientRequest (this, msg);
	}
	@Override
	public boolean sendOut (MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	    int code = buffs[0].get (buffs[0].position ()) & 0xFF; // it is lost after sendOut
	    if (super.sendOut (agent, to, checkBuffer, copy, buffs)){
		RadiusMuxClient client = (RadiusMuxClient) agent;
		client.getWriteRadiusIOHMeters ().getMeter (code).inc (1);
		_writeRadiusMeter.getMeter (code).inc (1);
		return true;
	    }
	    return false;
	}
	public UdpChannel getUdpChannel (){ return (UdpChannel) getChannel ();}
	public IOHUdpChannel getIOHChannel (){ return this;}
	public boolean sendAgent (IOHEngine.MuxClient agent, RadiusMessage msg){
	    return super.sendAgent (agent, msg.getFromAddress (), false, msg.getIdentifier (), false, msg.toByteBuffer ());
	}
	public IOHEngine.MuxClient pickAgent (Object preferenceHint){
	    return _agentsList.pick (preferenceHint);
	}
	public SimpleMonitorable getMonitorable (){
	    return null;
	}
    }
    public static class RadiusIOHUdpClientChannel extends IOHUdpChannel {
	protected RadiusIOHMeters _readRadiusMeterEngine, _writeRadiusMeterEngine;
	protected RadiusIOHMeters _readRadiusMeterAgent, _writeRadiusMeterAgent;
	private RadiusIOHUdpClientChannel (MuxClient agent, long bindId, InetSocketAddress local, Map<ReactorProvider.UdpOption, Object> opts){
	    super (agent, bindId, local, opts);
	    _isText = false;
	    _readRadiusMeterEngine = ((RadiusIOHEngine)agent.getIOHEngine ())._radiusReadMeters;
	    _writeRadiusMeterEngine = ((RadiusIOHEngine)agent.getIOHEngine ())._radiusWriteMeters;
	    _readRadiusMeterAgent = ((RadiusMuxClient)agent).getReadRadiusIOHMeters ();
	    _writeRadiusMeterAgent = ((RadiusMuxClient)agent).getWriteRadiusIOHMeters ();
	}
	@Override
	public void messageReceived(UdpChannel cnx,
				    ByteBuffer buff,
				    InetSocketAddress addr){
	    if (disabled (buff))
		return;
	
	    _readMeter.inc (buff.remaining ());
	    if (_logger.isDebugEnabled ())
		logReceived ("RECEIVED FROM "+addr, buff);

	    if (buff.remaining () < 20){
		buff.position (buff.limit ());
		return;
	    }
	    int code = buff.get (buff.position ()) & 0xFF;
	    
	    ByteBuffer copied = ByteBuffer.allocate (buff.remaining ());
	    copied.put (buff);
	    copied.flip ();
	    // for the sessionId : we take the msg identifier
	    sendAgent (_agent, addr, true, copied.get (copied.position ()+1) & 0xFF, false, copied);
	    _readRadiusMeterEngine.getMeter (code).inc (1);
	    _readRadiusMeterAgent.getMeter (code).inc (1);
	}
	@Override
	public boolean sendOut (MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	    int code = buffs[0].get (buffs[0].position ()) & 0xFF; // it is lost after sendOut
	    if (super.sendOut (agent, to, checkBuffer, copy, buffs)){
		_writeRadiusMeterEngine.getMeter (code).inc (1);
		_writeRadiusMeterAgent.getMeter (code).inc (1);
		return true;
	    }
	    return false;
	}
    }
}
