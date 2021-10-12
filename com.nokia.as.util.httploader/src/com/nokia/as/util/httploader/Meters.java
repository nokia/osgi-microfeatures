// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.util.httploader;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.metering2.ValueSupplier;

public class Meters extends SimpleMonitorable {

	protected MeteringService _metering;

	/*Request Http Received Meter*/
	protected Meter _requestHttpReceivedMeter;
	protected Meter _requestHttpReceivedOkMeter , _requestHttpReceivedKoMeter;
	protected Meter _requestHttpReceivedOkMeterRate , _requestHttpReceivedKoMeterRate;

	/*Request Http Send Meter*/	
	protected Meter _requestHttpSendMeter;
	protected Meter _requestHttpSendMeterRate;
	protected Meter _requestHttpSendGETMeter ,_requestHttpSendPOSTMeter, _requestHttpSendDELETEMeter, _requestHttpSendPUTMeter;

	
	public Meters(String name, String desc, MeteringService metering) {
		super(name, desc);
		_metering = metering;
	}

	public Meter createIncrementalMeter(String name, Meter parent) {
		return createIncrementalMeter(_metering, name, parent);
	}

	public Meter createAbsoluteMeter(String name) {
		return createAbsoluteMeter(_metering, name);
	}

	public Meter createValueSuppliedMeter(String name, ValueSupplier supplier) {
		return createValueSuppliedMeter(_metering, name, supplier);
	}

	public Meter createRateMeter(Meter target) {
		Meter rate = com.alcatel.as.service.metering2.util.Meters.createRateMeter(_metering, target, 1000);
		addMeter(rate);
		return rate;
	}

	public void stopRateMeter(Meter meter) {
		com.alcatel.as.service.metering2.util.Meters.stopRateMeter(meter);
	}

	public Meters init() {
		_requestHttpReceivedMeter = createIncrementalMeter("loader.request.received.http",null);
		_requestHttpReceivedOkMeter = createIncrementalMeter("loader.request.received.http.ok", _requestHttpReceivedMeter);
		_requestHttpReceivedKoMeter = createIncrementalMeter("loader.request.received.http.ko", _requestHttpReceivedMeter);
		_requestHttpReceivedOkMeterRate = createRateMeter(_requestHttpReceivedOkMeter);
		_requestHttpReceivedKoMeterRate = createRateMeter(_requestHttpReceivedKoMeter);
		
		_requestHttpSendMeter = createIncrementalMeter("loader.request.send.http", null);
		_requestHttpSendMeterRate = createRateMeter(_requestHttpSendMeter);
		_requestHttpSendGETMeter = createIncrementalMeter("loader.request.send.http.GET",_requestHttpSendMeter );
		_requestHttpSendPOSTMeter = createIncrementalMeter("loader.request.send.http.POST",_requestHttpSendMeter );
		_requestHttpSendDELETEMeter = createIncrementalMeter("loader.request.send.http.DELETE",_requestHttpSendMeter );
		_requestHttpSendPUTMeter = createIncrementalMeter("loader.request.send.http.PUT",_requestHttpSendMeter );
		return this;
	}

	/*@Override
	public void stop() {
		stopRateMeter(_readReqsRateMeter);
		super.stop();
	}*/

	public Meter getRequestHttpReceivedMeter() {
		return _requestHttpReceivedMeter;
	}
	
	public Meter getRequestHttpReceivedOkMeter() {
		return _requestHttpReceivedOkMeter;
	}
	
	public Meter getRequestHttpReceivedKoMeter() {
		return _requestHttpReceivedKoMeter;
	}
	
	public Meter getRequestHttpReceivedOkMeterRate() {
		return _requestHttpReceivedOkMeterRate;
	}
	
	public Meter getRequestHttpReceivedKoMeterRate() {
		return _requestHttpReceivedKoMeterRate;
	}
	
	public Meter getRequestHttpSendMeter() {
		return _requestHttpSendMeter;
	}
	
	public Meter getRequestHttpSendMeterRate() {
		return _requestHttpSendMeterRate;
	}
	
	public Meter getRequestHttpSendMeterGET() {
		return _requestHttpSendGETMeter;
	}
	public Meter getRequestHttpSendMeterPOST() {
		return _requestHttpSendPOSTMeter;
	}
	public Meter getRequestHttpSendMeterPUT() {
		return _requestHttpSendPUTMeter;
	}
	public Meter getRequestHttpSendMeterDELETE() {
		return _requestHttpSendDELETEMeter;
	}
	
	
	
	

}
