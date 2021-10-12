// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.radius.ioh;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.*;
import alcatel.tess.hometop.gateways.reactor.*;

import java.util.*;
import java.nio.*;
import java.net.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;
import java.nio.charset.Charset;

import com.alcatel.as.radius.parser.*;
import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.tools.ChannelWriter;
import com.alcatel.as.ioh.tools.ByteBufferUtils;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;
import static com.alcatel.as.ioh.tools.ByteBufferUtils.getUTF8;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;

public class RadiusIOHRouter {

    protected static Charset UTF8 = null;
    static {
	try{ UTF8 = Charset.forName ("utf-8");
	}catch(Exception e){}// cannot happen
    }
    protected static final AtomicInteger SEED_HOP_ID = new AtomicInteger (0);
    
    protected Logger _logger;

    protected RadiusIOHRouterFactory _factory;
    protected RadiusIOHMeters _droppedMeters, _routedMeters;
    protected boolean _test = false;
    protected boolean _localOnly = false;
    protected String _toString;
    protected ChannelWriter.SendBufferMonitor CHANNEL_WRITER_REMOTE_AGENT, CHANNEL_WRITER_LOCAL_AGENT;
    
    public RadiusIOHRouter (RadiusIOHRouterFactory factory, Logger logger){
	_factory = factory;
	_logger = logger;
	_toString = new StringBuilder ().append ("RadiusIOHRouter[").append ('-').append (']').toString ();
    }

    @Override
    public String toString (){
	return _toString;
    }
    
    /************************* The public methods called by the RadiusIOH ********************************/
    
    public void init (IOHEngine engine){
	_toString = new StringBuilder ().append ("RadiusIOHRouter[").append (engine.name ()).append (']').toString ();
	
	Map<String, Object> props = engine.getProperties ();
	_test = getBooleanProperty ("radius.ioh.router.test", props, false);
	
	_localOnly = engine.useMuxAgent () == false;

	_logger.info (this+" : _localOnly="+_localOnly+", _test="+_test);

	_droppedMeters = new RadiusIOHMeters (null, null);
	_droppedMeters.init (engine.getIOHServices ().getMeteringService (), engine.getIOHMeters (), "dropped", false);
	_routedMeters = new RadiusIOHMeters (null, null);
	_routedMeters.init (engine.getIOHServices ().getMeteringService (), engine.getIOHMeters (), "routed", false);

	CHANNEL_WRITER_REMOTE_AGENT = new ChannelWriter.ProgressiveSendBufferMonitorWithPriorities(
		getIntProperty("radius.ioh.router.mux.writer.lowWM", props,
			1000000), getIntProperty(
			"radius.ioh.router.mux.writer.highWM", props, 2000000), 2);
	CHANNEL_WRITER_LOCAL_AGENT = new ChannelWriter.ProgressiveSendBufferMonitorWithPriorities(
		getIntProperty("radius.ioh.router.local.writer.lowWM", props,
			10000), getIntProperty(
			"radius.ioh.router.local.writer.highWM", props, 20000), 2);
    }
    
    public void initMuxClient (MuxClient agent){
	agent.setContext (newAgentContext ().init (agent));
	if (agent.isLocalAgent ())
	    agent.setSendBufferMonitor(CHANNEL_WRITER_LOCAL_AGENT);
	else
	    agent.setSendBufferMonitor (CHANNEL_WRITER_REMOTE_AGENT);
    }
    protected AgentContext newAgentContext (){ // maybe overridden
	return new AgentContext ();
    }
    protected class AgentContext {
	protected RadiusIOHMeters _droppedMeters, _routedMeters;
	public Meter _dismissMeter;
	public AgentContext init (MuxClient agent){
	    _droppedMeters = new RadiusIOHMeters (null, null);
	    _droppedMeters.init (agent.getIOHEngine ().getIOHServices ().getMeteringService (), agent.getIOHMeters (), "dropped", false);
	    _routedMeters = new RadiusIOHMeters (null, null);
	    _routedMeters.init (agent.getIOHEngine ().getIOHServices ().getMeteringService (), agent.getIOHMeters (), "routed", false);
	    _dismissMeter = agent.getIOHMeters ().createIncrementalMeter ("router.error.dismiss", null);
	    return this;
	}
	public RadiusIOHMeters getRoutedMeters (){ return _routedMeters;}
	public RadiusIOHMeters getDroppedMeters (){ return _droppedMeters;}
    }
    protected AgentContext getAgentContext (MuxClient agent){
	return (AgentContext) agent.getContext ();
    }
    
    /*************************************/

    public void channelOpened (RadiusIOHChannel channel){
    }
    
    public void doClientRequest (RadiusIOHChannel client, RadiusMessage msg){
    }
    
    public boolean checkAgentOverload (MuxClient agent, RadiusMessage msg){
	int buffSize = agent.getSendBufferSize ();
	if (buffSize == 0 ||
	    ChannelWriter.check (buffSize, agent.getSendBufferMonitor (), getPriority (msg)))
	    return true;
	if (_logger.isInfoEnabled ())
	    _logger.info (agent+" : Buffer Full : cannot send message (sendBufferSize="+buffSize+")");
	return false;
    }

    protected int getPriority (RadiusMessage msg){
	return msg.isRequest () ? 0 : 1;
    }

    
    /*************************************/
    
    protected int getIntProperty (String name, Map props, int def){
	try{
	    String v = (String) props.get (name);
	    if (v == null) v = (String) _factory.getConf ().get (name);
	    if (v != null) return Integer.parseInt (v);
	}catch(Throwable t){
	    _logger.error (this+" : Invalid property value for : "+name+" : will apply default : "+def);
	}
	return def;
    }
    protected boolean getBooleanProperty (String name, Map props, boolean def){
	try{
	    String v = (String) props.get (name);
	    if (v == null) v = (String) _factory.getConf ().get (name);
	    if (v != null) return Boolean.parseBoolean (v);
	}catch(Throwable t){
	    _logger.error (this+" : Invalid property value for : "+name+" : will apply default : "+def);
	}
	return def;
    }
    protected String getStringProperty (String name, Map props, String def){
	String value = (String) props.get (name);
	if (value == null) value = (String) _factory.getConf ().get (name);
	if (value != null && (value = value.trim ()).length () > 0) return value;
	return def;
    }
}
