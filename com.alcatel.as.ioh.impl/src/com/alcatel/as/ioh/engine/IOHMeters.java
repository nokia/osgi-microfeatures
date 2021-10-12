// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.engine;

import java.util.*;
import java.util.concurrent.atomic.*;

import com.nextenso.mux.*;
import com.nextenso.mux.impl.ioh.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;

public class IOHMeters extends SimpleMonitorable {

    protected MeteringService _metering;
    protected IOHEngine.MuxClient _agent;
    protected Map<Integer, Meter> _remoteMeters = new HashMap<> ();
    protected AtomicInteger REMOTE_METER_SEED = new AtomicInteger (0);
    
    protected Meter _sendTcpMeter, _sendDroppedTcpMeter, _sendUdpMeter, _sendDroppedUdpMeter, _readTcpMeter, _readUdpMeter, _channelsTcpOpenMeter, _channelsTcpAcceptedOpenMeter, _channelsTcpConnectedOpenMeter, _channelsTcpClosedMeter, _channelsTcpAcceptedClosedMeter, _channelsTcpConnectedClosedMeter, _channelsTcpFailedMeter, _channelsTcpAcceptedMeter, _channelsTcpConnectedMeter;
    protected Meter _channelsUdpOpenMeter, _channelsUdpSharedOpenMeter, _channelsUdpUnsharedOpenMeter, _channelsUdpSharedFailedMeter, _channelsUdpUnsharedFailedMeter;
    protected Meter _channelsTcpConnectedDurationMeter, _channelsTcpAcceptedDurationMeter;
    protected Meter _serversTcpOpenMeter, _serversTcpFailedMeter;
    protected Meter _sendSctpMeter, _sendDroppedSctpMeter, _readSctpMeter, _channelsSctpOpenMeter, _channelsSctpAcceptedOpenMeter, _channelsSctpConnectedOpenMeter, _channelsSctpClosedMeter, _channelsSctpAcceptedClosedMeter, _channelsSctpConnectedClosedMeter, _channelsSctpFailedMeter, _channelsSctpAcceptedMeter, _channelsSctpConnectedMeter;
    protected Meter _channelsSctpConnectedDurationMeter, _channelsSctpAcceptedDurationMeter;
    protected Meter _serversSctpOpenMeter, _serversSctpFailedMeter;

    public IOHMeters (String name, MeteringService metering){
	this (name, name, metering);
    }
    public IOHMeters (String name, String desc, MeteringService metering){
	super (name, desc);
	_metering = metering;
    }
    
    public Meter createIncrementalMeter (String name, Meter parent){ return createIncrementalMeter (_metering, name, parent);}
    public Meter createAbsoluteMeter (String name){ return createAbsoluteMeter (_metering, name);}
    public Meter createValueSuppliedMeter (String name, ValueSupplier supplier){ return createValueSuppliedMeter (_metering, name, supplier);}
    public MeteringService getMetering (){ return _metering;}
    public Meter createRemoteMeter (String monitorable, String meter){ return createRemoteMeter (monitorable+":"+meter, monitorable, meter, 1000L);}
    public Meter createRemoteMeter (String name, String monitorable, String meter, long period){
	int id = REMOTE_METER_SEED.getAndIncrement ();
	Meter m = createAbsoluteMeter (name);
	_remoteMeters.put (id, m);
	MuxHeaderV0 h = new MuxHeaderV0 ();
	h.set (period, id, ExtendedMuxConnection.FLAG_MUX_METER_GET);
	String s = monitorable+" "+meter;
	java.nio.ByteBuffer b = null;
	try{b = java.nio.ByteBuffer.wrap (s.getBytes ("ascii"));}catch(Exception e){} // not possible
	MuxHandler handler = _agent.getMuxHandler ();
	if (handler instanceof ExtendedMuxHandler){
	    ((ExtendedMuxHandler)handler).internalMuxData (_agent, h, b);
	}
	return m;
    }
    public Meter setRemoteValue (MuxHeader header){
	int meterId = header.getChannelId ();
	Meter meter = _remoteMeters.get (meterId);
	meter.set (header.getSessionId ());
	return meter;
    }

