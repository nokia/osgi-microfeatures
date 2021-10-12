// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.metering2.reporter.jmx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alcatel.as.util.metering2.reporter.codahale.MeteringCodahaleRegistry;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;

import alcatel.tess.hometop.gateways.utils.Log;

public class JmxMetricsReporter {
	private final static Log _log = Log.getLogger(JmxMetricsReporter.class);

	private volatile ConcurrentHashMap<String, JmxReporter> _reporters;

	/**
	 * Creates and starts a Codahale Log4j Reporter.
	 */
	protected void start() {
		_log.info("Starting jmx reporter ...");
		_reporters = new ConcurrentHashMap<>();
	}

	
	final void added(MeteringCodahaleRegistry registry, Map<String, ?> props) {
		_log.debug("[JMX REPORTER] ADDED");
		registry.getMeteringRegistries().forEach((domain, reg) -> {
			JmxReporter reporter = JmxReporter.forRegistry(reg).inDomain(domain).build();
			_log.debug("[JMX REPORTER] Registered -> %s", domain);
			_reporters.put(domain,reporter);
			reporter.start();
		});
		
	}
	
	final void removed(MeteringCodahaleRegistry registry, Map<String, ?> props) {
		String monitorableName = (String) props.get(MeteringCodahaleRegistry.MONITORABLE_NAME);
		_log.debug("[JMX REPORTER] REMOVED %s", monitorableName);
		if(_reporters.containsKey(monitorableName)) {
			_reporters.get(monitorableName).stop();
			_reporters.get(monitorableName).close();
			_reporters.remove(monitorableName);
		}
	}
	
	// Called by DM (thread safe).
	final void changed(MeteringCodahaleRegistry registry, Map<String, ?> props) {
		String monitorableName = (String) props.get(MeteringCodahaleRegistry.MONITORABLE_NAME);
		MetricRegistry reg = registry.getMeteringRegistries().get(monitorableName);
		if(_reporters.containsKey(monitorableName)) {
			_log.debug("[JMX REPORTER] CHANGED %s, Already exists, first removing old reporter", monitorableName);
			_reporters.get(monitorableName).stop();
			_reporters.get(monitorableName).close();
			_reporters.remove(monitorableName);
		}
		if (reg == null){
		    _log.debug("[JMX REPORTER] REMOVED %s, removed mbean", monitorableName);
		} else {
		    _log.debug("[JMX REPORTER] CHANGED %s, adding mbean, reg=%s", monitorableName, reg);
		    JmxReporter reporter = JmxReporter.forRegistry(reg).inDomain(monitorableName).build();
		    _reporters.put(monitorableName, reporter);
		    reporter.start();
	        }
	}
	
	
	protected void stop() {
		_log.info("Stopping jmx reporter ...");
		_reporters.values().forEach(reporter -> {
			reporter.stop();
			reporter.close();
		});
		_reporters.clear();
	}
}
