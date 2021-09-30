package com.nokia.as.k8s.sless.fwk.controller.ioh;

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
import com.alcatel.as.service.metering2.*;
import com.nextenso.mux.*;
import com.nextenso.mux.util.MuxIdentification;

import com.nokia.as.k8s.sless.fwk.controller.*;

public class ControllerIOHEngine extends IOHEngine {

    private Controller _controller;

    public ControllerIOHEngine (String name, IOHServices services, Controller controller){
	super (name, services);
	_controller = controller;
    }
    
    public IOHEngine init (TcpServer server){
	server.getProperties ().put (PROP_TCP, "false");
	server.getProperties ().put (PROP_UDP, "false");
	server.getProperties ().put (PROP_SCTP, "false");
	server.getProperties ().put (PROP_EXT_SERVER_MIN, "0");
	super.init (server);
	return this;
    }
    @Override
    public void initMuxClient (MuxClient agent){
	super.initMuxClient (agent);
	Meter pushMeter = agent.getIOHMeters ().createIncrementalMeter ("sless.push", null);
	Meter unpushMeter = agent.getIOHMeters ().createIncrementalMeter ("sless.unpush", null);
	String instance = agent.getMuxIdentification ().getInstanceName ();
	int i = instance.indexOf ("__");
	String group = i >  0 ?
	    instance.substring (0, i) : instance;
	ControlledAgent rt = new ControlledAgent (agent.getExtendedMuxHandler (),
						  group,
						  agent.getApplicationParam ("agent.protocol", ""),
						  pushMeter,
						  unpushMeter);
	agent.setContext (rt);
	_controller.addControlled (rt);
    }
    @Override
    public void resetMuxClient (MuxClient agent){
	super.resetMuxClient (agent);
	if (_logger.isInfoEnabled ()) _logger.info (this+" : "+agent+" : closed");
	ControlledAgent rt = agent.getContext ();
	if (rt != null) _controller.removeControlled (rt);
    }
    
    @Override
    public boolean sendMuxData(final MuxClient agent, MuxHeader header, boolean copy, ByteBuffer ... bufs) {
	ControlledAgent rt = agent.getContext ();
	rt.muxData (header, com.alcatel.as.ioh.tools.ByteBufferUtils.aggregate (false, false, bufs));
	return true;
    }
}
