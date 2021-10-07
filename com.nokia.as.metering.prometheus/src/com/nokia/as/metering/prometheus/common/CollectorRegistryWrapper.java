package com.nokia.as.metering.prometheus.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;

/**
 * That wrapper is andatory to access registered Prometheus Meters...
 *
 */
public class CollectorRegistryWrapper {
	
	/**
	 * Prometheus official Collector Registry
	 */
	protected final CollectorRegistry _registry;
	
	/**
	 * Map containing local registrations. Map <key = Meter Name, value={@link PrometheusRegistration}
	 * @see PrometheusRegistration
	 */
	private Map<String, PrometheusRegistration> _registrations;

	/**
	 * Constructor
	 * @param _registry
	 */
	public CollectorRegistryWrapper() {
		super();
		this._registry = new CollectorRegistry(true);
		this._registrations = new HashMap<>(); 
	}

	public final CollectorRegistry getRegistry() { return _registry; }

	public Collection<PrometheusRegistration> getRegistrations() { return _registrations.values();	}
	
	public Map<String, PrometheusRegistration> getRegistrationsMap(){ return _registrations;}

	public PrometheusRegistration getRegistration(String meterName){
		return _registrations.get(meterName);
	}
	
	public boolean isRegistrable(String meterName){
		PrometheusRegistration reg = _registrations.get(meterName);
		
		return (reg == null);
	}
	
	public boolean isRegistrable(String meterName, LinkedList<String> labelNames, LinkedList<String> labelValues){
		if(labelNames == null || labelValues == null) {
			return isRegistrable(meterName);
		}
		
		PrometheusRegistration reg = _registrations.get(meterName);
		if (reg == null) return true;
		if (reg.isLabelsEquals(labelNames)){
			KeyValueLabels orderedLabel = reg.getOrderedLabels(new KeyValueLabels(labelNames, labelValues));
			return (reg.isLabelsExisting(orderedLabel.getValues())) ? false : true;
		}
		return false;
	}
	
	public PrometheusRegistration getRegistration(String meterName, LinkedList<String> labelNames, LinkedList<String> labelValues){
		PrometheusRegistration reg = _registrations.get(meterName);
		
		if (reg == null) return null;
		
		if (reg.isLabelsEquals(labelNames)){
			KeyValueLabels orderedLabel = reg.getOrderedLabels(new KeyValueLabels(labelNames, labelValues));
			return (reg.isLabelsExisting(orderedLabel.getValues())) ? null : reg;
		}
		return null;
	}
	
	public boolean contains(String meterName){ return _registrations.containsKey(meterName); }
	
	/**
	 * Register unmodifiable metrics
	 * @param gauge
	 */
	public void register(Collector gauge) { _registry.register(gauge); }
	
	/**
	 * Register local metrics
	 * @param meterName
	 * @param gauge
	 */
	public void register(String meterName, PrometheusRegistration registration) { 
		if (!_registrations.containsKey(meterName)){
			_registrations.put(meterName, registration);
			_registry.register(registration.getPrometheusMeter()); 
		}
	}
	
	public void unregister(Collector gauge) { _registry.unregister(gauge); }
	
	public void unregister(String meterName, LinkedList<String> labels) {
		if(_registrations.containsKey(meterName)){
			// TODO : stop job & unregister from CollectorRegistry
			PrometheusRegistration reg = _registrations.get(meterName);
			reg.stopJob(labels);
		}
	}
	
	public void unregister(String meterName){
		if (_registrations.containsKey(meterName)){
			PrometheusRegistration reg = _registrations.get(meterName);
			reg.stopJobs();
			_registry.unregister(reg.getPrometheusMeter());
			_registrations.remove(meterName);
		}
	}
	
}
