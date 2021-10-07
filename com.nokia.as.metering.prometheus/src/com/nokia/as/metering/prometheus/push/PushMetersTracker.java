package com.nokia.as.metering.prometheus.push;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.felix.dm.annotation.api.Component;
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
import com.nokia.as.metering.prometheus.pull.AbstractPullMetersTracker;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.PushGateway;

@Component
public class PushMetersTracker {
	/**
	 * Logger
	 */
	private final static Logger _log = Logger.getLogger(PushMetersTracker.class);
	
	/**
	 * System configuration.
	 */
	private Dictionary<String, String> _system;

	/**
	 * Meters Tracker Configuration
	 */
	private PushConfiguration _configuration;

	private PushGateway _pushGateway;
	
	private Gauge _gaugePush;
	
	@ServiceDependency(required=true)
	MeteringRegistry _meteringRegistry;
	
	/**
	 * ASR PlatformExecutors service, using to retrieve the queue that is
	 * automatically created for our component. We'll retrieve our queue from
	 * our @Start method.
	 */
	@ServiceDependency(required=true)
	private PlatformExecutors _pfexecs;
	
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

	private int _delayJob;
	
	private PrometheusPushUpdater _updater;
	

	/**
	 * Executor
	 */
	private final SerialExecutor _serial = new SerialExecutor();

	@ConfigurationDependency(required = true)
	void loadMetersTrackerConf(PushConfiguration conf) {
		boolean alreadyStarted = (_configuration != null && _log != null);
		_configuration = conf;
		_delayJob = _configuration.getJobDelay();
		if (alreadyStarted) {
			exportMetrics(_configuration);
		}
	}

	@ConfigurationDependency(pid = "system")
	void updated(Dictionary<String, String> system) {
		_system = system;
	}

	/**
	 * Start method
	 */
	@Start
	void start() {
		try {	
			
			_registry = new CollectorRegistryWrapper();
			_groupInstanceName = ConfigHelper.getString(_system, ConfigConstants.GROUP_NAME) + "."
					+ ConfigHelper.getString(_system, ConfigConstants.INSTANCE_NAME);
			_log.warn("### MetersTracker Starting...");
			if (_configuration != null){
				String connect_adress = _configuration.getPushgatewayAddress();
				int connect_port = _configuration.getPushgatewayPort();
				String full_adress = connect_adress+":"+connect_port;
				_log.debug(String.format("PUSH GATEWAY connection... address -> %s", full_adress));
				_pushGateway = new PushGateway(full_adress);
				_log.debug("### First read of filedata");
				exportMetrics(_configuration);
				
				_gaugePush = Gauge.build()
					     		  .name("my_batch_job_duration_seconds")
					     		  .help("Duration of my batch job in seconds.")
					     		  .create();
				_registry.register(_gaugePush);
				_updater = new PrometheusPushUpdater();
			} else {
				_log.debug("Configuration is not set yet...");
			}
		} catch (Throwable e) {
			_log.warn("exception", e);
		}
	}

	/**
	 * Stop method
	 */
	@Stop
	void stop() {
		_entries.values().forEach(entry -> entry.stop());
		_registry = null;
		_updater.cancel();
		_log.info("### MetersTracker Stopping....");
	}

	/**
	 * Push metrics to the pushgateway
	 */
	private void pushMetrics(){
	    try {
	    	_gaugePush.inc();
			_pushGateway.pushAdd(_registry.getRegistry(), (_configuration != null)? _configuration.getJobName():"CASR");
		} catch (Throwable t) {
			_log.error("Push to Prometheus Gateway failed ",t);
		}
	}
	

	private void exportMetrics(PushConfiguration conf) {
		_serial.execute(new Runnable() {
			@Override
			public void run() {
				String lines = conf.getPrometheusMeters();
				List<String> histoDomains = conf.getHistogramDomains();
				TrackerConfiguration commonAtt = new TrackerConfiguration(_log, _meteringRegistry, _serial, 
						_registry, _groupInstanceName, histoDomains, conf.getJobDelay(), _pfexecs);
				if (lines == null)
			          return;
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
					_log.debug("[MetersReader] stop");
				}
				_entries = entries;
				for (Entry entry : _entries.values())
					entry.start();
			}
		});
	}

	/**
	 * Updater to push metrics at a fixed rate
	 */
	private class PrometheusPushUpdater implements Runnable {

        private final ScheduledFuture<?> _future;
        
        public PrometheusPushUpdater() throws Exception {
        	_future = _pfexecs.getCurrentThreadContext()
        					   .getCurrentExecutor()
        					   .scheduleAtFixedRate(this, 0, _delayJob, TimeUnit.MILLISECONDS);
        	_log.debug(String.format("[PrometheusPushUpdater] period update (each %s milliseconds)", _delayJob));
        }

        @Override
        public void run() {
            try {
            	pushMetrics();
            }
            catch (Exception e) {
                _log.error("Push failed...", e);
            }
        }

        public void cancel() {
            try {
                _future.cancel(false);
            }
            catch (Exception e) {
                _log.error("Push deregistration update failed", e);
            }
        }
    }
}