    public IOHMeters setIOHEngineMeters (IOHEngine engine){
	if (engine.useTcp ()){
	    setTcpServerMeters ();
	    setTcpChannelMeters (null, engine.sharedTcpAccept (), engine.sharedTcpConnect (), engine.uniqueTcpConnect ());
	}
	if (engine.useSctp ()){
	    setSctpServerMeters ();
	    setSctpChannelMeters (null, engine.sharedSctpAccept (), engine.sharedSctpConnect (), engine.uniqueSctpConnect ());
	}
	if (engine.useUdp ()){
	    setUdpChannelMeters (null, engine.sharedUdpBind ());
	}
	return this;
    }
    public IOHMeters setMuxClientMeters (IOHEngine.MuxClient agent){
	_agent = agent;
	IOHEngine engine = agent.getIOHEngine ();
	if (engine.useTcp ())
	    setTcpChannelMeters (engine.getIOHMeters (), engine.sharedTcpAccept (), engine.sharedTcpConnect (), engine.uniqueTcpConnect ());
	if (engine.useSctp ())
	    setSctpChannelMeters (engine.getIOHMeters (), engine.sharedSctpAccept (), engine.sharedSctpConnect (), engine.uniqueSctpConnect ());
	if (engine.useUdp ())
	    setUdpChannelMeters (engine.getIOHMeters (), engine.sharedUdpBind ());
	addMeter (Meters.createUptimeMeter (_metering));
	return this;
    }

    // only for IOHEngine for now
    protected IOHMeters setTcpServerMeters (){
	_serversTcpOpenMeter = createIncrementalMeter (_metering, "server.open.tcp", null);
	_serversTcpFailedMeter = createIncrementalMeter (_metering, "server.failed.tcp", null);
	return this;
    }
    // only for IOHEngine for now
    protected IOHMeters setSctpServerMeters (){
	_serversSctpOpenMeter = createIncrementalMeter (_metering, "server.open.sctp", null);
	_serversSctpFailedMeter = createIncrementalMeter (_metering, "server.failed.sctp", null);
	return this;
    }
    
