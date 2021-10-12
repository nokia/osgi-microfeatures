// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.engine.generic;

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

public class GenericIOHEngine extends IOHEngine {
    
    public GenericIOHEngine (String name, IOHServices services){
	super (name, services);
    }

    public IOHEngine init (TcpServer server){
	if (server.getProperties ().get (PROP_TCP_CONNECT_SHARED) == null &&
	    server.getProperties ().get (PROP_TCP_CONNECT_UNIQUE) == null)
	    server.getProperties ().put (PROP_TCP_CONNECT_SHARED, "false"); // MAYBE overridden in xml
	if (server.getProperties ().get (PROP_TCP_LISTEN_NOTIFY) == null) // MAYBE overridden in xml
	    server.getProperties ().put (PROP_TCP_LISTEN_NOTIFY, "true");
	server.getProperties ().put (PROP_UDP, "false");
	super.init (server);	
	return this;
    }
}
