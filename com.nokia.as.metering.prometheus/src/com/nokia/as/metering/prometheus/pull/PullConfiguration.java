// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.metering.prometheus.pull;
import java.util.List;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;


@Config(section = "Meters")
public interface  PullConfiguration {

	public final static int DEF_JOB_DELAY = 500;
	
	public final static String DEF_HISTOGRAM = "[histogram]";
	
	@IntProperty(min = 1, max = 10000, title = "General job delay (in ms)",
            	 help = "Specifies the delay to refresh the meters of a job in milliseconds.",
            	 required = false, dynamic = false, defval = DEF_JOB_DELAY)
	public static final String JOB_DELAY = "job.delay";

	/**
	 * Prometheus meters configuration.
	 */
	@FileDataProperty(title="Prometheus Meters", dynamic=true, required=true, fileData="prometheusMeters.txt",
	  help="This property contains the list of meters exported to prometheus")
	public final static String PROMETHEUS_METERS = "prometheus.meters";
	  
	@StringProperty(title = "Histogram  domains",defval = DEF_HISTOGRAM, help = "Meters matching domain will be converted to Histograms", dynamic = false, required=false)
	public static final String HISTOGRAM_DOMAINS = "histogram.domains";
	  
	int getJobDelay();
	  
	String getPrometheusMeters();
	  
	List<String> getHistogramDomains();
}