// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.socks;

import java.util.*;
import java.util.concurrent.atomic.*;

import com.nextenso.mux.impl.ioh.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;

public class Meters extends SimpleMonitorable {

    protected MeteringService _metering;
    
    protected Meter _clientChannelsTcpOpenMeter, _clientChannelsTcpClosedMeter, _clientSendTcpMeter, _clientReadTcpMeter;
    protected Meter _clientOverloadTcpMeter;
    protected Meter _destChannelsTcpOpenMeter, _destChannelsTcpClosedMeter, _destChannelsTcpFailedMeter, _destSendTcpMeter, _destReadTcpMeter;
    protected Meter _destOverloadTcpMeter;

    protected Meter _authFailed;
    
    public Meters (String name, String desc, MeteringService metering){
	super (name, desc);
	_metering = metering;
    }
    
    public Meter createIncrementalMeter (String name, Meter parent){ return createIncrementalMeter (_metering, name, parent);}
    public Meter createAbsoluteMeter (String name){ return createAbsoluteMeter (_metering, name);}
    public Meter createValueSuppliedMeter (String name, ValueSupplier supplier){ return createValueSuppliedMeter (_metering, name, supplier);}
    
    public Meters init (){
	_clientChannelsTcpOpenMeter = createIncrementalMeter ("client.open.tcp", null);
	_clientChannelsTcpClosedMeter = createIncrementalMeter ("client.closed.tcp", null);
	_clientReadTcpMeter = createIncrementalMeter ("client.read.tcp", null);
	_clientSendTcpMeter = createIncrementalMeter ("client.write.tcp", null);
	_clientOverloadTcpMeter = createIncrementalMeter ("client.overload.tcp", null);
	
	_destChannelsTcpOpenMeter = createIncrementalMeter ("dest.open.tcp", null);
	_destChannelsTcpClosedMeter = createIncrementalMeter ("dest.closed.tcp", null);
	_destChannelsTcpFailedMeter = createIncrementalMeter ("dest.failed.tcp", null);
	_destReadTcpMeter = createIncrementalMeter ("dest.read.tcp", null);
	_destSendTcpMeter = createIncrementalMeter ("dest.write.tcp", null);
	_destOverloadTcpMeter = createIncrementalMeter ("dest.overload.tcp", null);

	_authFailed = createIncrementalMeter ("auth.failed", null);
	
	return this;
    }
}
