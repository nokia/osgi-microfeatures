// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.metering.prometheus.common;

import java.util.List;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.SerialExecutor;
import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.metering2.util.MeteringRegistry;

public class TrackerConfiguration {
	private Logger log;
	
	private MeteringRegistry meteringRegistry;
	
	private SerialExecutor executor;
	
	private CollectorRegistryWrapper collectorRegistry;

	private String instanceName;
	
	private List<String> histogramDomains;
	
	private PlatformExecutors platformExecutor;
	
	private int jobDelay;
	
	public TrackerConfiguration(Logger log, MeteringRegistry meteringRegistry, SerialExecutor executor,
			CollectorRegistryWrapper collectorRegistry, String instanceName, List<String> histogramDomains,
			int jobDelay, PlatformExecutors pf) {
		super();
		this.log = log;
		this.meteringRegistry = meteringRegistry;
		this.executor = executor;
		this.collectorRegistry = collectorRegistry;
		this.instanceName = instanceName;
		this.histogramDomains = histogramDomains;
		this.jobDelay = jobDelay;
		platformExecutor = pf;
	}

	public Logger getLog() {
		return log;
	}

	public MeteringRegistry getMeteringRegistry() {
		return meteringRegistry;
	}

	public SerialExecutor getExecutor() {
		return executor;
	}

	public CollectorRegistryWrapper getCollectorRegistry() {
		return collectorRegistry;
	}
	
	public String getInstanceName(){
		return instanceName;
	}

	public List<String> getHistogramDomains() {
		return histogramDomains;
	}

	public PlatformExecutors getPlatformExecutors() {
		return platformExecutor;
	}
	
	public int getJobDelay() {
		return jobDelay;
	}
}
