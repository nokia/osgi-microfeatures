package com.nokia.as.metering.prometheus.push;

import java.util.List;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

@Config(section = "Meters")
public interface  PushConfiguration {

	public final static int DEF_JOB_DELAY = 1000;
	
	public final static String DEF_JOB_NAME = "CASR";
	
	public final static String DEF_PUSHGATEWAY_ADDRESS = "127.0.0.1";

	public final static String DEF_HISTOGRAM = "[histogram]";
			
	public final static int DEF_PUSHGATEWAY_PORT = 9091;
	
	@IntProperty(min = 1, max = 10000, title = "General job delay (in ms)",
            	 help = "Specifies the delay to refresh the meters of a job in milliseconds.",
            	 required = false, dynamic = false, defval = DEF_JOB_DELAY)
	public static final String JOB_DELAY = "job.delay";
	
	@StringProperty(title = "Job Name that will appear on each metric on Prometheus",
	       	 help = "", required = false, dynamic = false, defval = DEF_JOB_NAME)
		public static final String JOB_NAME = "job.name";
	
	@StringProperty(title = "PushGateway Address",
       	 help = "The tracker will push metrics to that Server Address (it is a Prometheus asset deisgned for push)",
       	 required = false, dynamic = false, defval = DEF_PUSHGATEWAY_ADDRESS)
	public static final String PUSHGATEWAY_ADDRESS = "pushgateway.address";

	@IntProperty(min = 1, max = 10000, title = "PushGateway Port",
       	 help = "The tracker will push metrics to the specified address and at this port",
       	 required = false, dynamic = false, defval = DEF_PUSHGATEWAY_PORT)
	public static final String PUSHGATEWAY_PORT = "pushgateway.port";
	
	/**
	 * Prometheus meters configuration.
	 */
	@FileDataProperty(title = "Prometheus Meters", dynamic = true, required = true, fileData = "prometheusMeters.txt", 
			help = "This property contains the list of meters exported to prometheus")
	public final static String PROMETHEUS_METERS = "prometheus.meters";

	
	@StringProperty(title = "Histogram  domains",defval = DEF_HISTOGRAM, help = "Meters matching domain will be converted to Histograms", dynamic = false, required=false)
	public static final String HISTOGRAM_DOMAINS = "histogram.domains";
	
	
	int getJobDelay();
	
	String getJobName();
	
	String getPushgatewayAddress();

	int getPushgatewayPort();

	String getPrometheusMeters();

	List<String> getHistogramDomains();
}