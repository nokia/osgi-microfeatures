package com.alcatel.as.http.proxy;

import java.util.*;
import java.util.concurrent.atomic.*;

import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;

public class Meters extends SimpleMonitorable {

    protected MeteringService _metering;
    
    protected Meter _channelsTcpAcceptOpenMeter, _channelsTcpAcceptClosedMeter;
    protected Meter _channelsTcpConnectOpenMeter, _channelsTcpConnectClosedMeter, _channelsTcpConnectFailedMeter;
    protected Meter _channelsTcpSwitchMeter;
    protected Meter _readReqsMeter, _readReqsRateMeter, _readFailedReqsMeter;
    protected Meter _readGETMeter, _readPOSTMeter, _readPUTMeter, _readDELETEMeter, _readCONNECTMeter, _readPATCHMeter, _readHEADMeter, _readOTHERMeter;
    protected Meter _parserErrorMeter, _dnsFailedMeter;
    
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
	_channelsTcpAcceptOpenMeter = createIncrementalMeter ("channel.open.tcp.accept", null);
	_channelsTcpAcceptClosedMeter = createIncrementalMeter ("channel.closed.tcp.accept", null);
	_channelsTcpConnectOpenMeter = createIncrementalMeter ("channel.open.tcp.connect", null);
	_channelsTcpConnectFailedMeter = createIncrementalMeter ("channel.failed.tcp.connect", null);
	_channelsTcpConnectClosedMeter = createIncrementalMeter ("channel.closed.tcp.connect", null);
	_channelsTcpSwitchMeter = createIncrementalMeter ("channel.switch", null);

	_readReqsMeter = createIncrementalMeter ("read.req", null);
	_readReqsRateMeter = createRateMeter (_readReqsMeter);

	_readGETMeter = createIncrementalMeter ("read.req.GET", _readReqsMeter);
	_readPUTMeter = createIncrementalMeter ("read.req.PUT", _readReqsMeter);
	_readPOSTMeter = createIncrementalMeter ("read.req.POST", _readReqsMeter);
	_readDELETEMeter = createIncrementalMeter ("read.req.DELETE", _readReqsMeter);
	_readCONNECTMeter = createIncrementalMeter ("read.req.CONNECT", _readReqsMeter);
	_readHEADMeter = createIncrementalMeter ("read.req.HEAD", _readReqsMeter);
	_readPATCHMeter = createIncrementalMeter ("read.req.PATCH", _readReqsMeter);
	_readOTHERMeter = createIncrementalMeter ("read.req.OTHER", _readReqsMeter);

	_readFailedReqsMeter = createIncrementalMeter ("failed.req", null);
	_parserErrorMeter = createIncrementalMeter ("failed.parser", null);
	_dnsFailedMeter = createIncrementalMeter ("failed.dns", null);
	
	return this;
    }
    @Override
    public void stop (){
	stopRateMeter (_readReqsRateMeter);
	super.stop ();
    }

    public Meter getOpenAcceptedChannelsMeter (){ return _channelsTcpAcceptOpenMeter;}
    public Meter getClosedAcceptedChannelsMeter (){ return _channelsTcpAcceptClosedMeter;}
    public Meter getOpenConnectedChannelsMeter (){ return _channelsTcpConnectOpenMeter;}
    public Meter getFailedConnectedChannelsMeter (){ return _channelsTcpConnectFailedMeter;}
    public Meter getClosedConnectedChannelsMeter (){ return _channelsTcpConnectClosedMeter;}
    public Meter getChannelSwitchMeter (){ return _channelsTcpSwitchMeter;}

    public Meter getFailedReqsMeter (){ return _readFailedReqsMeter;}
    public Meter getParserErrorMeter (){ return _parserErrorMeter;}
    public Meter getFailedDNSMeter (){ return _dnsFailedMeter;}

    public Meter getReadReqMeter (String method){
	switch (method){
	case "GET": return _readGETMeter;
	case "PUT": return _readPUTMeter;
	case "POST": return _readPOSTMeter;
	case "DELETE": return _readDELETEMeter;
	case "HEAD": return _readHEADMeter;
	case "PATCH": return _readPATCHMeter;
	case "CONNECT": return _readCONNECTMeter;
	default: return _readReqsMeter;
	}
    }
    
}
