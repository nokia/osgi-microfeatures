// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb.plugins;

import com.alcatel.as.ioh.lb.*;

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

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL, property={"router.id=unicast-round-robin"})
public class UnicastRoundRobinRouterFactory implements UnicastRouterFactory {

    @Activate
    public void start (){
    }

    public Object newUnicastRouterConfig (Map<String, Object> props){
	return new RoundRobinConfig (props).init ();
    }

    public UnicastRouter newUnicastRouter (Object config){
	return ((RoundRobinConfig) config)._router;
    }

    public String toString (){ return "UnicastRoundRobinRouterFactory[id=unicast-round-robin]";}

    protected static class RoundRobinConfig {
	protected RoundRobinRouter _router;

	protected RoundRobinConfig (Map<String, Object> props){
	}
	protected RoundRobinConfig init (){
	    _router = new RoundRobinRouter (this);
	    return this;
	}
    }

    protected static class RoundRobinRouter implements UnicastRouter {

	protected AtomicInteger _count = new AtomicInteger (0);

	protected RoundRobinRouter (RoundRobinConfig config){
	}

	public int neededBuffer (){ return 0;}

	public void init (Client client){}
	
	public Destination route (Client client, Chunk initChunk){
	    List<Destination> destinations = client.getTcpClient ().getDestinations ();
	    int size = destinations.size ();
	    if (size == 0) return null;
	    Destination dest = destinations.get ((_count.getAndIncrement () & 0x7FFFFFFF) % size);
	    return dest;
	}

    }
    
}
