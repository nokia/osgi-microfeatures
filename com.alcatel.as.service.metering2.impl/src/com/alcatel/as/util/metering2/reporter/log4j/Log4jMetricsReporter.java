// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.metering2.reporter.log4j;

import java.util.Dictionary;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel.as.util.metering2.reporter.codahale.MeteringCodahaleRegistry;
import com.alcatel.as.util.metering2.reporter.codahale.SimpleMetricFilter;
import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;
import com.codahale.metrics.Slf4jReporter;

@Config(section = "Metering Service/Log4j Reporter")
public class Log4jMetricsReporter {
	public final static String PID = Log4jMetricsReporter.class.getName();

	/**
	 * Injected, holds a codahale registry which maps all available metering meters
	 * to real codhale meters.
	 */
	private MeteringCodahaleRegistry _meteringRegistry;

	@StringProperty(title = "Filter for monitored services", required = true, defval = "*", dynamic = true, help = "Enter the list of monitored services prefix (comma seprated). Optionally each prefix may"
			+ " contain a semicolon followined by a (blank seprated) list of enabled meter names prefix<p>"
			+ " (example: * or system,ioh or reactor;as.stat.sockets as.stat.cpu)")
	public final static String CNF_FILTER = "meters.log4j.filter";

	@IntProperty(title = "Log4j Reporting Rate", required = true, defval = 10, dynamic = false, help = "Enter the rate of the monitoring reports (in seconds).")
	private final static String CNF_RATE_REPORTS = "meters.log4j.rateReports";

	@StringProperty(title = "Log4j logger name", required = true, defval = "asrmetrics", dynamic = false, help = "Enter the log4j logger name used to report monitorable metrics.")
	private final static String CNF_LOGGER = "meters.log4j.logger";

	/**
	 * Our SL4J reporter.
	 */
	private volatile CopyOnWriteArrayList<Slf4jReporter> _reporters;

	/**
	 * A filter used to avoid displaying all available meters.
	 */
	private final SimpleMetricFilter _filter = new SimpleMetricFilter();

	/**
	 * This is the rate in seconds used to report metrics to log4j.
	 */
	private int _reportRate;

	/**
	 * The log4j logger name used to report monitorable metrics.
	 */
	private String _logger;

	protected synchronized void updated(Dictionary<String, String> conf) {
		if (conf != null) {
			_filter.update(ConfigHelper.getString(conf, CNF_FILTER, ""));
			_reportRate = ConfigHelper.getInt(conf, CNF_RATE_REPORTS, 10);
			_logger = ConfigHelper.getString(conf, CNF_LOGGER, "asrmetrics");
		}
	}

	protected void start() {
		
		_reporters = new CopyOnWriteArrayList<>();
		_meteringRegistry.getMeteringRegistries()
						 .values()
						 .forEach(registry -> {
							 Slf4jReporter reporter =
								 Slf4jReporter.forRegistry(registry)
											  .outputTo(LoggerFactory.getLogger(_logger))
											  .convertRatesTo(TimeUnit.SECONDS)
											  .convertDurationsTo(TimeUnit.NANOSECONDS)
											  .filter(_filter)
											  .build();
							 _reporters.add(reporter);
							 reporter.start(_reportRate, TimeUnit.SECONDS);
						 });
	}

	protected void stop() {
		_reporters.forEach(reporter -> reporter.stop());
	}
}
