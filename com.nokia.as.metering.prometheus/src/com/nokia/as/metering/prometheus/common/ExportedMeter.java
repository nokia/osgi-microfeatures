// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.metering.prometheus.common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.SerialExecutor;
import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.Meter.Type;
import com.alcatel.as.service.metering2.MeterListener;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.metering2.MonitoringJob;
import com.alcatel.as.service.metering2.util.MeteringRegistry;
import com.alcatel.as.service.metering2.util.Meters;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.SimpleCollector;
import io.prometheus.client.Summary;


public class ExportedMeter extends MeteringRegistry.MetersTracker implements Entry {
	
	/* wew, that's a lot of attributes... */
	boolean _started = false;
	String _monName, _mtsPattern, _msPattern, _alias, _prefix;
	boolean _useMtsPattern, _useMsPattern, _isUnique, _useLabels, _useLabelPattern;
	List<String> _metersNames;
	List<String> _labels;
	List<String> _histogramDomains;
	Double _multiply;
	Object _tracking;
	Map<Meter, LocalRegistration> _registrations;
	Pattern _labelPattern;
	LinkedList<String> _labelPatternNames;
	String _optionalTypeOverride; //can be null
	String _help;
	
	/* Common */
	Logger _log;
	MeteringRegistry _meteringRegistry;
	SerialExecutor _serial;
	CollectorRegistryWrapper _registry;
	String _groupInstanceName;
	PlatformExecutors _pf;
	int jobDelay;
	private String XMTNAME;
	
	public ExportedMeter(final String monitorable, 
						 final String prefix, 
						 final List<String> metersNames, 
						 final String metersPattern, 
						 final String alias, 
						 final String monitorablesPattern, 
						 final List<String> labels,
						 final String multiply,
						 final String optionalTypeOverride,
						 final String help,
						 final TrackerConfiguration common) {
		super (null, false, true, false);
		_log = common.getLog();
		_histogramDomains = common.getHistogramDomains();
		_monName = monitorable;
		_mtsPattern = metersPattern;
		_msPattern = monitorablesPattern;
		_alias = alias;
		_prefix = prefix;
		_help = help;
		_metersNames = (metersNames == null) ? new ArrayList<>() : metersNames;
		_registrations = new ConcurrentHashMap<>();
		_isUnique = metersPattern == null && metersNames.size() == 1;
		_labels = labels;
		try{
			_multiply = (multiply != null)?Double.parseDouble(multiply):null;
		} catch(NullPointerException e){
			_log.error(String.format("ExportMeter line: \"multiply\" has to be specified, it cannot be null"));
			_multiply = null;
		} catch(NumberFormatException e){
			_log.error(String.format("ExportMeter line: \"multiply\" value cannot be parsed to Double, assuming null"));
			_multiply = null;
		} 
		_meteringRegistry = common.getMeteringRegistry();
		_serial = common.getExecutor();
		_registry = common.getCollectorRegistry();
		_groupInstanceName = common.getInstanceName();
		_pf = common.getPlatformExecutors();
		jobDelay = common.getJobDelay();
		_useLabels = _labels != null && _labels.size() > 0;
		_useMtsPattern = _mtsPattern != null;
		_useMsPattern = _msPattern != null;
		_useLabelPattern = (_useMtsPattern && _mtsPattern.contains("{") && _mtsPattern.contains("}"));
		_labelPatternNames = new LinkedList<>();
		_optionalTypeOverride = optionalTypeOverride;
		// Check arguments, stop if illegal arguments
		checkArguments();
		// Compute meter Pattern if existing
		_log.debug(String.format("CONSTRUCTOR -> useMts %s useLblMts %s", _useMtsPattern, _useLabelPattern));
		if (_useLabelPattern){
			// Extract labels with format "{labelName}"
			List<String> labelsTmp = getAllMatches(_mtsPattern, "\\{(.*?)\\}");
			//replace each label {labelName} to (\w+) in order to match added meters with the new meterPatter,
			for(String labelWithAccolade : labelsTmp){
				_mtsPattern = _mtsPattern.replace(labelWithAccolade, "(\\w+)");
		    }
			_log.debug(String.format("CONSTRUCTOR -> mtsPattern %s labelsTMP ->", _mtsPattern, labelsTmp));
			// Escape special characters and add ^regex$ to match exactly what we're tracking
			_mtsPattern = _mtsPattern.replace(".", "\\.").replace(":", "\\:").replace("*", "\\*");
			_mtsPattern = "^".concat(_mtsPattern).concat("$");
			List<String> labelNames = labelsTmp.stream()
					  .map(label -> label.replace("{", "").replace("}", ""))
					  .collect(Collectors.toList());

			_log.debug(String.format("CONSTRUCTOR -> labelnames= %s", labelNames));
			// Store labelNames
			_labelPatternNames = new LinkedList<>(labelNames);
		}

		_log.debug(String.format("[ExportedMeter CONSTRUCTOR] labels = %s or %s", _labels, _labelPatternNames));
	}

