// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.radius.ioh;

import java.util.*;

import com.alcatel.as.radius.parser.RadiusMessage;

import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;

public class RadiusIOHMeters extends SimpleMonitorable {
    
    protected Meter _MsgMeter, _ReqMeter, _RespMeter;
    protected Meter _AccessRequestMeter, _AccessAcceptMeter, _AccessChallengeMeter, _AccessRejectMeter;
    protected Meter _AcctReqMeter, _AcctRespMeter;
    protected Meter _CoARequestMeter, _CoAAckMeter, _CoANackMeter;
    protected Meter _DisconnectRequestMeter, _DisconnectAckMeter, _DisconnectNackMeter;
    protected String _prefix;
    
    public RadiusIOHMeters (String name, String desc){
	super (name, desc);
    }
    
    public RadiusIOHMeters init (MeteringService metering, SimpleMonitorable dest, String prefix, boolean makeRates){
	_prefix = prefix;
	createMeters (metering, dest, makeRates);
	return this;
    }
    
    private void createMeters (MeteringService metering, SimpleMonitorable dest, boolean makeRates){
	_MsgMeter = dest.createIncrementalMeter (metering, _prefix+".msg", null);
	_ReqMeter = dest.createIncrementalMeter (metering, _prefix+".msg.req", _MsgMeter);
	_RespMeter = dest.createIncrementalMeter (metering, _prefix+".msg.resp", _MsgMeter);
	_AccessRequestMeter = dest.createIncrementalMeter (metering, _prefix+".msg.req.access", _ReqMeter);
	_AccessAcceptMeter = dest.createIncrementalMeter (metering, _prefix+".msg.resp.accept", _RespMeter);
	_AccessChallengeMeter = dest.createIncrementalMeter (metering, _prefix+".msg.resp.challenge", _RespMeter);
	_AccessRejectMeter = dest.createIncrementalMeter (metering, _prefix+".msg.resp.reject", _RespMeter);
	_AcctReqMeter = dest.createIncrementalMeter (metering, _prefix+".msg.req.acct", _ReqMeter);
	_AcctRespMeter = dest.createIncrementalMeter (metering, _prefix+".msg.resp.acct", _RespMeter);
	_CoARequestMeter = dest.createIncrementalMeter (metering, _prefix+".msg.req.coa", _ReqMeter);
	_CoAAckMeter = dest.createIncrementalMeter (metering, _prefix+".msg.resp.coa-ack", _RespMeter);
	_CoANackMeter = dest.createIncrementalMeter (metering, _prefix+".msg.resp.coa-nack", _RespMeter);
	_DisconnectRequestMeter = dest.createIncrementalMeter (metering, _prefix+".msg.req.disc", _ReqMeter);
	_DisconnectAckMeter = dest.createIncrementalMeter (metering, _prefix+".msg.resp.disc-ack", _RespMeter);
	_DisconnectNackMeter = dest.createIncrementalMeter (metering, _prefix+".msg.resp.disc-nack", _RespMeter);
	if (makeRates){
	    dest.addMeter (Meters.createRateMeter (metering, _ReqMeter, 1000L));
	    dest.addMeter (Meters.createRateMeter (metering, _RespMeter, 1000L));
	}
    }

    public Meter getMeter (RadiusMessage msg){
	return getMeter (msg.getCode ());
    }
    public Meter getMeter (int code){
	switch (code){
	case 1 : return _AccessRequestMeter;
	case 2 : return _AccessAcceptMeter;
	case 3 : return _AccessRejectMeter;
	case 11 : return _AccessChallengeMeter;
	case 4 : return _AcctReqMeter;
	case 5 : return _AcctRespMeter;
	case 40 : return _DisconnectRequestMeter;
	case 41 : return _DisconnectAckMeter;
	case 42 : return _DisconnectNackMeter;
	case 43 : return _CoARequestMeter;
	case 44 : return _CoAAckMeter;
	case 45 : return _CoANackMeter;
	default : return _MsgMeter;
	}
    }
    
}
