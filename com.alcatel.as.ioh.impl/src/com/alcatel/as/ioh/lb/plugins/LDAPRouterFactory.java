// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb.plugins;

import com.alcatel.as.ioh.lb.*;
import com.alcatel.as.ioh.lb.mux.*;
import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;

import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.*;

import com.alcatel.as.ioh.client.TcpClient;
import com.alcatel.as.ioh.client.TcpClient.Destination;
import com.alcatel.as.ioh.client.UdpClient;
import org.apache.log4j.Logger;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL, property={"router.id=ldap"})
public class LDAPRouterFactory implements RouterFactory, IOHRouterFactory {

    public static final String PROP_REMOTE_IP = "router.add.remote.ip";
    public static final String PROP_REMOTE_IP_VERSION = "router.add.remote.ip.version";
    public static final String PROP_REMOTE_PORT = "router.add.remote.port";

    @Activate
    public void start (){
    }

    public Object newRouterConfig (Map<String, Object> props){
	return new LDAPConfig (props);
    }

    public Router newRouter (Object config){
	return new LDAPRouter (((LDAPConfig) config));
    }
    
    public Object newIOHRouterConfig (Map<String, Object> props){
	return new LDAPConfig (props);
    }

    public IOHRouter newIOHRouter (Object config){
	return new LDAPIOHRouter (((LDAPConfig) config));
    }

    public String toString (){ return "LDAPRouterFactory[id=round-robin]";}

    protected static class LDAPConfig {
	protected int _maxSendBuffer;
	protected boolean _remoteIP;
	protected boolean _remotePort;
	protected boolean _remoteIPVersion;
	protected int _remoteExtra;
	
	protected LDAPConfig (Map<String, Object> props){
	    String s = (String) props.get (LoadBalancer.PROP_DEST_WRITE_BUFFER_MAX);
	    _maxSendBuffer = s != null ? Integer.parseInt (s) : Integer.MAX_VALUE;
	    s = (String) props.get (PROP_REMOTE_IP);
	    if (s != null) _remoteIP = Boolean.parseBoolean (s);
	    if (_remoteIP){
		s = (String) props.get (PROP_REMOTE_IP_VERSION);
		if (s != null) _remoteIPVersion = Boolean.parseBoolean (s);
		s = (String) props.get (PROP_REMOTE_PORT);
		if (s != null) _remotePort = Boolean.parseBoolean (s);
		if (_remoteIPVersion) _remoteExtra++;
		if (_remotePort) _remoteExtra+=2;
	    }
	}
    }

    protected static class LDAPRouter extends RoundRobinRouterFactory.RoundRobinRouter {

	private ByteBuffer _bind = null;
	private boolean _bindPending = false;
	private int _bindId;
	private LDAPConfig _config;
	private ByteBuffer _remoteAddr;

	protected LDAPRouter (LDAPConfig config){
	    super ();
	    _config = config;
	    _maxSendBuffer = config._maxSendBuffer;
	}

	public int neededBuffer (){ return -1;}
	public boolean needServerData () { return true; }
	@Override
	public void init (Client client){
	    InetSocketAddress remoteAddr = ((TcpChannel)client.getChannel ()).getRemoteAddress ();
	    byte[] remoteIP = remoteAddr.getAddress ().getAddress ();
	    _remoteAddr = ByteBuffer.allocate (remoteIP.length + _config._remoteExtra);
	    if (_config._remoteIPVersion){
		if (remoteAddr.getAddress () instanceof Inet4Address)
		    _remoteAddr.put ((byte)4);
		else
		    _remoteAddr.put ((byte)6);
	    }
	    _remoteAddr.put (remoteIP);
	    if (_config._remotePort){
		int remotePort = remoteAddr.getPort ();
		_remoteAddr.put ((byte) (remotePort >> 8));
		_remoteAddr.put ((byte) remotePort);
	    }
	    _remoteAddr.flip ();
	}
	@Override
	public void init (Client client, TcpClient.Destination dest){
	    Logger logger = client.getLogger ();
	    if (_config._remoteIP){
		if (logger.isDebugEnabled ())
		    logger.debug (client+" : send Remote Client Address to "+dest);
		int orig = _remoteAddr.position ();
		client.sendToDestination (dest, true, _remoteAddr);
		_remoteAddr.position (orig);
	    }
	    
	    if (_bind == null) return;
	    if (logger.isDebugEnabled ())
		logger.debug (client+" : send Bind to "+dest+" id="+_bindId);
	    int orig = _bind.position ();
	    client.sendToDestination (dest, true, _bind);
	    _bind.position (orig);
	}

	@Override
	public void route (Client client, Chunk chunk){
	    LDAPParserFactory.ChunkInfo info = chunk.attachment ();
	    Logger logger = client.getLogger ();
	    if (info._type == 0x60){
		// BIND
		_bindId = chunk.getId ();
		_bindPending = true;
		ByteBuffer data = chunk.getData ();
		_bind = ByteBuffer.allocate (data.remaining ());
		_bind.put (data);
		_bind.flip ();
		int orig = _bind.position ();
		for (Destination dest : client.getOpenDestinations ()){
		    if (logger.isDebugEnabled ())
			logger.debug (client+" : send Bind to "+dest+" id="+_bindId);
		    client.sendToDestination (dest, true, _bind);
		    _bind.position (orig);
		}
		client.sendToDestination (null, chunk); // force a compact()
	    } else if (info._type == 0x42){
		// UNBIND
		_bind = null;
		_bindPending = false;
		ByteBuffer data = chunk.getData ();
		int orig = data.position ();
		for (Destination dest : client.getOpenDestinations ()){
		    if (logger.isDebugEnabled ())
			logger.debug (client+" : send UnBind to "+dest);
		    client.sendToDestination (dest, true, data);
		    data.position (orig);
		}
		data.position (data.limit ());
		client.sendToDestination (null, chunk); // force a compact()
	    } else {
		super.route (client, chunk);
	    }
	}