	@Override
	public void start() {
		if (_started)
			return;
		if (_log.isDebugEnabled())
			_log.debug(this + " : start");
		_started = true;
		if (_useMsPattern){
			_tracking = _meteringRegistry.trackMonitorables(_msPattern, this, _serial);
		} else {
			_tracking = _meteringRegistry.trackMonitorable(_monName, this, _serial);
		}
	}

	@Override
	public void stop() {
		if (_log.isDebugEnabled ()) _log.debug (this + " : stop");
	    _log.debug(String.format("STOP ExportedMeter %s %s", _metersNames, _mtsPattern));
		_meteringRegistry.stopTracking (_tracking, null);
	    _registrations.values().forEach(this::stop);
	}
	
	private void stop(final LocalRegistration reg){
		if (reg == null) {
			_log.debug("Can't stop current registration (is null)");
			return;
		}
		if(reg.getLabelValues() == null)
			_registry.unregister(reg.getMeterName());
		else
			_registry.unregister(reg.getMeterName(), reg.getLabelValues());
	}
	
	@Override
	public void addedMonitorable(final Monitorable monitorable) {
		super.addedMonitorable(monitorable);
	}
	
	@Override
	public void addedMeter(final Monitorable monitorable, final Meter meter) {
		try {
			super.addedMeter(monitorable, meter);
			final String meterName = getMeterName(_prefix, _alias, meter.getName(), monitorable.getName());
			XMTNAME = meterName;
			LocalRegistration reg = null;
			if (_useMtsPattern){
				if (_useLabelPattern) {
					final Pattern pattern = Pattern.compile(_mtsPattern);
					final Matcher matcher = pattern.matcher(meter.getName());
					final LinkedList<String> labelValues = new LinkedList<>();
					if (matcher.find()) {
//						_log.debug("AddedMeter meterName = %s",meter.getName());
						for (int j = 1; j <= matcher.groupCount(); j++) {
							labelValues.add(matcher.group(j));
						}
						
						reg = initializeMeter(meterName, _prefix, _labelPatternNames, 
								labelValues, monitorable, meter, _help);
					}
				} else{
					if (Meters.matches(meter, _mtsPattern)){
						_log.debug(String.format("AddedMeter meterName = %s",meter.getName()));
						if (_useLabels){
							final KeyValueLabels keyValueLabels = new KeyValueLabels(_labels);
							reg = initializeMeter(meterName, _prefix, keyValueLabels.getKeys(), 
									keyValueLabels.getValues(), monitorable, meter, _help);
						} else {
							reg = initializeMeter(meterName, _prefix, null, 
									null, monitorable, meter, _help);
						}
					}
				}
				
			} else {
				if (_metersNames.contains(meter.getName())){
					_log.debug(String.format("AddedMeter meterName = %s",meter.getName()));
					if (_useLabels){
						final KeyValueLabels keyValueLabels = new KeyValueLabels(_labels);
						reg = initializeMeter(meterName, _prefix, keyValueLabels.getKeys(), 
								keyValueLabels.getValues(), monitorable, meter, _help);
						if (reg != null) _log.debug(String.format("[AddedMeter] reg Name=%s, labels=%s", reg.meterName, reg.labelValues));
					} else {
						reg = initializeMeter(meterName, _prefix, null, 
								null, monitorable, meter, _help);
					}
				}
			}
			if (reg != null) {
//				_log.debug("[AddedMeter] reg Name=%s", reg.meterName);
				_registrations.put(meter, reg);
			}
		} catch (final Exception e){
			_log.error(String.format("ADDED METER FAILED for %s",e,meter.getName()));
		}
	}
	
	@Override
	public void removedMeter(final Monitorable monitorable, final Meter meter) {
		super.removedMeter(monitorable, meter);
		_log.debug(String.format("Meter %s removed...", meter.getName()));
		final LocalRegistration reg = _registrations.get(meter);
		stop(reg);
		_registrations.remove(meter);
	}
	
