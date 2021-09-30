package com.nokia.as.metering.prometheus.pull;

import java.io.StringWriter;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.SerialExecutor;
import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.alcatel.as.service.metering2.impl.util.MetersReader;
import com.alcatel.as.service.metering2.util.MeteringRegistry;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.config.ConfigHelper;
import com.nokia.as.metering.prometheus.common.CollectorRegistryWrapper;
import com.nokia.as.metering.prometheus.common.Entry;
import com.nokia.as.metering.prometheus.common.ExportedMeter;
import com.nokia.as.metering.prometheus.common.TrackerConfiguration;

import io.prometheus.client.Counter;
import io.prometheus.client.SimpleCollector;

/**
 * Provide metrics to Prometheus in the standard Pull Format
 * 
 * @author cedossan
 *
 */
public abstract class AbstractPullMetersTracker {
	private static final long serialVersionUID = 4331270162472868053L;

	/**
	 * Logger
	 */
	private final static Logger _log = Logger.getLogger(AbstractPullMetersTracker.class);

	
	/**
	 * System configuration.
	 */
	private Dictionary<String, String> _system;

	/**
	 * Meters Tracker Configuration
	 */
	private PullConfiguration _configuration;

	@ServiceDependency(required=true)
	MeteringRegistry _meteringRegistry;
	
	@ServiceDependency(required=true)
	PlatformExecutors _platformExecutor;
	
	/**
	 * Prometheus Registry
	 */
	CollectorRegistryWrapper _registry;

	/**
	 * Filedata Entries with commands (command line to Entry)
	 */
	protected Map<String, Entry> _entries = new HashMap<> ();
	
	/**
	 * Our instance name.
	 */
	private String _groupInstanceName;

	/**
	 * Give the amount of requests (Polling) made by Prometheus
	 */
	private Counter _prometheusRequests;

	/**
	 * Executor
	 */
	private final SerialExecutor _serial = new SerialExecutor();

	/**
	 * Map of unique meter Name to a Prometheus Simple Collector (Gauge, Counter
	 * and others...)
	 */
	Map<String, SimpleCollector<?>> _prometheusMeters = new ConcurrentHashMap<>();

	@ConfigurationDependency(required = true)
	void loadMetersTrackerConf(PullConfiguration conf) {
		boolean alreadyStarted = (_configuration != null);
		_configuration = conf;
		if (alreadyStarted) {
			exportMetrics();
		}
	}
	
	private void exportMetrics() {
		_log.warn("### MetersTracker Config changed");
		exportMetrics(_configuration.getPrometheusMeters(), 
				_configuration.getHistogramDomains(),
				_configuration.getJobDelay());
	}

	@ConfigurationDependency(pid = "system")
	void updated(Dictionary<String, String> system) {
		_system = system;
	}

	/**
	 * Expose Prometheus metrics through HTTP
	 */
	protected String getMetrics(){
		_prometheusRequests.inc();
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
			io.prometheus.client.exporter.common.TextFormat.write004(writer, _registry.getRegistry().metricFamilySamples());
		} catch (Exception e) {
			return e.getMessage();
		}
		return writer.toString();
	}

	/**
	 * Start method
	 */
	@Start
	void start() {
		try {
			_registry = new CollectorRegistryWrapper();
//			_log = _logFactory.getLogger(AbstractPullMetersTracker.class);
			_groupInstanceName = ConfigHelper.getString(_system, ConfigConstants.GROUP_NAME) + "."
					+ ConfigHelper.getString(_system, ConfigConstants.INSTANCE_NAME);

			_prometheusRequests = Counter.build()
					.name(_groupInstanceName.replace(".", "_") + "_prometheus_total_requests")
					.help("Number of Prometheus requests by Pull").create();
			_log.warn(String.format("counter -> %s", _prometheusRequests.get()));
			_registry.register(_prometheusRequests);

			_log.warn("### MetersTracker Starting...");
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			exportMetrics();
		} catch (Throwable e) {
			_log.warn("exception", e);
			Thread.currentThread().setContextClassLoader(null);
		}
	}

	/**
	 * Stop method
	 */
	@Stop
	void stop() {
		_registry = null;
		_log.info("### MetersTracker Stopping....");
	}

	
	protected void exportMetrics(String prometheusMeters, List<String> domains, int jobDelay) {
		_log.debug(String.format("### %s exportMetrics", this));
		_serial.execute(new Runnable() {
			@Override
			public void run() {
				String lines = prometheusMeters;
				List<String> histogramDomains = domains;
				TrackerConfiguration commonAtt = new TrackerConfiguration(_log, _meteringRegistry, _serial, 
						_registry, _groupInstanceName, histogramDomains, jobDelay, _platformExecutor);
				if (lines == null)
			          return;
				_log.debug(String.format("### %s exportMetrics LINES %s", this, lines));
				MetersReader.LineReader<Map<String, Entry>> reader = new MetersReader.LineReader<Map<String, Entry>>() {
					public Map<String, Entry> readLine(String line, Map<String, Entry> entries) {
						Entry newEntry = null;
						Entry o = _entries.remove(line);
						if (o != null) {
							if (_log.isDebugEnabled())
								_log.debug(this + " : keeping entry : " + o);
							entries.put(line, o);
						} else if (line.startsWith("exportMeters")) {
							if (_log.isDebugEnabled())
								_log.debug(this + " : parsing line : " + line);
							newEntry = new ExportedMeter(
									MetersReader.getParam(line, "-m", "-mon", "-monitorable"),
									MetersReader.getParam(line, "-p", "-prefix"), 
									MetersReader.getParams(line, "-mt", "-meter"),
									MetersReader.getParam(line, "-mts", "-meters"),
									MetersReader.getParam(line, "-to", "-a", "-alias"),
									MetersReader.getParam(line, "-ms", "-mons", "-monitorables"),
									MetersReader.getParams(line, "-lb", "-label"),
									MetersReader.getParam(line, "-multiply"),
									MetersReader.getParam(line, "-t", "-type"),
									MetersReader.getParam(line, "-h", "-help"),
									commonAtt);
						}
						if (newEntry != null) {
							entries.put(line, newEntry);
						}
						return entries;
					}
				};
				Map<String, Entry> entries = MetersReader.parse(lines, reader, new HashMap<String, Entry>());
				for (String line : _entries.keySet()) {
					if (entries.get(line) != null)
						continue;
					_entries.get(line).stop();
				}
				_entries = entries;
				for (Entry entry : _entries.values())
					entry.start();
			}
		});
	}
}
