package com.alcatel.as.util.metering2.reporter.ganglia;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel.as.util.metering2.reporter.codahale.MeteringCodahaleRegistry;
import com.alcatel.as.util.metering2.reporter.codahale.SimpleMetricFilter;
import com.alcatel_lucent.as.management.annotation.config.AddressProperty;
import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;
import com.codahale.metrics.ganglia.GangliaReporter;

import alcatel.tess.hometop.gateways.utils.Log;
import info.ganglia.gmetric4j.gmetric.GMetric;
import info.ganglia.gmetric4j.gmetric.GMetric.UDPAddressingMode;

@Config(section = "Metering Service/Ganglia Reporter")
public class GangliaMetricsReporter {
  private final static Log _log = Log.getLogger(GangliaMetricsReporter.class);
  
  public final static String PID = GangliaMetricsReporter.class.getName();
  public final static String PID_SYSTEM = "system";
  
  @StringProperty(title = "Filter for monitored services", required = true, defval = "*", dynamic = true,
                  help = "Enter the list of monitored services prefix (comma seprated). Optionally each prefix may"
                      + " contain a semicolon followined by a (blank seprated) list of enabled meter names prefix<p>"
                      + " (example: * or system,ioh or reactor;as.stat.sockets as.stat.cpu)")
  public final static String CNF_FILTER = "meters.ganglia.filter";
  
  @AddressProperty(title = "Ganglia Addresses", required = true, defval = "127.0.0.1:8649", dynamic = false,
                   help = "Enter the list of Ganglia server address:port (comma separated)")
  public final static String CNF_GANGLIA_ADDRS = "meters.ganglia.addrs";
  
  @IntProperty(title = "Ganglia Reporting Rate", required = true, defval = 10, dynamic = false,
      help = "Enter the reporting rate used to periodically report meters to ganglia")
  public final static String CNF_GANGLIA_REPORT_RATE = "meters.ganglia.reportRate";

  /**
   * A filter used to avoid displaying all available meters.
   */
  private final SimpleMetricFilter _filter = new SimpleMetricFilter();
  
  /**
   * List of Ganglia server ipaddr:port
   */
  private String _gangliaAddrs;
  
  /**
   * The actual Ganglia Metrics Reporters.
   */
  private List<GangliaReporter> _reporters;
    
  /**
   * This is the rate in seconds used to report metrics to ganglia. 
   */
  private int _reportRate;
  
  /**
   * Injected, holds a codahale registry which maps all available metering meters to real codhale meters.
   */
  private MeteringCodahaleRegistry _meteringRegistry;

  /**
   * We handle two different conf PIDS: our own PID + the "system" pid.
   */
  protected void updated(Dictionary<String, String> conf) {
    if (conf != null) {
      String pid = conf.get("service.pid");
      if (pid.equals(PID)) {
        _filter.update(ConfigHelper.getString(conf, CNF_FILTER, null));
        _gangliaAddrs = ConfigHelper.getString(conf, CNF_GANGLIA_ADDRS);
        _reportRate = ConfigHelper.getInt(conf, CNF_GANGLIA_REPORT_RATE, 10);
        _log.info("Configured Ganglia addresses: addrs=%s", _gangliaAddrs);
      }
    }
  }
  
  protected void start() {
    _log.info("Starting ganglia reporter ...");
    try {
    	_reporters = new ArrayList<GangliaReporter> ();
    	for (String ipsports:_gangliaAddrs.split(",")) {
    		String[] v = ipsports.split(":");
    		GMetric ganglia = new GMetric(v[0], Integer.parseInt(v[1]), UDPAddressingMode.MULTICAST, 1);
    		// TODO tests with new Codahale metricRegistry
    		_meteringRegistry.getMeteringRegistries().forEach((monName, registry) -> {
    			GangliaReporter r = GangliaReporter
        				.forRegistry(registry)
        				.convertRatesTo(TimeUnit.SECONDS)
        				.convertDurationsTo(TimeUnit.MILLISECONDS).build(ganglia);
        		_reporters.add(r);
        		r.start(_reportRate, TimeUnit.SECONDS);
    		});
    	}
    } catch (Throwable t) {
      _log.error("Could not start ganglia reporters", t);
    }
  }
  
  protected void stop() {
    _log.info("Stopping ganglia reporters ...");
    for (GangliaReporter r : _reporters) {
    	r.stop();
    }
  }
  
}
