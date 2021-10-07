package com.nokia.as.gpto.scenarii.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeterListener;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.ValueSupplier;
import com.alcatel.as.service.metering2.util.Meters;
import com.nokia.as.gpto.common.msg.api.GPTOMonitorable;
import com.nokia.as.gpto.common.msg.api.AgentRegistration.MeterType;

public class HttpClientMetering {

	protected MeteringService _metering;
	protected int _executionId;
	private GPTOMonitorable monitorable;
	/*Request Http Received Meter*/
	protected Meter _requestHttpReceivedMeter;
	protected Meter _requestHttpReceivedOkMeter , _requestHttpReceivedKoMeter;
	protected Meter _requestHttpReceivedOkMeterRate , _requestHttpReceivedKoMeterRate;
	protected Meter _totalResponseLatency;
	protected Meter _lastResponseLatency;
	protected Meter _minResponseLatency;
	protected Meter _maxResponseLatency;
	
	/*Request Http Send Meter*/	
	protected Meter _requestHttpSendMeter;
	protected Meter _requestHttpSendMeterRate;
	protected Meter _requestHttpSendGETMeter ,_requestHttpSendPOSTMeter, _requestHttpSendDELETEMeter, _requestHttpSendPUTMeter;
	
	protected Map<Integer, Meter> _responseHttpCodeMeter;
	
	public HttpClientMetering(MeteringService metering, GPTOMonitorable monitorable, int executionId) {
		_metering = metering;
		this.monitorable = monitorable;
		_executionId = executionId;
	}

	public Meter createIncrementalMeter(String name, Meter parent, MeterType type) {
		return monitorable.createIncrementalMeter(_metering, name, parent, type);
		
	}

	public Meter createAbsoluteMeter(String name, MeterType type) {
		return monitorable.createAbsoluteMeter(_metering, name, type);
	}

	public Meter createValueSuppliedMeter(String name, ValueSupplier supplier, MeterType type) {
		return monitorable.createValueSuppliedMeter(_metering, name, supplier, type);
	}
	
	public Meter createIncrementalMeter(String name, Meter parent) {
		return monitorable.createIncrementalMeter(_metering, name, parent);
		
	}

	public Meter createAbsoluteMeter(String name) {
		return monitorable.createAbsoluteMeter(_metering, name);
	}

	public Meter createValueSuppliedMeter(String name, ValueSupplier supplier) {
		return monitorable.createValueSuppliedMeter(_metering, name, supplier);
	}

	private static class MinValueListener implements MeterListener<Meter> {
	    private long _minValue;
	    private boolean started = false;
	    private MinValueListener() {
	    }
	    
	    public Meter updated(Meter meter, Meter store) {
	    	long value = meter.getValue();
	    	if (!started) {
	    		store.set(_minValue = value);
	    		started = true;
	    	}
		    if (value < _minValue && started)
		      store.set(_minValue = value);
		    return store;
	    }
	  }
	
	public Meter createMinValueMeter(Meter target) {
		Meter store = _metering.createAbsoluteMeter(new StringBuilder("loader.latency").append(".min").toString());
	    store.attach (target.startJob(new MinValueListener(), store, null));
	    monitorable.addMeter(MeterType.MIN, store);
	    return store;
	}
	
	public Meter createRateMeter(Meter target) {
		Meter rate = com.alcatel.as.service.metering2.util.Meters.createRateMeter(_metering, target, 1000);
		monitorable.addMeter(rate);
		return rate;
	}

	public void stopRateMeter(Meter meter) {
		com.alcatel.as.service.metering2.util.Meters.stopRateMeter(meter);
	}

	public void init() {
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
		_lastResponseLatency = createAbsoluteMeter("loader.histogram.latency", MeterType.HISTOGRAM);
		_minResponseLatency = createMinValueMeter(_lastResponseLatency);
		_maxResponseLatency = Meters.createMaxValueMeter(_metering, "loader.latency.max", _lastResponseLatency);
		monitorable.addMeter(MeterType.MAX, _maxResponseLatency);
		_totalResponseLatency = createIncrementalMeter("loader.latency.total", null);
		_responseHttpCodeMeter = new ConcurrentHashMap<>();
		Integer[] statuses = new Integer[]{100, 101, 200, 201, 202, 203, 204, 205, 206, 
										 300, 301, 302, 303, 304, 305, 306, 307, 308, 
										 310, 400, 401, 402, 403, 404, 405, 406, 407, 
										 408, 409, 410, 411, 412, 413, 414, 415, 416, 
										 417, 426, 428, 429, 431, 500, 501, 502, 503, 
										 504, 505, 506, 509, 510, 520, 999};
	    Arrays.asList(statuses).forEach(status -> 
	    			_responseHttpCodeMeter.put(status, createIncrementalMeter(_executionId+".response.status."+status, null)));
	}

	/*@Override
	public void stop() {
		stopRateMeter(_readReqsRateMeter);
		super.stop();
	}*/

	public Meter getRequestHttpReceivedMeter() {
		return _requestHttpReceivedMeter;
	}
	
	/**
	 * Return the meter of the given status code. If the status code is unknown, a meter with status code 999 will be given
	 * @param statusCode
	 * @return
	 */
	public Meter getResponseHttpCodeMeter(int statusCode) {
		return _responseHttpCodeMeter.containsKey(statusCode)?_responseHttpCodeMeter.get(statusCode):_responseHttpCodeMeter.get(999);
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
	
	public Meter getTotalResponseLatency() {
		return _totalResponseLatency;
	}
	public Meter getLastResponseLatency() {
		return _lastResponseLatency;
	}
	public Meter getMinResponseLatency() {
		return _minResponseLatency;
	}
	public Meter getMaxResponseLatency() {
		return _maxResponseLatency;
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
