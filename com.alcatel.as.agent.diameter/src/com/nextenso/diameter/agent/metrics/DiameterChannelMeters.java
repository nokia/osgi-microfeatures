// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.metrics;

import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.metering2.util.Meters;
import com.nextenso.proxylet.diameter.DiameterMessage;

public class DiameterChannelMeters extends DiameterMeters {
	private DiameterMeters _parent;
	private boolean forSocket;
	
	public DiameterChannelMeters(SimpleMonitorable _mon, MeteringService metering, DiameterMeters parent, boolean forSocket) {
		this._mon = _mon;
		createMeters(metering, _mon, false);
		this._parent = parent;
		this.forSocket = forSocket;
	}
	
	@Override
	public void socketClosed() {
		if(forSocket) {
			_mon.stop();
		}
	}

	@Override
	public void incParseFailedMeter() {
		_parseFailedMeter.inc(1);
		_parent.incParseFailedMeter();
	}
	
	@Override
	protected void incReadCERMeter() {
		_readCERMeter.inc(1);
		_parent.incReadCERMeter();
	}

	@Override
	protected void incReadCEAMeter() {
		_readCEAMeter.inc(1);
		_parent.incReadCEAMeter();
	}

	@Override
	protected void incReadCURMeter() {
		_readCURMeter.inc(1);
		_parent.incReadCURMeter();
	}

	@Override
	protected void incReadCUAMeter() {	
		_readCUAMeter.inc(1);
		_parent.incReadCUAMeter();
	}

	@Override
	protected void incReadDWRMeter() {
		_readDWRMeter.inc(1);
		_parent.incReadDWRMeter();
	}

	@Override
	protected void incReadDWAMeter() {
		_readDWAMeter.inc(1);
		_parent.incReadDWAMeter();
	}

	@Override
	protected void incReadDPRMeter() {
		_readDPRMeter.inc(1);
		_parent.incReadDPRMeter();
	}

	@Override
	protected void incReadDPAMeter() {
		_readDPAMeter.inc(1);
		_parent.incReadDPAMeter();
	}

	@Override
	protected void incWriteCERMeter() {
		_writeCERMeter.inc(1);
		_parent.incWriteCERMeter();
	}

	@Override
	protected void incWriteCEAMeter() {
		_writeCEAMeter.inc(1);
		_parent.incWriteCEAMeter();
	}

	@Override
	protected void incWriteCURMeter() {
		_writeCURMeter.inc(1);
		_parent.incWriteCURMeter();
	}

	@Override
	protected void incWriteCUAMeter() {
		_writeCUAMeter.inc(1);
		_parent.incWriteCUAMeter();
	}

	@Override
	protected void incWriteDWRMeter() {
		_writeDWRMeter.inc(1);
		_parent.incWriteDWRMeter();
	}

	@Override
	protected void incWriteDWAMeter() {
		_writeDWAMeter.inc(1);
		_parent.incWriteDWAMeter();
	}

	@Override
	protected void incWriteDPRMeter() {
		_writeDPRMeter.inc(1);
		_parent.incWriteDPRMeter();
	}

	@Override
	protected void incWriteDPAMeter() {
		_writeDPAMeter.inc(1);
		_parent.incWriteDPAMeter();
	}
	
	protected void incSendAppReqMeter(DiameterMessage msg) {
		super.incSendAppReqMeter(msg);
		_parent.incSendAppReqMeter(msg);
	}

	protected void incSendAppRespMeter(DiameterMessage msg) {
		super.incSendAppRespMeter(msg);
		_parent.incSendAppRespMeter(msg);
	}

	protected void incReadAppReqMeter(DiameterMessage msg) {
		super.incReadAppReqMeter(msg);
		_parent.incReadAppReqMeter(msg);
	}

	protected void incReadAppRespMeter(DiameterMessage msg) {
		super.incReadAppRespMeter(msg);
		_parent.incReadAppRespMeter(msg);
	}
}
