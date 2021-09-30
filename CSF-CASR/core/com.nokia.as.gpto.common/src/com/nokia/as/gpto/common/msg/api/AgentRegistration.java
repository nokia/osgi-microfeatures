package com.nokia.as.gpto.common.msg.api;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class AgentRegistration implements Serializable{
	private static final long serialVersionUID = 1L;

	/**
	 * Meter Type used for aggregation in the controller
	 */
	public enum MeterType{
		HISTOGRAM,
		GAUGE,
		MIN,
		MAX,
		AVG
	}
	
	/**
	 * Unique name with following format: "MonitorableName/MeterName"
	 */
	private String uniqueName;
	
	private String meterName;
	
	private String monitorableName;
	
	private Map<Long, LongAdder> values;
	
	private AtomicLong singleValue;
	
	private MeterType type;

	public AgentRegistration(String uniqueName, MeterType type) {
		super();
		meterName = getMeterNamefrom(uniqueName);
		monitorableName = getMonitorableNamefrom(uniqueName);
		this.uniqueName = uniqueName;
		values = new ConcurrentHashMap<>();
		singleValue = new AtomicLong();
		this.type = type;
	}

	/**
	 * Set a gauge value
	 * @param value
	 */
	public void setValue(Long value) {
		singleValue.set(value);
	}

	/**
	 * Add a value to the histogram
	 * @param value
	 */
	public void addValue(Long value) {
		values.computeIfAbsent(value, (k) -> new LongAdder()).increment();
	}
	
	/**
	 * Set a value with a specific number of iteration in the histogram
	 * @param value
	 * @param iteration
	 */
	public void setValue(Long value, Long iteration) {
		LongAdder adder = new LongAdder();
		adder.add(iteration);
		values.put(value, adder);
	}
	
	public static String generateUniqueName(String monitorableName, String meterName) {
		return new StringBuilder(monitorableName)
					.append("/")
					.append(meterName)
					.toString();
	}
	
	public boolean isHistogram() {
		return type.equals(MeterType.HISTOGRAM);
	}
	
	public String getUniqueName() {
		return uniqueName;
	}
	
	public String getMeterName() {
		return meterName;
	}

	public String getMonitorableName() {
		return monitorableName;
	}
	
	
	
	public Map<Long, LongAdder> getValues() {
		return values;
	}

	public AtomicLong getSingleValue() {
		return singleValue;
	}

	public MeterType getType() {
		return type;
	}

	public String getMeterNamefrom(String uniqueName) {
		return uniqueName.split("/")[1];
	}
	
	public String getMonitorableNamefrom(String uniqueName) {
		return uniqueName.split("/")[0];
	}
	
	@Override
	public String toString() {
		return (type.equals(MeterType.HISTOGRAM))?
				new StringBuffer(uniqueName).append("(").append(type).append(") : ").append(values).toString():
				new StringBuffer(uniqueName).append("(").append(type).append(") : ").append(singleValue).toString();
	}
}