	@Override
	public void removedMonitorable(final Monitorable monitorable, final List<Meter> meters) {
		super.removedMonitorable(monitorable, meters);
		_log.debug(String.format("Monitorable %s removed...", monitorable.getName()));
		meters.forEach(meter -> removedMeter(monitorable, meter));
	}
	
	
	/**
	 * register all meters to the prometheus registry
	 * Example of command : exportMeters -mt read.req.GET -alias read_req -label method=GET
	 * @param mon
	 * @param prefix
	 * @param meter
	 */
	private LocalRegistration initializeMeter(final String meterName, final String prefix, final LinkedList<String> labelNames, 
			final LinkedList<String> labelValues, final Monitorable mon, final Meter meter, final String help) throws IllegalArgumentException {
		try {
			final PrometheusMeterType type = _optionalTypeOverride != null ? 
					PrometheusMeterType.valueOf(_optionalTypeOverride) : getMeterType(meter);
			MonitoringJob job = null;
			KeyValueLabels orderedLabels = null;
			final SimpleCollector<?> prometheusMetric;
			LinkedList<String> ultKeys = null;
			LinkedList<String> ultValues = null;
			PrometheusRegistration reg;
			MeterListener<?> listener = null;
			final boolean useLabels = (labelNames != null && labelValues != null);
			// Check if meter is registrable and present on registry
			// if reg is null, then, no registration has been found
			if (_registry.isRegistrable(meterName, labelNames, labelValues))
				reg = _registry.getRegistration(meterName, labelNames, labelValues);
			else
				return null;
			
			// if _registry does not contain any meter with <meterName> as name, then create it
			if (reg == null && !_registry.contains(meterName)) {
				
				prometheusMetric = createPrometheusMetric(type, useLabels, meterName, labelNames, _help);
				if (useLabels) ultKeys = labelNames;
			} 
			// Else, if the registry contain such a meter, retrieve it and sort passed labels
			else if (reg != null){
				prometheusMetric = reg.getPrometheusMeter();
				orderedLabels = reg.getOrderedLabels(new KeyValueLabels(labelNames, labelValues));
			} else return null;
					
			// create array of labels if labels are used...
			// Then create appropriate listener
			final String[] labelGet;
			if (useLabels){
				if(reg == null) {
					final int size = labelValues.size();
					ultValues = labelValues;
					labelGet = labelValues.toArray(new String[size]);
				} else {
					final LinkedList<String> ordered = orderedLabels.getValues();
					ultValues = ordered;
					labelGet = ordered.toArray(new String[ordered.size()]);
				}
			} else {
				labelGet = null;
			}
			
			listener = getValueListener(type, prometheusMetric, labelGet);
			
//			job = meter.getType() == Type.SUPPLIED ? 
//					meter.startScheduledJob(listener, null, _pf.getProcessingThreadPoolExecutor(meterName), 1000, 0) : 
//					meter.startScheduledJob(listener, null, _pf.getProcessingThreadPoolExecutor(meterName), 1000, 0);
			job = meter.startScheduledJob(listener, null, _pf.getProcessingThreadPoolExecutor(meterName), jobDelay, 0);
			if (job == null) return null;
			else 	if (reg == null ) {
				reg = new PrometheusRegistration(meterName,job, prometheusMetric, ultKeys, ultValues);
				_registry.register(meterName, reg);
				return new LocalRegistration(meterName, ultValues);
			} else {
				reg.setJob(ultValues, job);
				return new LocalRegistration(meterName, ultValues);
			}
		} catch(final ClassCastException e) {
			_log.error("[initializeMeter] Class cast error with "+ meter.getName() + ". reasons: meterPattern has tracked different ASR meter types, please use -t <METERTYPE> to enforce the type", e);
		} catch (final Exception e){
			_log.error("[initializeMeter] ERROR with "+ meter.getName(), e);
		}
		return null;
	}
	private MeterListener<?> getValueListener(final PrometheusMeterType type, final SimpleCollector<?> prometheusMetric, final String[] labels) {
		switch(type) {
		case GAUGE:
			return new GaugeLabelListener(prometheusMetric, labels);
		case HISTOGRAM:
			return new HistogramLabelListener(prometheusMetric, labels);
		case COUNTER:
			return new CounterListener(prometheusMetric, labels);
		default:
			throw new RuntimeException("unknown type " + type);
		}
	}

