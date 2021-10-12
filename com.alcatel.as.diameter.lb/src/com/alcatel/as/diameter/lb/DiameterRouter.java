// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.apache.log4j.Logger;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.client.TcpClient.Destination;

import alcatel.tess.hometop.gateways.reactor.*;
import org.osgi.service.component.annotations.*;
import com.alcatel.as.service.concurrent.*;

public class DiameterRouter {
    
    public static final Logger LOGGER = Logger.getLogger("as.diameter.lb.router");
    
    public void clientOpened (DiameterClient client){}
    public void clientClosed (DiameterClient client){}

    public void serverOpened (DiameterClient client, Destination server){}
    public void serverClosed (DiameterClient client, Destination server){}
    
    public void doClientRequest (DiameterClient client, DiameterMessage msg){}
    public void doServerRequest (DiameterClient client, Destination server, DiameterMessage msg){}
    public void doClientResponse (DiameterClient client, DiameterMessage msg){}
    public void doServerResponse (DiameterClient client, Destination server, DiameterMessage msg){}
    public boolean checkClientOverload (DiameterClient client, DiameterMessage msg){
	return true;
    }
    public boolean checkServerOverload (DiameterClient client, Destination server, DiameterMessage msg){
	return true;
    }
    
    public void clientBlocked (DiameterClient client){} // not called unless activated    
    public void clientUnblocked (DiameterClient client){} // not called unless activated
    public void serverBlocked (DiameterClient client, Destination server){} // not called unless activated
    public void serverUnblocked (DiameterClient client, Destination server){} // not called unless activated
}
