package com.alcatel.as.ioh.lb;

import java.util.*;
import java.util.concurrent.atomic.*;

import com.nextenso.mux.impl.ioh.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;

public class UdpMeters extends SimpleMonitorable {

    protected MeteringService _metering;

    protected Meter _channelsUdpOpenMeter, _channelsUdpClosedMeter, _channelsUdpFailedMeter, _sendUdpMeter, _readUdpMeter, _readUdpMessageMeter, _failedUdpMeter;
    protected Meter _sessionsActiveMeter, _sessionsPoolMeter;
    protected Meter _readUdpMessageMeterRate;
    protected Meter _sendUdpDestMeter, _readUdpDestMeter;
    protected Meter _overloadDestMeter, _overloadClientMeter;
    
    public UdpMeters (String name, String desc, MeteringService metering){
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

    public UdpMeters init (){
	_channelsUdpOpenMeter = createIncrementalMeter ("channel.open.udp", null);
	_channelsUdpClosedMeter = createIncrementalMeter ("channel.closed.udp", null);
	_channelsUdpFailedMeter = createIncrementalMeter ("channel.failed.udp", null);

	_sessionsActiveMeter = createIncrementalMeter ("session.active", null);
	_sessionsPoolMeter = createIncrementalMeter ("session.pool", null);
	
	_readUdpMeter = createIncrementalMeter ("read.udp", null);
	_readUdpMessageMeter = createIncrementalMeter ("read.udp.message", null);
	_sendUdpMeter = createIncrementalMeter ("write.udp", null);
	_readUdpMessageMeterRate = createRateMeter (_readUdpMessageMeter);
	_failedUdpMeter = createIncrementalMeter ("failed.udp", null);

	_readUdpDestMeter = createIncrementalMeter ("dest.read.udp", null);
	_sendUdpDestMeter = createIncrementalMeter ("dest.write.udp", null);
	
	_overloadDestMeter = createIncrementalMeter ("dest.overload", null);
	_overloadClientMeter = createIncrementalMeter ("client.overload", null);
	
	return this;
    }
    @Override
    public void stop (){
	stopRateMeter (_readUdpMessageMeterRate);
	super.stop ();
    }

    public Meter getSendUdpMeter (){ return _sendUdpMeter;}
    public Meter getReadUdpMeter (){ return _readUdpMeter;}
    public Meter getReadUdpMessageMeter (){ return _readUdpMessageMeter;}
    public Meter getOpenUdpChannelsMeter (){ return _channelsUdpOpenMeter;}
    public Meter getClosedUdpChannelsMeter (){ return _channelsUdpClosedMeter;}
    public Meter getFailedUdpChannelsMeter (){ return _channelsUdpFailedMeter;}
    public Meter getFailedUdpMeter (){ return _failedUdpMeter;}
    public Meter getSessionsActiveMeter (){return _sessionsActiveMeter;}
    public Meter getSessionsPoolMeter (){return _sessionsPoolMeter;}

    public Meter getSendUdpDestMeter (){ return _sendUdpDestMeter;}
    public Meter getReadUdpDestMeter (){ return _readUdpDestMeter;}
    
    public Meter getOverloadDestMeter (){ return _overloadDestMeter;}
    public Meter getOverloadClientMeter (){ return _overloadClientMeter;}
}
