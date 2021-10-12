// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb.impl.router;


import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;


public class RoutingMeters {

    public Meter _meterErrorOther, _meterOverloadClient, _meterOverloadLocal, _meterOverloadServer, _meterUnroutable;
    public Meter _meterProcessing;

    public RoutingMeters (){
    }
    public RoutingMeters init (SimpleMonitorable monitorable, MeteringService metering, RoutingMeters parent){
	_meterErrorOther = monitorable.createIncrementalMeter (metering,
							       parent != null ? "router:error.other" : "error.other",
							       parent != null ? parent._meterErrorOther : null);
	_meterOverloadClient = monitorable.createIncrementalMeter (metering,
								   parent != null ? "router:error.3004.client" : "error.3004.client",
								   parent != null ? parent._meterOverloadClient : null);
	_meterOverloadLocal = monitorable.createIncrementalMeter (metering,
								  parent != null ? "router:error.3004.local" : "error.3004.local",
								  parent != null ? parent._meterOverloadLocal : null);
	_meterOverloadServer = monitorable.createIncrementalMeter (metering,
								  parent != null ? "router:error.3004.server" : "error.3004.server",
								   parent != null ? parent._meterOverloadServer : null);
	_meterUnroutable = monitorable.createIncrementalMeter (metering,
							       parent != null ? "router:error.3002" : "error.3002",
							       parent != null ? parent._meterUnroutable : null);
	
	if (parent != null )
	    // these are client meters
	    _meterProcessing = monitorable.createIncrementalMeter (metering, "router:reqs.processing", null);

	return this;
    }

    public Meter getErrorOtherMeter (){ return _meterErrorOther;}
    public Meter getClientOverloadMeter (){ return _meterOverloadClient;}
    public Meter getLocalOverloadMeter (){ return _meterOverloadLocal;}
    public Meter getServerOverloadMeter (){ return _meterOverloadServer;}
    public Meter getUnroutableMeter (){ return _meterUnroutable;}
    public Meter getProcessingReqsMeter (){ return _meterProcessing;}
}