    protected IOHMeters setTcpChannelMeters (IOHMeters parent, boolean sharedAccept, boolean sharedConnect, boolean uniqueConnect){
	if (parent == null){
	    // We dont track for agents
	    _sendTcpMeter = createIncrementalMeter (_metering, "write.tcp",  null);
	    _sendDroppedTcpMeter = createIncrementalMeter (_metering, "write.tcp.dropped",  null);
	    _readTcpMeter = createIncrementalMeter (_metering, "read.tcp", null);
	}
	if (parent == null){
	    _channelsTcpOpenMeter = createIncrementalMeter (_metering, "channel.open.tcp", null);
	    _channelsTcpAcceptedOpenMeter = createIncrementalMeter (_metering, "channel.open.tcp.accept", _channelsTcpOpenMeter);
	    _channelsTcpConnectedOpenMeter = createIncrementalMeter (_metering, "channel.open.tcp.connect", _channelsTcpOpenMeter);
	} else {
	    if (sharedAccept == false)
		_channelsTcpAcceptedOpenMeter = createIncrementalMeter (_metering, "channel.open.tcp.accept", null); // dont set the parent
	    if (sharedConnect == false)
		_channelsTcpConnectedOpenMeter = createIncrementalMeter (_metering, "channel.open.tcp.connect", parent._channelsTcpConnectedOpenMeter);
	    else if (uniqueConnect)
		_channelsTcpConnectedOpenMeter = createIncrementalMeter (_metering, "channel.open.tcp.connect", null); // dont set the parent
	}
	
	if (parent == null)
	    _channelsTcpFailedMeter = createIncrementalMeter (_metering, "channel.failed.tcp.connect", null);
	else {
	    if (sharedConnect == false)
		_channelsTcpFailedMeter = createIncrementalMeter (_metering, "channel.failed.tcp.connect", parent._channelsTcpFailedMeter);
	    else if (uniqueConnect)
		_channelsTcpFailedMeter = createIncrementalMeter (_metering, "channel.failed.tcp.connect", null);
	}
	if (parent == null){
	    _channelsTcpClosedMeter = createIncrementalMeter (_metering, "channel.closed.tcp", null);
	    _channelsTcpAcceptedClosedMeter = createIncrementalMeter (_metering, "channel.closed.tcp.accept", _channelsTcpClosedMeter);
	    _channelsTcpConnectedClosedMeter = createIncrementalMeter (_metering, "channel.closed.tcp.connect", _channelsTcpClosedMeter);
	} else {
	    if (sharedAccept == false)
		_channelsTcpAcceptedClosedMeter = createIncrementalMeter (_metering, "channel.closed.tcp.accept", null); // dont set the parent
	    if (sharedConnect == false)
		_channelsTcpConnectedClosedMeter = createIncrementalMeter (_metering, "channel.closed.tcp.connect", parent._channelsTcpConnectedClosedMeter);
	    else if (uniqueConnect)
		_channelsTcpConnectedClosedMeter = createIncrementalMeter (_metering, "channel.closed.tcp.connect", null);
	}

	if (parent == null){
	    _channelsTcpAcceptedMeter = createIncrementalMeter (_metering, "channel.accepted.tcp", null);
	    _channelsTcpConnectedMeter = createIncrementalMeter (_metering, "channel.connected.tcp", null);
	    addMeter (Meters.createRateMeter (_metering, _channelsTcpAcceptedMeter, 1000L));
	    addMeter (Meters.createRateMeter (_metering, _channelsTcpConnectedMeter, 1000L));
	} else {
	    if (sharedAccept == false)
		_channelsTcpAcceptedMeter = createIncrementalMeter (_metering, "channel.accepted.tcp", null); // dont set the parent
	    if (sharedConnect == false)
		_channelsTcpConnectedMeter = createIncrementalMeter (_metering, "channel.connected.tcp", parent._channelsTcpConnectedMeter);
	    else if (uniqueConnect)
		_channelsTcpConnectedMeter = createIncrementalMeter (_metering, "channel.connected.tcp", null);
	}
	
	if (parent == null){
	    _channelsTcpAcceptedDurationMeter = createAbsoluteMeter (_metering, "channel.duration.tcp.accepted");
	    _channelsTcpConnectedDurationMeter = createAbsoluteMeter (_metering, "channel.duration.tcp.connected");
	}
	return this;
    }
    protected IOHMeters setSctpChannelMeters (IOHMeters parent, boolean sharedAccept, boolean sharedConnect, boolean uniqueConnect){
	if (parent == null){
	    // We dont track for agents
	    _sendSctpMeter = createIncrementalMeter (_metering, "write.sctp",  null);
	    _sendDroppedSctpMeter = createIncrementalMeter (_metering, "write.sctp.dropped",  null);
	    _readSctpMeter = createIncrementalMeter (_metering, "read.sctp", null);
	}
	if (parent == null){
	    _channelsSctpOpenMeter = createIncrementalMeter (_metering, "channel.open.sctp", null);
	    _channelsSctpAcceptedOpenMeter = createIncrementalMeter (_metering, "channel.open.sctp.accept", _channelsSctpOpenMeter);
	    _channelsSctpConnectedOpenMeter = createIncrementalMeter (_metering, "channel.open.sctp.connect", _channelsSctpOpenMeter);
	} else {
	    if (sharedAccept == false)
		_channelsSctpAcceptedOpenMeter = createIncrementalMeter (_metering, "channel.open.sctp.accept", null); // dont set the parent
	    if (sharedConnect == false)
		_channelsSctpConnectedOpenMeter = createIncrementalMeter (_metering, "channel.open.sctp.connect", parent._channelsSctpConnectedOpenMeter);
	    else if (uniqueConnect)
		_channelsSctpConnectedOpenMeter = createIncrementalMeter (_metering, "channel.open.sctp.connect", null); // dont set the parent
	}
	
	if (parent == null)
	    _channelsSctpFailedMeter = createIncrementalMeter (_metering, "channel.failed.sctp.connect", null);
	else {
	    if (sharedConnect == false)
		_channelsSctpFailedMeter = createIncrementalMeter (_metering, "channel.failed.sctp.connect", parent._channelsSctpFailedMeter);
	    else if (uniqueConnect)
		_channelsSctpFailedMeter = createIncrementalMeter (_metering, "channel.failed.sctp.connect", null);
	}
	if (parent == null){
	    _channelsSctpClosedMeter = createIncrementalMeter (_metering, "channel.closed.sctp", null);
	    _channelsSctpAcceptedClosedMeter = createIncrementalMeter (_metering, "channel.closed.sctp.accept", _channelsSctpClosedMeter);
	    _channelsSctpConnectedClosedMeter = createIncrementalMeter (_metering, "channel.closed.sctp.connect", _channelsSctpClosedMeter);
	} else {
	    if (sharedAccept == false)
		_channelsSctpAcceptedClosedMeter = createIncrementalMeter (_metering, "channel.closed.sctp.accept", null); // dont set the parent
	    if (sharedConnect == false)
		_channelsSctpConnectedClosedMeter = createIncrementalMeter (_metering, "channel.closed.sctp.connect", parent._channelsSctpConnectedClosedMeter);
	    else if (uniqueConnect)
		_channelsSctpConnectedClosedMeter = createIncrementalMeter (_metering, "channel.closed.sctp.connect", null);
	}

	if (parent == null){
	    _channelsSctpAcceptedMeter = createIncrementalMeter (_metering, "channel.accepted.sctp", null);
	    _channelsSctpConnectedMeter = createIncrementalMeter (_metering, "channel.connected.sctp", null);
	    addMeter (Meters.createRateMeter (_metering, _channelsSctpAcceptedMeter, 1000L));
	    addMeter (Meters.createRateMeter (_metering, _channelsSctpConnectedMeter, 1000L));
	} else {
	    if (sharedAccept == false)
		_channelsSctpAcceptedMeter = createIncrementalMeter (_metering, "channel.accepted.sctp", null); // dont set the parent
	    if (sharedConnect == false)
		_channelsSctpConnectedMeter = createIncrementalMeter (_metering, "channel.connected.sctp", parent._channelsSctpConnectedMeter);
	    else if (uniqueConnect)
		_channelsSctpConnectedMeter = createIncrementalMeter (_metering, "channel.connected.sctp", null);
	}
	
	if (parent == null){
	    _channelsSctpAcceptedDurationMeter = createAbsoluteMeter (_metering, "channel.duration.sctp.accepted");
	    _channelsSctpConnectedDurationMeter = createAbsoluteMeter (_metering, "channel.duration.sctp.connected");
	}
	return this;
    }
    protected IOHMeters setUdpChannelMeters (IOHMeters parent, boolean sharedUdpOnly){
	if (parent == null){
	    _sendUdpMeter = createIncrementalMeter (_metering, "write.udp", null);
	    _sendDroppedUdpMeter = createIncrementalMeter (_metering, "write.udp.dropped", null);
	    _readUdpMeter = createIncrementalMeter (_metering, "read.udp", null);
	    _channelsUdpOpenMeter = createIncrementalMeter (_metering, "channel.open.udp", null);
	    if (sharedUdpOnly){
		_channelsUdpSharedOpenMeter = _channelsUdpOpenMeter;
		_channelsUdpSharedFailedMeter = createIncrementalMeter (_metering, "channel.failed.udp", null);
	    } else {
		_channelsUdpSharedOpenMeter = createIncrementalMeter (_metering, "channel.open.udp.shared", _channelsUdpOpenMeter);
		_channelsUdpUnsharedOpenMeter = createIncrementalMeter (_metering, "channel.open.udp.unshared", _channelsUdpOpenMeter);
		_channelsUdpSharedFailedMeter = createIncrementalMeter (_metering, "channel.failed.udp.shared", null);
		_channelsUdpUnsharedFailedMeter = createIncrementalMeter (_metering, "channel.failed.udp.unshared", null);
	    }
	}else{
	    //TBD
	}
	return this;
    }
    