	/**
	 * Create a Prometheus Metric according to the given meter from ASR metering API
	 * @param type
	 * @param useLabels
	 * @param meterName
	 * @param labelNames
	 * @return an Histogram (no buckets defined in advance), a simple Gauge or a Counter
	 */
	private SimpleCollector<?> createPrometheusMetric(final PrometheusMeterType type, final boolean useLabels, final String meterName,
			final LinkedList<String> labelNames, final String help) {
		SimpleCollector<?> metric;
		
		switch(type) {
		case HISTOGRAM:
			final io.prometheus.client.Summary.Builder summaryBuilder = buildSummary(meterName, useLabels ? labelNames : null, useLabels);
			if(_help != null) {
				summaryBuilder.help(help);
			}
			metric = summaryBuilder.create();
			break;
		case GAUGE:
			final io.prometheus.client.Gauge.Builder gaugeBuilder = Gauge.build(meterName, meterName);
			if(useLabels) {
				gaugeBuilder.labelNames(labelNames.toArray(new String[labelNames.size()]));
			}
			if(_help != null) {
				gaugeBuilder.help(help);
			}
			metric = gaugeBuilder.create();
			break;
		case COUNTER:
			final io.prometheus.client.Counter.Builder counterBuilder = Counter.build(meterName, meterName);

			if(useLabels) {
				counterBuilder.labelNames(labelNames.toArray(new String[labelNames.size()]));
			}
			if(_help != null) {
				counterBuilder.help(help);
			}
			metric = counterBuilder.create();
			break;
		default: throw new RuntimeException("Unknown meter type " + type);
		}

		return metric;
	}

	/**
	 * Get all elements from text matching with the passed regex
	 * @param text
	 * @param regex
	 * @return
	 */
	private List<String> getAllMatches(final String text, final String regex) {
        final List<String> matches = new ArrayList<String>();
        final Matcher m = Pattern.compile("(?=(" + regex + "))").matcher(text);
        while(m.find()) {
            matches.add(m.group(1));
        }
        return matches;
    }
	
	/**
	 * Get a unique meter name
	 * 
	 * @param meterName
	 * @param monitorableName
	 * @return the meter name
	 */
	private String getMeterName(final String prefix, final String alias, final String meterName, final String monitorableName) {
		String name = "";
		if (prefix != null){
			name = new StringBuilder(prefix).append('_').toString().toString();
		}
		
		name = name.concat((alias != null)? alias : new StringBuilder(monitorableName).append('_').append(meterName).toString());
		
		return name.replace(".", "_")
				   .replace("*", "ALL")
				   .replace(":", "_")
				   .replace("-", "_");
	}
	
	
	private PrometheusMeterType getMeterType(final Meter mt) {
		
		final boolean isHistogram = _histogramDomains.stream()
								.filter(histoDomain -> Meters.matches(mt, histoDomain))
								.findFirst()
								.isPresent();
		
		if(isHistogram) {
			return PrometheusMeterType.HISTOGRAM;
		} else if(mt.getType() == Type.INCREMENTAL) {
			return PrometheusMeterType.COUNTER;
		} else {
			return PrometheusMeterType.GAUGE;
		}
	}
	
	
	/**
	 * 
	 * @param meterName
	 * @param labelNames:  have to be null if isLabeled = false
	 * @param isLabeled
	 * @return
	 */
	private io.prometheus.client.Summary.Builder buildSummary(final String meterName, final LinkedList<String> labelNames, final boolean isLabeled) {
		final io.prometheus.client.Summary.Builder builder = 
				Summary.build(meterName, meterName)
						.quantile(0.1, 0.09)
						.quantile(0.2, 0.08)
						.quantile(0.3, 0.08)
						.quantile(0.4, 0.08)
						.quantile(0.5, 0.05)
						.quantile(0.7, 0.03)
				        .quantile(0.9, 0.01)
				        .quantile(0.99,   0.001)
				        .quantile(0.999,  0.0001)
				        .quantile(0.9999, 0.00001)
						.quantile(0.99999,0.000001);
		if (isLabeled && labelNames != null && !labelNames.isEmpty()) {
			builder.labelNames(labelNames.toArray(new String[labelNames.size()]));
		}

		return builder;
	}
	
	// TODO: only multiply, could handle other operations
	private double operate(final long value) {
		final double val = Long.valueOf(value).doubleValue();
		return (_multiply != null)? (val*_multiply) : val;
	}
	
