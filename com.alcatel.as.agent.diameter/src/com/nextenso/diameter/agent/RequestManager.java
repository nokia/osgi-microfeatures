package com.nextenso.diameter.agent;

import java.util.Map;

import org.apache.log4j.Logger;

import com.alcatel.as.service.metering.Gauge;
import com.nextenso.diameter.agent.impl.DiameterRequestFacade;
import com.nextenso.proxylet.diameter.DiameterResponse;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;

/**
 * The Request Manager.
 */
public class RequestManager {
	
	private static Meter _sizeMeter, _sizePxMeter, _sizeClientMeter;
	
	public static void createMeters (MeteringService metering, SimpleMonitorable mon) {
		_sizeMeter = mon.createIncrementalMeter(metering, "work.pending", null);
		_sizePxMeter = mon.createIncrementalMeter(metering, "work.pending.proxy", _sizeMeter);
		_sizeClientMeter = mon.createIncrementalMeter(metering, "work.pending.client", _sizeMeter);
	}

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.requestmanager");

	private Map<Integer, DiameterRequestFacade> _requests = Utils.newConcurrentHashMap();

	/**
	 * Constructor for this class.
	 */
	public RequestManager() {
		super();
	}

	public void close() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("close: remove all requests for this=" + this);
		}
		for (DiameterRequestFacade request : _requests.values()) {
			request.cancel(DiameterResponse.UNABLE_TO_DELIVER_CAUSE.ROUTE_CLOSED);
		}
	}

	private void inc (DiameterRequestFacade req, int inc){
		if (req.isClientRequest ()) _sizeClientMeter.inc (inc);
		else _sizePxMeter.inc (inc);
	}

	public DiameterRequestFacade getRequest(int serverHopIdentifier) {
		DiameterRequestFacade res = _requests.get(serverHopIdentifier);
		return res;
	}

	public void removeRequest(DiameterRequestFacade request) {
		int key = Utils.getRequestManagerKey(request);
		if (_requests.remove(key) != null) {
			inc (request, -1);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("removeRequest: after removing the key=" + key + ", manager=" + this);
			}
		}
	}

	public void addRequest(DiameterRequestFacade request) {
		int key = Utils.getRequestManagerKey(request);
		_requests.put(key, request);
		inc (request, 1);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("addRequest: after adding the key=" + key + ", manager=" + this);
		}
	}

	@Override
	public String toString() {
		StringBuilder res = new StringBuilder(super.toString());
		res.append(" \n\tRequest set: [");
		for (int key : _requests.keySet()) {
			res.append(" ").append(key);
		}
		res.append("]");

		return res.toString();
	}

}
