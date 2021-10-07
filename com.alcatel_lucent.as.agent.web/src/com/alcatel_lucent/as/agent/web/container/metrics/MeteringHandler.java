package com.alcatel_lucent.as.agent.web.container.metrics;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.util.MuxHandlerMeters;

public class MeteringHandler extends AbstractHandler {
	
	private SimpleMonitorable parentMonitorable;
	private HttpMeters parentMeters;
	private MeteringService meteringService;
		
	public MeteringHandler(MeteringService meteringService) {
		parentMonitorable = new SimpleMonitorable("agent.web", "Aggregated Metrics for Web Agent");
		parentMeters = new HttpMeters(parentMonitorable, meteringService);
		parentMeters.init();
		this.meteringService = meteringService;
	}
	
	public void registerMuxCnxMonitorable(MuxConnection cnx, SimpleMonitorable mon) {
		HttpMeters meters = new HttpMeters(mon, meteringService);
		meters.init();
		mon.updated();
		cnx.attach(meters);
	}
	
	public void unregisterMuxCnxMonitorable(MuxConnection cnx) {	
		HttpMeters meters = cnx.attachment();
		if(meters != null) {
			meters.stop();
		}
	}
	
	@Override
	public void handle(String arg0, Request arg1, HttpServletRequest arg2, HttpServletResponse arg3)
			throws IOException, ServletException {
		MuxConnection muxCnx = (MuxConnection) arg1.getHttpChannel().getEndPoint().getTransport();
		
		HttpMeters meters = muxCnx.attachment();
		
		if(meters != null) {
			if(arg1 != null) {
				meters.getReadReqMeter(arg1.getMethod()).inc(1);
				if(arg1.getResponse() != null) {
					meters.getWriteRespMeter(arg1.getResponse().getStatus()).inc(1);
				}
			}
		}
		if(arg1 != null) {
			parentMeters.getReadReqMeter(arg1.getMethod()).inc(1);
			if(arg1.getResponse() != null) {
				parentMeters.getWriteRespMeter(arg1.getResponse().getStatus()).inc(1);
			}
		}
	}
	
	public SimpleMonitorable getParentMonitorable() {
		return parentMonitorable;
	}
}
