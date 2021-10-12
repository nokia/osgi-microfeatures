// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb;

import java.util.*;
import java.util.concurrent.atomic.*;

import com.nextenso.mux.impl.ioh.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;

public class Meters extends SimpleMonitorable {

    protected MeteringService _metering;
    
    protected Meter _channelsTcpOpenMeter, _channelsTcpClosedMeter, _sendTcpMeter, _readTcpMeter, _readTcpMessageMeter;
    protected Meter _readTcpMessageMeterRate;
    protected Meter _channelsTcpOpenDestMeter, _sendTcpDestMeter, _readTcpDestMeter, _sendTcpDroppedDestMeter, _channelsTcpFailedDestMeter;
    protected Meter _overloadDestMeter;
    
    public Meters (String name, String desc, MeteringService metering){
	super (name, desc);
	_metering = metering;
    }
    
    public Meter createIncrementalMeter (String name, Meter parent){ return createIncrementalMeter (_metering, name, parent);}
    public Meter createAbsoluteMeter (String name){ return createAbsoluteMeter (_metering, name);}
    public Meter createValueSuppliedMeter (String name, ValueSupplier supplier){ return createValueSuppliedMeter (_metering, name, supplier);}
    public Meter createRateMeter (Meter target){
	Meter rate = com.alcatel.as.service.metering2.util.Meters.createRateMeter (_metering, target, 1000);
	addMeter (rate);
	return rate;
    }
    public void stopRateMeter (Meter meter){ com.alcatel.as.service.metering2.util.Meters.stopRateMeter (meter);}

    public Meters init (){
	_channelsTcpOpenMeter = createIncrementalMeter ("channel.open.tcp", null);
	_channelsTcpClosedMeter = createIncrementalMeter ("channel.closed.tcp", null);
	_readTcpMeter = createIncrementalMeter ("read.tcp", null);
	_readTcpMessageMeter = createIncrementalMeter ("read.tcp.message", null);
	_sendTcpMeter = createIncrementalMeter ("write.tcp", null);
	_readTcpMessageMeterRate = createRateMeter (_readTcpMessageMeter);

	_channelsTcpOpenDestMeter = createIncrementalMeter ("dest.channel.open.tcp", null);
	_channelsTcpFailedDestMeter = createIncrementalMeter ("dest.channel.failed.tcp", null);
	_readTcpDestMeter = createIncrementalMeter ("dest.read.tcp", null);
	_sendTcpDestMeter = createIncrementalMeter ("dest.write.tcp", null);
	_sendTcpDroppedDestMeter = createIncrementalMeter ("dest.write.tcp.dropped", null);

	_overloadDestMeter = createIncrementalMeter ("dest.overload", null);
	
	return this;
    }
    @Override
    public void stop (){
	stopRateMeter (_readTcpMessageMeterRate);
	super.stop ();
    }

    public Meter getSendTcpMeter (){ return _sendTcpMeter;}
    public Meter getReadTcpMeter (){ return _readTcpMeter;}
    public Meter getReadTcpMessageMeter (){ return _readTcpMessageMeter;}
    public Meter getOpenTcpChannelsMeter (){ return _channelsTcpOpenMeter;}
    public Meter getClosedTcpChannelsMeter (){ return _channelsTcpClosedMeter;}

    public Meter getSendTcpDestMeter (){ return _sendTcpDestMeter;}
    public Meter getReadTcpDestMeter (){ return _readTcpDestMeter;}
    public Meter getOpenTcpChannelsDestMeter (){ return _channelsTcpOpenDestMeter;}
    public Meter getSendTcpDroppedDestMeter (){ return _sendTcpDroppedDestMeter;}
    public Meter getFailedTcpChannelsDestMeter (){ return _channelsTcpFailedDestMeter;}

    public Meter getOverloadDestMeter (){ return _overloadDestMeter;}
    
}
