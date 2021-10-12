// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.agent.metering;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.SerialExecutor;
import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeterListener;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.metering2.MonitoringJob;
import com.alcatel.as.service.metering2.util.MeteringRegistry;
import com.nokia.as.gpto.agent.impl.GPTOAgent;
import com.nokia.as.gpto.common.msg.api.AgentRegistration;
import com.nokia.as.gpto.common.msg.api.AgentRegistration.MeterType;
import com.nokia.as.gpto.common.msg.api.GPTOMonitorable;

public class AgentMetricExporter extends MeteringRegistry.MetersTracker {
	
	private final String GPTO_MONITORABLES = "as.gpto.*";
	
	MeteringRegistry meteringRegistry;
	
	private final SerialExecutor serial = new SerialExecutor();
	
	private Object tracking;
	
	private volatile Map<String, AgentRegistration> metricRegistry;
	
	private static Logger log = Logger.getLogger(GPTOAgent.class);
	
	public AgentMetricExporter(MeteringRegistry meteringRegistry) {
		super ("*", false, true, false);
		this.meteringRegistry = meteringRegistry;
		metricRegistry = new ConcurrentHashMap<>();
	}

	public void start() {
		tracking = meteringRegistry.trackMonitorables(GPTO_MONITORABLES, this, serial);
	}
	
	public void stop() {
		meteringRegistry.stopTracking (tracking, null);
		
	}
	
	@Override
	public void addedMeter(Monitorable monitorable, Meter meter) {
		super.addedMeter(monitorable, meter);
		String uniqueName = AgentRegistration.generateUniqueName(monitorable.getName(), meter.getName());
		MeterType type = MeterType.GAUGE;
		if (monitorable instanceof GPTOMonitorable) {
			type  = ((GPTOMonitorable) monitorable).getMeterType(meter.getName());
			log.debug("[AddedMeter] analyzing "+ monitorable.getName()+"/"+meter.getName()+" with type="+type);
		}
		
		AgentRegistration reg = initializeMeter(uniqueName, monitorable, meter, type);
		if (reg != null) {
			metricRegistry.put(uniqueName, reg);
		} else {
			log.debug("Unable to initialize "+ uniqueName);
		}
		
	}

	@Override
	public void addedMonitorable(Monitorable monitorable) {
		super.addedMonitorable(monitorable);
		monitorable.getMeters()
		 		   .values()
		 		   .forEach(m -> addedMeter(monitorable, m));
	}
	
	@Override
	public void removedMeter(Monitorable monitorable, Meter meter) {
		super.removedMeter(monitorable, meter);
		String uniqueName = AgentRegistration.generateUniqueName(monitorable.getName(), meter.getName());
		metricRegistry.remove(uniqueName);
	}
	
	@Override
	public void removedMonitorable(Monitorable monitorable, List<Meter> meters) {
		super.removedMonitorable(monitorable, meters);
		monitorable.getMeters()
		   .values()
		   .forEach(m -> removedMeter(monitorable, m));
	}
	
	private AgentRegistration initializeMeter(String uniqueName, final Monitorable mon, final Meter meter, final MeterType type) throws IllegalArgumentException {
		try {
	
		MonitoringJob job;
		AgentRegistration reg = new AgentRegistration(uniqueName, type);
		MeterListener<Void> histogramListener = new MeterListener<Void>() {
			public Void updated(Meter m, Void arg1) {
				reg.addValue(m.getValue());
				return null;
			};
		};
		
		
		MeterListener<Void> gaugeListener = new MeterListener<Void>() {
			public Void updated(Meter m, Void arg1) {
				reg.setValue(m.getValue());
				return null;
			};
		};
		
		
		switch (meter.getType()) {
		case INCREMENTAL:
		case ABSOLUTE:
			job = meter.startJob(isHistogram(mon, meter)?histogramListener:gaugeListener, null, null);
			if (job == null) return null;
			else {
				return reg;
			}
		case SUPPLIED:
			job = meter.startScheduledJob(isHistogram(mon, meter)?histogramListener:gaugeListener, null, null, 1000, 0);
			if (job == null) return null;
			else {
				return reg;
			}

		default:
			return null;
		}
		} catch (Throwable t){
			log.error("[initializeMeter] ERROR", t);
		}
		return null;
	}

	private boolean isHistogram(Monitorable mon, Meter meter) {
		if (mon instanceof GPTOMonitorable) {
			return ((GPTOMonitorable) mon).getMeterType(meter.getName()).equals(MeterType.HISTOGRAM);
		} else {
			return false;
		}
	}

	public Map<String, AgentRegistration> getMetricRegistry() {
		return metricRegistry;
	}
	
	
}