// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb.mux.agent;

import java.nio.ByteBuffer;
import java.net.InetSocketAddress;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.alcatel.as.ioh.server.*;

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class EchoTcpAgent implements TcpAgent {

    public int id (){ return 300;}
    public String protocol (){ return "Any";}

    public String toString (){ return "EchoTcpAgent";}

    public void clientData (TcpClient client, ByteBuffer data){
	client.send (false, data);
    }
}
