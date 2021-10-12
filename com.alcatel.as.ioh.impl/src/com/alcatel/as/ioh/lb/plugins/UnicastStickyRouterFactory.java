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

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL, property={"router.id=unicast-sticky"})
public class UnicastStickyRouterFactory implements UnicastRouterFactory {

    @Activate
    public void start (){
    }

    public Object newUnicastRouterConfig (Map<String, Object> props){
	return new StickyConfig (props).init ();
    }

    public UnicastRouter newUnicastRouter (Object config){
	return ((StickyConfig) config)._router;
    }

    public String toString (){ return "StickyRouterFactory[id=unicast-sticky]";}

    protected static class StickyConfig {
	protected StickyRouter _router;

	protected StickyConfig (Map<String, Object> props){
	}
	protected StickyConfig init (){
	    _router = new StickyRouter (this);
	    return this;
	}
    }

    protected static class StickyRouter implements UnicastRouter {
	
	protected StickyRouter (StickyConfig config){
	}

	public int neededBuffer (){ return -1;}

	public void init (Client client){}

	public Destination route (Client client, Chunk initChunk){
	    List<Destination> destinations = client.getTcpClient ().getDestinations ();
	    int size = destinations.size ();
	    if (size == 0) return null;
	    int id = initChunk.getId () & 0x7FFFFFFF;
	    return destinations.get (id % size);
	}
    }
    
}
