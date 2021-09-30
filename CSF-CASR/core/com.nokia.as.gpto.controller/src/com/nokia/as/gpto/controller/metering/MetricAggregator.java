package com.nokia.as.gpto.controller.metering;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.LongStream;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.ValueSupplier;
import com.alcatel.as.service.metering2.util.MeteringRegistry;
import com.nokia.as.gpto.common.msg.api.AgentRegistration;
import com.nokia.as.gpto.common.msg.api.AgentRegistration.MeterType;
import com.nokia.as.gpto.common.msg.api.GPTOMonitorable;
import com.nokia.as.gpto.common.msg.api.Pair;
import com.nokia.as.gpto.controller.impl.GPTOAgent;

public class MetricAggregator {
	private MeteringRegistry meteringRegistry;

	private MeteringService meteringService;
	
	private BundleContext bc;
	
	private static Logger LOG = Logger.getLogger(MetricAggregator.class);
	
	/**
	 * (MonitorableName,MeterName) -> Map(K:Agent hashcode, V:GPTORegistration)
	 */
	Map<Pair<String, String>, Map<Integer, AgentRegistration>> gptoRegistry;
	
	public MetricAggregator(BundleContext bc, MeteringService meteringService, MeteringRegistry meteringRegistry) {
		super();
		this.meteringRegistry = meteringRegistry;
		this.meteringService = meteringService;
		this.bc = bc;
		gptoRegistry = new ConcurrentHashMap<>();
	}
	
	public synchronized void updateRegistry(int hashcode, GPTOAgent agent, Map<String, AgentRegistration> registry) {
		Map<String, GPTOMonitorable> newMonitorables = new ConcurrentHashMap<>();
		Collection<AgentRegistration> registrations = registry.values();
		registrations.forEach(agentRegistration -> {
			String monitorableName = agentRegistration.getMonitorableName();
			String meterName = agentRegistration.getMeterName();
			
			// Check if monitorable is already in MeteringRegistry
			GPTOMonitorable monitorable =  (GPTOMonitorable) meteringRegistry.getMonitorable(monitorableName);
			if (monitorable == null && !newMonitorables.containsKey(monitorableName)) {
				// Then we must create it
				String monitorableDesc = "Execution of a GPTO scenario"; 
				monitorable = new GPTOMonitorable(monitorableName, monitorableDesc);
				newMonitorables.putIfAbsent(monitorableName, (GPTOMonitorable)monitorable);
			} else if (newMonitorables.containsKey(monitorableName)) {
				monitorable = newMonitorables.get(monitorableName);
			}
			
			Meter meter = monitorable.getMeter(meterName);
			// If the meter does not exist then we must create & add it to our local registry
			if (meter == null) {
				buildMeter(monitorable, monitorableName, meterName, agentRegistration.getType());
				Map<Integer, AgentRegistration> hashcodeToRegistration = new ConcurrentHashMap<>();
				hashcodeToRegistration.put(hashcode, agentRegistration);
				gptoRegistry.putIfAbsent(Pair.of(monitorableName, meterName), hashcodeToRegistration);
				meter = monitorable.getMeter(meterName);
			}
			// Check and update the value of our registry
			updateRegistration(hashcode, agentRegistration, monitorableName, meterName, meter);
		});
		removeOldRegistrations(registrations, hashcode);
		
		// Add all new monitorables to the Metering Registry
		newMonitorables.values().forEach(mon -> mon.start(bc));
	}
	
	
	private void buildMeter(GPTOMonitorable monitorable, String monitorableName, String meterName, MeterType type) {
		if(gptoRegistry.get(Pair.of(monitorableName, meterName)) == null) {
			if (type.equals(MeterType.HISTOGRAM)) {
				LOG.debug(Thread.currentThread().toString());
				monitorable.createAbsoluteMeter(meteringService, meterName, type);
			} else {
				monitorable.createValueSuppliedMeter(meteringService, meterName, new OpAggregatorSupplier(monitorableName, meterName, type), type);
			}
		} else {
			LOG.debug("[BuildMeter] "+ Pair.of(monitorableName, meterName)+" already registered");
		}
		
	}

	private void updateRegistration(int hashcode, AgentRegistration reg, String monitorableName, String meterName, Meter meter) {
			if(reg.getType().equals(MeterType.HISTOGRAM)) {
				updateHistogramRegistration(hashcode, reg, monitorableName, meterName, meter);
			} else {
				updateGaugeRegistration(hashcode, reg, monitorableName, meterName);
			}
	}
	