	/**
	 * Check if arguments are valid
	 */
	private void checkArguments(){
		final String usage = "usage: exportMeters -m <monitorable> (or -ms <monitorablePattern>)"
					+ "-mt <meterName>  (or -mts <meterPattern>) -prefix <prefix> -alias";
		
		if ((_metersNames.size() == 0 && _mtsPattern == null) || (_metersNames.size() > 0 && _mtsPattern != null))
			throw new IllegalArgumentException("Invalid declaration [error on -mt or -mts option], "+ usage); 
		if ((_monName == null && _msPattern == null) || (_monName != null && _msPattern != null))
			throw new IllegalArgumentException("Invalid declaration [error on -m or -ms option], "+ usage); 
//		if (_alias != null && ((_mtsPattern != null && !_useLabelPattern)|| _metersNames.size() > 1))
//			throw new IllegalArgumentException("Invalid declaration [error, alias with prefix or several meters is impossible], " + usage);
		if (_useLabels && ((_mtsPattern == null && _metersNames.size() > 1) || _prefix != null || _alias == null))
			throw new IllegalArgumentException("Invalid declaration [error, label usage only available for one meter at a time], " + usage);
		if (_useLabels){
			final Pattern pattern = Pattern.compile("^\\w+=\\S+$"); 
			final boolean goodFormat = _labels.stream().allMatch(label -> pattern.matcher(label).find());
			_log.debug(String.format("PARSE %s", _labels));
			if (!goodFormat) throw new IllegalArgumentException("Invalid label declaration [must be -lb <key>=<value>], " + usage);
		}
		
		if(_optionalTypeOverride != null) {
			try {
				PrometheusMeterType.valueOf(_optionalTypeOverride);
			} catch (final IllegalArgumentException e) {
				throw new IllegalArgumentException("unknown Prometheus Meter Type " + _optionalTypeOverride);
			}
		}
		
	}
	
	private class GaugeLabelListener extends LabeledListener implements MeterListener<Void> {
		private final Gauge g = (Gauge) prometheusMetric;
		private io.prometheus.client.Gauge.Child childWlabels = null;
		
		public GaugeLabelListener(final SimpleCollector<?> prometheusMetric, final String[] labels) {
			super(prometheusMetric, labels);
			if (this.labels != null)
				childWlabels = g.labels(labels);
		}
		
		public GaugeLabelListener(final SimpleCollector<?> prometheusMetric) {
			super(prometheusMetric, null);
		}
		
		@Override
		public Void updated(final Meter meter, final Void context) {
			final long currentValue = meter.getValue();
			if(labels != null) {
				childWlabels.set(operate(currentValue));
			} else {
				g.set(operate(currentValue));
			}
			return null;
		}
		
	}
	private class CounterListener extends LabeledListener implements MeterListener<Void> {
		private Long oldValue = null;
		private final Counter c = (Counter) prometheusMetric;
		private io.prometheus.client.Counter.Child childWlabels = null;
		
		public CounterListener(final SimpleCollector<?> prometheusMetric) {
			super(prometheusMetric, null);
		}
		
		public CounterListener(final SimpleCollector<?> prometheusMetric, final String[] labels) {
			super(prometheusMetric, labels);
			if (this.labels != null)
				childWlabels = c.labels(labels);
		}
		
		
		private void inc(final long value) {
			if(labels != null) {
				childWlabels.inc(operate(value));
			} else {
				c.inc(operate(value));
			}
		}
		
		
		@Override
		public Void updated(final Meter meter, final Void context) {
			final long currentValue = meter.getValue();
			if(oldValue == null) {
				oldValue = currentValue;
				inc(oldValue);
			} 
			else {
				final long delta = currentValue - oldValue;
				if(delta < 0) {
					if (labels != null)
						resetChild();
					else
						c.clear();
					inc(currentValue);
				} 
				else if (delta == 0) {
					return null;
				}
				else {
					inc(delta);
				}
				oldValue = currentValue;
			}
			
			return null;
		}
		
		private void resetChild() {
			c.remove(labels);
			childWlabels = c.labels(labels);
		}
	}
	
	private class HistogramLabelListener extends LabeledListener implements MeterListener<Void> {
		private final Summary summary = (Summary) prometheusMetric;
		private io.prometheus.client.Summary.Child childWlabels = null;
		
		public HistogramLabelListener(final SimpleCollector<?> prometheusMetric, final String[] labels) {
			super(prometheusMetric, labels);
			if (this.labels != null)
				childWlabels = summary.labels(this.labels);
		}
		
		public HistogramLabelListener(final SimpleCollector<?> prometheusMetric) {
			super(prometheusMetric, null);
		}
		
		@Override
		public Void updated(final Meter meter, final Void context) {
			final Summary s = (Summary) prometheusMetric;
			final long currentValue = meter.getValue();
			if(labels != null) {
				childWlabels.observe(operate(currentValue));
			} else {
				s.observe(operate(currentValue));
			}
			return null;
		}
		
	}
	
	private class LabeledListener {
		SimpleCollector<?> prometheusMetric;
		String[] labels;
		LabeledListener(final SimpleCollector<?> prometheusMetric, final String[] labels) {
			this.prometheusMetric = prometheusMetric;
			this.labels = labels;
		}
	}
	
}