    public Meter getOpenTcpServersMeter (){ return _serversTcpOpenMeter;}
    public Meter getFailedTcpServersMeter (){ return _serversTcpFailedMeter;}

    public Meter getOpenSctpServersMeter (){ return _serversSctpOpenMeter;}
    public Meter getFailedSctpServersMeter (){ return _serversSctpFailedMeter;}
    
    public Meter getOpenSharedUdpChannelsMeter (){ return _channelsUdpSharedOpenMeter;}
    public Meter getOpenUnsharedUdpChannelsMeter (){ return _channelsUdpUnsharedOpenMeter;}
    public Meter getFailedSharedUdpChannelsMeter (){ return _channelsUdpSharedFailedMeter;}
    public Meter getFailedUnsharedUdpChannelsMeter (){ return _channelsUdpUnsharedFailedMeter;}

    public Meter getSendTcpMeter (){ return _sendTcpMeter;}
    public Meter getSendDroppedTcpMeter (){ return _sendDroppedTcpMeter;}
    public Meter getSendUdpMeter (){ return _sendUdpMeter;}
    public Meter getSendDroppedUdpMeter (){ return _sendDroppedUdpMeter;}
    public Meter getReadTcpMeter (){ return _readTcpMeter;}
    public Meter getReadUdpMeter (){ return _readUdpMeter;}
    public Meter getTcpChannelsAcceptedMeter (){ return _channelsTcpAcceptedMeter;}
    public Meter getTcpChannelsConnectedMeter (){ return _channelsTcpConnectedMeter;}
    public Meter getOpenTcpChannelsMeter (){ return _channelsTcpOpenMeter;}
    public Meter getOpenTcpChannelsAcceptedMeter (){ return _channelsTcpAcceptedOpenMeter;}
    public Meter getOpenTcpChannelsConnectedMeter (){ return _channelsTcpConnectedOpenMeter;}
    public Meter getClosedTcpChannelsAcceptedMeter (){ return _channelsTcpAcceptedClosedMeter;}
    public Meter getClosedTcpChannelsConnectedMeter (){ return _channelsTcpConnectedClosedMeter;}
    public Meter getFailedTcpChannelsConnectMeter (){ return _channelsTcpFailedMeter;}
    public Meter getTcpChannelsAcceptedDurationMeter (){ return _channelsTcpAcceptedDurationMeter;}
    public Meter getTcpChannelsConnectedDurationMeter (){ return _channelsTcpConnectedDurationMeter;}
    public Meter getSendSctpMeter (){ return _sendSctpMeter;}
    public Meter getSendDroppedSctpMeter (){ return _sendDroppedSctpMeter;}
    public Meter getReadSctpMeter (){ return _readSctpMeter;}
    public Meter getOpenSctpChannelsMeter (){ return _channelsSctpOpenMeter;}
    public Meter getSctpChannelsAcceptedMeter (){ return _channelsSctpAcceptedMeter;}
    public Meter getSctpChannelsConnectedMeter (){ return _channelsSctpConnectedMeter;}
    public Meter getOpenSctpChannelsAcceptedMeter (){ return _channelsSctpAcceptedOpenMeter;}
    public Meter getOpenSctpChannelsConnectedMeter (){ return _channelsSctpConnectedOpenMeter;}
    public Meter getClosedSctpChannelsAcceptedMeter (){ return _channelsSctpAcceptedClosedMeter;}
    public Meter getClosedSctpChannelsConnectedMeter (){ return _channelsSctpConnectedClosedMeter;}
    public Meter getFailedSctpChannelsConnectMeter (){ return _channelsSctpFailedMeter;}
    public Meter getSctpChannelsAcceptedDurationMeter (){ return _channelsSctpAcceptedDurationMeter;}
    public Meter getSctpChannelsConnectedDurationMeter (){ return _channelsSctpConnectedDurationMeter;}
}
