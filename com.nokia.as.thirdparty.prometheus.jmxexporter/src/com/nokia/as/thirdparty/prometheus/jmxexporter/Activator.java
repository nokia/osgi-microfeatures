// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.thirdparty.prometheus.jmxexporter;

import java.io.StringWriter;

import javax.management.MalformedObjectNameException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;

import io.prometheus.jmx.shaded.io.prometheus.client.CollectorRegistry;
import io.prometheus.jmx.shaded.io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.jmx.shaded.io.prometheus.jmx.JmxCollector;

@Component(provides = Activator.class)
public class Activator{

	private static final long serialVersionUID = 1L;

	final static Logger _log = Logger.getLogger(Activator.class);
	
	Configuration _configuration;
	
	@ConfigurationDependency
	void updated(Configuration conf){
		if (conf != null) {
			_log.debug(conf.getConfig());
			_configuration = conf;
			initializeJMXCollector();
		}
	}

	@Start
	void start() {
		_log.debug("STARTED");
	}
	
	void initializeJMXCollector(){
		try {
			new JmxCollector(_configuration.getConfig()).register();
			DefaultExports.initialize();
			if (_log != null) _log.debug("Configuration Updated -> "+ System.lineSeparator() + _configuration.getConfig());
		} catch (MalformedObjectNameException e) {
			_log.error(e);
		}
	}
	
	
	/**
	 * Expose Prometheus metrics through HTTP
	 */
	public String getMetrics(){
		return formatMetrics();
	}
	
	/**
	 * Format metrics in order to expose metrics with Prometheus format
	 * 
	 * @return formated Metrics
	 */
	private String formatMetrics() {
		StringWriter writer = new StringWriter();
		try {
			io.prometheus.jmx.shaded.io.prometheus.client.exporter.common.TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
		} catch (Exception e) {
			return e.getMessage();
		}
		return writer.toString();
	}
}
