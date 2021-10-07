package com.nokia.as.gpto.common.msg.api;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.metering2.ValueSupplier;
import com.nokia.as.gpto.common.msg.api.AgentRegistration.MeterType;

public class GPTOMonitorable extends SimpleMonitorable{

	Map<String, MeterType> meterNameToType;
	
	public GPTOMonitorable(String monitorableName, String monitorableDesc) {
		super(monitorableName, monitorableDesc);
		meterNameToType = new ConcurrentHashMap<>();
	}
	
	public Meter createAbsoluteMeter(MeteringService service, String name, MeterType type) {
		meterNameToType.put(name, type);
		return super.createAbsoluteMeter(service, name);
	}
	
	public Meter createIncrementalMeter(MeteringService service, String name, Meter parent, MeterType type) {
		meterNameToType.put(name, type);
		return super.createIncrementalMeter(service, name, parent);
	}

	public Meter createValueSuppliedMeter(MeteringService service, String name, ValueSupplier valueSupplier, MeterType type) {
		meterNameToType.put(name, type);
		return super.createValueSuppliedMeter(service, name, valueSupplier);
	}
	
	@Override
	public Meter createAbsoluteMeter(MeteringService service, String name) {
		meterNameToType.put(name, MeterType.GAUGE);
		return super.createAbsoluteMeter(service, name);
	}
	
	@Override
	public Meter createIncrementalMeter(MeteringService service, String name, Meter parent) {
		meterNameToType.put(name, MeterType.GAUGE);
		return super.createIncrementalMeter(service, name, parent);
	}
	
	@Override
	public Meter createValueSuppliedMeter(MeteringService service, String name, ValueSupplier valueSupplier) {
		meterNameToType.put(name, MeterType.GAUGE);
		return super.createValueSuppliedMeter(service, name, valueSupplier);
	}

	@Override
	public SimpleMonitorable addMeter(Meter... meters) {
		Arrays.asList(meters).forEach(mt -> meterNameToType.put(mt.getName(), MeterType.GAUGE));
		return super.addMeter(meters);
	}
	
	public SimpleMonitorable addMeter(MeterType type, Meter... meters) {
		Arrays.asList(meters).forEach(mt -> meterNameToType.put(mt.getName(), type));
		return super.addMeter(meters);
	}
	
	/**
	 * Get MeterType of a given meter name from this monitorable
	 * @param meterName
	 * @return the according type
	 */
	public MeterType getMeterType(String meterName) {
		MeterType type = meterNameToType.get(meterName);
		if (type == null) return MeterType.GAUGE;
		else return type;
	}
	
	@Override
	public SimpleMonitorable removeMeter(Meter... meters) {
		Arrays.asList(meters).forEach(mt -> meterNameToType.remove(mt.getName()));
		return super.removeMeter(meters);
	}
	
	@Override
	public void start(BundleContext bc) {
		super.start(bc);
	}
	
}
