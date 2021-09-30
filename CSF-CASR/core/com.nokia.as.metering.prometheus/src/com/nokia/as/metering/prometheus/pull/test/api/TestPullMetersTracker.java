package com.nokia.as.metering.prometheus.pull.test.api;

import java.util.List;

import org.apache.felix.dm.annotation.api.Component;

import com.nokia.as.metering.prometheus.pull.AbstractPullMetersTracker;

@Component(provides = TestPullMetersTracker.class)
public class TestPullMetersTracker extends AbstractPullMetersTracker {

	public String getMetrics() {
		return super.getMetrics();
	}

	public void forceExportMetrics(String prometheusMeters, List<String> domains, int jobDelay) {
		exportMetrics(prometheusMeters, domains, jobDelay);
	}
}