	private void updateGaugeRegistration(int hashcode, AgentRegistration reg, String monitorableName, String meterName) {
		Map<Integer, AgentRegistration> regs = gptoRegistry.get(Pair.of(monitorableName, meterName));
		AgentRegistration currentReg = regs.get(hashcode);
		if(currentReg == null)
			regs.putIfAbsent(hashcode, reg);
		else {
			currentReg.setValue(reg.getSingleValue().get());
		}
	}
	
	private void updateHistogramRegistration(int hashcode, AgentRegistration reg, String monitorableName, String meterName, Meter meter) {
		Map<Integer, AgentRegistration> regs = gptoRegistry.get(Pair.of(monitorableName, meterName));
		AgentRegistration currentReg = regs.get(hashcode);
		LOG.debug("[UpdateHisto] agent<"+hashcode+">nb_values="+reg.getValues().values().stream().mapToLong(num -> num.longValue()).sum()+
				"   |||   sum= "+
				reg.getValues().keySet().stream().mapToLong(val -> val*reg.getValues().get(val).longValue()).sum());
		if(currentReg == null) {
			regs.put(hashcode, reg);
			// TODO: fix->listener not called
			reg.getValues().forEach((val,iteration) -> {
				for (int i = 0; i < iteration.longValue() ; i++) {
					meter.set(val);
				}
			});
			
		} else {
			Map<Long, LongAdder> localValues = currentReg.getValues();
			reg.getValues().forEach((val,remoteIterations) -> {
				long iteration = remoteIterations.longValue();
				if(localValues.containsKey(val)) {
					iteration = remoteIterations.longValue() - localValues.get(val).longValue();
				}
				if(iteration < 0) iteration = 0L;
				for (int i = 0; i < iteration ; i++) {
					meter.set(val);
					currentReg.addValue(val);
				}
			});
		}
	}
	
	// TODO not working for now...
	private void removeOldRegistrations(Collection<AgentRegistration> regs, int hashcode) {
//		List<AgentRegistration> localRegs = gptoRegistry.values()
//														.stream()
//														.filter(map -> map.containsKey(hashcode))
//														.flatMap(map -> Stream.of(map.get(hashcode)))
//														.collect(Collectors.toList());
//		regs.forEach(reg -> {
//			Optional<AgentRegistration> foundReg =
//					localRegs.stream()
//					 .filter(localReg -> localReg.getUniqueName().equals(reg.getUniqueName()))
//					 .findFirst();
//			if(foundReg.isPresent())
//				localRegs.remove(foundReg);
//		});
//		
//		if(!localRegs.isEmpty()) {
//			localRegs.forEach(localReg -> {
//				gptoRegistry.get(Pair.of(localReg.getMonitorableName(), localReg.getMeterName())).remove(hashcode);
//				LOG.debug("REMOVED "+hashcode+" from <"+localReg.getMonitorableName()+", "+localReg.getMeterName() +">");
//			});
//		}
	}
	
	
	/**
	 * Aggregate the values for min or the following operations: min, max & avg
	 */
	class OpAggregatorSupplier implements ValueSupplier{
		private String monitorableName;
		private String meterName;
		private MeterType type;
		
		OpAggregatorSupplier(String monitorableName, String meterName, MeterType type){
			this.monitorableName = monitorableName;
			this.meterName = meterName;
			this.type = type;
		}
		
		@Override
		public long getValue() {
			Map<Integer, AgentRegistration> regs = gptoRegistry.get(Pair.of(monitorableName, meterName));
			if (regs != null) {
				LongStream values = regs.values()
										.stream()
										.mapToLong(reg -> reg.getSingleValue().get());
				return process(values);
			} else {
				return 0L;
			}
		}
		
		// TODO renvoyer exception au lieu de 0L = mieux pour debug...
		private long process(LongStream lstream) {
			long value = 0L;
			switch(type) {
				case GAUGE:
					value = lstream.sum();
					break;
				case MIN:
					value = lstream.min().orElse(0L);
					break;
				case MAX:
					value = lstream.max().orElse(0L);
					break;
				case AVG:
					Double d = lstream.average().orElse(0L);
					value = d.longValue();
					break;
				default: 
					return 0L;
			}
			
			return value;
		}
		
	}
	
}