	// called in server _exec
	public void route (final Client client, final TcpClient.Destination server, Chunk chunk){
	    final LDAPParserFactory.ChunkInfo info = chunk.attachment ();
	    final Logger logger = client.getLogger ();
	    if (info._type == 0x61){
		// BIND RESP
		ByteBuffer data = chunk.getData ();
		final ByteBuffer clone = ByteBuffer.allocate (data.remaining ());
		clone.put (data);
		clone.flip ();
		final int id = chunk.getId ();
		Runnable r = new Runnable (){
			public void run (){
			    if (!_bindPending){
				if (logger.isDebugEnabled ())
				    logger.debug (client+" : Dropping duplicate Bind Resp from "+server);
				return;
			    }
			    if (id != _bindId){
				if (logger.isDebugEnabled ())
				    logger.debug (client+" : Dropping Bind Resp with invalid id : "+id+" expected :"+_bindId);
				return;
			    }
			    if (logger.isDebugEnabled ())
				logger.debug (client+" : Returning Bind Resp from "+server);
			    client.sendToClient (false, clone);
			    _bindPending = false;
			}
		    };
		client.getExecutor ().execute (r);
		client.sendToClient (null, chunk); // force a compact()
		return;
	    }
	    client.sendToClient (server, chunk);
	}
	
    }

    protected static class LDAPIOHRouter extends RoundRobinRouterFactory.RoundRobinIOHRouter {

	private ByteBuffer _bind = null;
	private boolean _bindPending = false;
	private int _bindId;

	protected LDAPIOHRouter (LDAPConfig config){
	    super ();
	}

	public int neededBuffer (){ return -1;}
	public boolean needServerData () { return true; }

	@Override
	public void init (IOHClient client, MuxClient agent){
	    if (_bind == null) return;
	    Logger logger = client.getLogger ();
	    if (logger.isDebugEnabled ())
		logger.debug (client+" : send Bind to "+agent+" id="+_bindId);
	    int orig = _bind.position ();
	    client.sendToDestination (agent, true, _bind);
	    _bind.position (orig);
	}

	@Override
	public void route (IOHClient client, Chunk chunk){
	    LDAPParserFactory.ChunkInfo info = chunk.attachment ();
	    Logger logger = client.getLogger ();
	    if (info._type == 0x60){
		// BIND
		_bindId = chunk.getId ();
		_bindPending = true;
		ByteBuffer data = chunk.getData ();
		_bind = ByteBuffer.allocate (data.remaining ());
		_bind.put (data);
		_bind.flip ();
		int orig = _bind.position ();
		MuxClientList.Iterator<Object> it = new MuxClientList.Iterator<Object> (){
			public Object next (MuxClient agent, Object ctx){
			    if (logger.isDebugEnabled ())
				logger.debug (client+" : send Bind to "+agent+" id="+_bindId);
			    client.sendToDestination (agent, true, _bind);
			    _bind.position (orig);
			    return null;
			}
		    };
		client.getIOHChannel ().getAgents ().iterate (it, null);
		client.sendToDestination (null, chunk, false); // force a compact()
	    } else if (info._type == 0x42){
		// UNBIND
		_bind = null;
		_bindPending = false;
		ByteBuffer data = chunk.getData ();
		int orig = data.position ();
		MuxClientList.Iterator<Object> it = new MuxClientList.Iterator<Object> (){
			public Object next (MuxClient agent, Object ctx){
			    if (logger.isDebugEnabled ())
				logger.debug (client+" : send UnBind to "+agent);
			    client.sendToDestination (agent, true, data);
			    data.position (orig);
			    return null;
			}
		    };
		client.getIOHChannel ().getAgents ().iterate (it, null);
		data.position (data.limit ());
		client.sendToDestination (null, chunk, false); // force a compact()
	    } else {
		super.route (client, chunk);
	    }
	}

	// called in server _exec
	public void route (IOHClient client, MuxClient agent, Chunk chunk){
	    final LDAPParserFactory.ChunkInfo info = chunk.attachment ();
	    final Logger logger = client.getLogger ();
	    if (info._type == 0x61){
		// BIND RESP
		ByteBuffer data = chunk.getData ();
		final ByteBuffer clone = ByteBuffer.allocate (data.remaining ());
		clone.put (data);
		clone.flip ();
		final int id = chunk.getId ();
		Runnable r = new Runnable (){
			public void run (){
			    if (!_bindPending){
				if (logger.isDebugEnabled ())
				    logger.debug (client+" : Dropping duplicate Bind Resp from "+agent);
				return;
			    }
			    if (id != _bindId){
				if (logger.isDebugEnabled ())
				    logger.debug (client+" : Dropping Bind Resp with invalid id : "+id+" expected :"+_bindId);
				return;
			    }
			    if (logger.isDebugEnabled ())
				logger.debug (client+" : Returning Bind Resp from "+agent);
			    client.sendToClient (false, clone);
			    _bindPending = false;
			}
		    };
		client.getExecutor ().execute (r);
		client.sendToClient (null, chunk); // force a compact()
		return;
	    }
	    client.sendToClient (agent, chunk);
	}
	
    }
}
