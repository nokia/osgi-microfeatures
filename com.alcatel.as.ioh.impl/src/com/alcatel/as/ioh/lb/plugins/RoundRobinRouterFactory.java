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

import java.util.*;
import java.util.concurrent.atomic.*;

import com.alcatel.as.ioh.client.TcpClient.Destination;
import com.alcatel.as.ioh.client.UdpClient;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL, property={"router.id=round-robin"})
public class RoundRobinRouterFactory implements RouterFactory, IOHRouterFactory {

    @Activate
    public void start (){
    }

    public Object newRouterConfig (Map<String, Object> props){
	return new RoundRobinConfig (props).init ();
    }

    public Router newRouter (Object config){
	return ((RoundRobinConfig) config)._router;
    }

    public Object newIOHRouterConfig (Map<String, Object> props){
	return new RoundRobinConfig (props).init ();
    }

    public IOHRouter newIOHRouter (Object config){
	return ((RoundRobinConfig) config)._muxrouter;
    }

    public String toString (){ return "RoundRobinRouterFactory[id=round-robin]";}

    protected static class RoundRobinConfig {
	protected int _maxSendBuffer;
	protected RoundRobinRouter _router;
	protected RoundRobinIOHRouter _muxrouter;

	protected RoundRobinConfig (Map<String, Object> props){
	    String s = (String) props.get (LoadBalancer.PROP_DEST_WRITE_BUFFER_MAX);
	    _maxSendBuffer = s != null ? Integer.parseInt (s) : Integer.MAX_VALUE;
	}
	protected RoundRobinConfig init (){
	    _router = new RoundRobinRouter (this);
	    _muxrouter = new RoundRobinIOHRouter (this);
	    return this;
	}
    }

    protected static class RoundRobinRouter implements Router {

	protected AtomicInteger _count = new AtomicInteger (0);
	protected int _maxSendBuffer;

	protected RoundRobinRouter (RoundRobinConfig config){
	    _maxSendBuffer = config._maxSendBuffer;
	}
	protected RoundRobinRouter (){} // for superclasses

	public int neededBuffer (){ return 0;}
	
	public void route (Client client, Chunk chunk){
	    List<Destination> destinations = client.getOpenDestinations ();
	    int size = destinations.size ();
	    if (size == 0){
		client.sendToDestination (null, chunk);
		return;
	    }
	    for (int i=0; i<size; i++){
		Destination dest = destinations.get ((_count.getAndIncrement () & 0x7FFFFFFF) % size);
		int buffSize = dest.getChannel ().getSendBufferSize ();
		if (buffSize > _maxSendBuffer){
		    if (client.getLogger ().isInfoEnabled ())
			client.getLogger ().info (client+" : destination : "+dest+" : overloaded");
		    client.getMeters ().getOverloadDestMeter ().inc (1);
		    continue;
		}
		client.sendToDestination (dest, chunk);
		return;
	    }
	    if (client.getLogger ().isInfoEnabled ())
		client.getLogger ().info (client+" : all destinations overloaded : dropping message");
	    client.sendToDestination (null, chunk);
	}

	public void route (UdpClientContext client, Chunk chunk){
	    int buffSize = client.getEndpoint ().getChannel ().getSendBufferSize ();
	    if (buffSize > _maxSendBuffer){
		if (client.getLogger ().isInfoEnabled ())
		    client.getLogger ().info (client+" : destinations overloaded");
		client.getMeters ().getOverloadDestMeter ().inc (1);
		client.getMeters ().getFailedUdpChannelsMeter ().inc (chunk.size ());
		client.sendToDestination (null, chunk);
		return;
	    }
	    List<UdpClient.Destination> destinations = client.getDestinations ();
	    int size = destinations.size ();
	    if (size == 0){
		client.sendToDestination (null, chunk);
		return;
	    }
	    UdpClient.Destination dest = destinations.get ((_count.getAndIncrement () & 0x7FFFFFFF) % size);
	    client.sendToDestination (dest, chunk);
	}
    }
    protected static class RoundRobinIOHRouter implements IOHRouter {

	protected AtomicInteger _count = new AtomicInteger (0);

	protected RoundRobinIOHRouter (RoundRobinConfig config){
	}
	protected RoundRobinIOHRouter (){} // for superclasses

	public int neededBuffer (){ return 0;}
	
	public void route (IOHClient client, Chunk chunk){
	    IOHChannel channel = client.getIOHChannel ();
	    MuxClientList agents = channel.getAgents ();
	    int size = agents.sizeOfActive ();
	    if (size == 0){
		client.sendToDestination (null, chunk, false);
		return;
	    }
	    MuxClient local = agents.pickLocalAgent ();
	    if (local != null) size--;
	    for (int i=0; i<size; i++){
		MuxClient agent = agents.pick (_count.getAndIncrement () & 0x7FFFFFFF);
		if (!channel.checkSendBufferAgent (agent, null)){
		    if (client.getLogger ().isInfoEnabled ())
			client.getLogger ().info (client+" : destination : "+agent+" : overloaded");
		    continue;
		}
		client.sendToDestination (agent, chunk, false);
		return;
	    }
	    if (local != null){
		if (channel.checkSendBufferAgent (local, null)){
		    client.sendToDestination (local, chunk, false);
		    return;
		}
		if (client.getLogger ().isInfoEnabled ())
		    client.getLogger ().info (client+" : destination : "+local+" : overloaded");
	    }
	    if (client.getLogger ().isInfoEnabled ())
		client.getLogger ().info (client+" : all destinations overloaded : dropping message");
	    client.sendToDestination (null, chunk, false);
	}
    }
    
}
