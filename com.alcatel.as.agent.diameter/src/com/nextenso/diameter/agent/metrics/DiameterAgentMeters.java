// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.metrics;

import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.metering2.util.Meters;
import com.nextenso.diameter.agent.RequestManager;

public class DiameterAgentMeters extends DiameterMeters {
	public DiameterAgentMeters(SimpleMonitorable _mon, MeteringService metering) {
		this._mon = _mon;
		createMeters(metering, _mon, true);
		RequestManager.createMeters(metering, _mon);
	}
	
	public void agentStopped() {
		Meters.stopRateMeter(_readReqRateMeter);
		Meters.stopRateMeter(_writeReqRateMeter);
		Meters.stopRateMeter(_readRespRateMeter);
		Meters.stopRateMeter(_writeRespRateMeter);
		_mon.stop();
	}
	
	//Called when a socket close, does nothing for agent-level meters
	@Override
	public void socketClosed() {
	}
	
	@Override
	public void incParseFailedMeter() {
		_parseFailedMeter.inc(1);
	}

	@Override
	protected void incReadCERMeter() {
		_readCERMeter.inc(1);
	}

	@Override
	protected void incReadCEAMeter() {
		_readCEAMeter.inc(1);
	}

	@Override
	protected void incReadCURMeter() {
		_readCURMeter.inc(1);
	}

	@Override
	protected void incReadCUAMeter() {	
		_readCUAMeter.inc(1);
	}

	@Override
	protected void incReadDWRMeter() {
		_readDWRMeter.inc(1);
	}

	@Override
	protected void incReadDWAMeter() {
		_readDWAMeter.inc(1);
	}

	@Override
	protected void incReadDPRMeter() {
		_readDPRMeter.inc(1);
	}

	@Override
	protected void incReadDPAMeter() {
		_readDPAMeter.inc(1);
	}

	@Override
	protected void incWriteCERMeter() {
		_writeCERMeter.inc(1);
	}

	@Override
	protected void incWriteCEAMeter() {
		_writeCEAMeter.inc(1);
	}

	@Override
	protected void incWriteCURMeter() {
		_writeCURMeter.inc(1);
	}

	@Override
	protected void incWriteCUAMeter() {
		_writeCUAMeter.inc(1);
	}

	@Override
	protected void incWriteDWRMeter() {
		_writeDWRMeter.inc(1);
	}

	@Override
	protected void incWriteDWAMeter() {
		_writeDWAMeter.inc(1);
	}

	@Override
	protected void incWriteDPRMeter() {
		_writeDPRMeter.inc(1);
	}

	@Override
	protected void incWriteDPAMeter() {
		_writeDPAMeter.inc(1);
	}
}
