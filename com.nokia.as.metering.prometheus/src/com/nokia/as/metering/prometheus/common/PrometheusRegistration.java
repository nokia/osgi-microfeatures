// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.metering.prometheus.common;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.alcatel.as.service.metering2.MonitoringJob;

import io.prometheus.client.SimpleCollector;

public class PrometheusRegistration {
	MonitoringJob _job;
	
	SimpleCollector<?> _prometheusMeter;
	
	LinkedList<String> _labelNames;
	
	String _meterName;
	
	/**
	 * Set of "Label values" to a Monitoring Job
	 */
	Map<LinkedList<String>, MonitoringJob> _labelsToJob;
	
	/**
	 * Constructor for a labeled registration
	 * @param meterName
	 * @param job
	 * @param prometheusMeter
	 * @param labelNames
	 * @param labelValues
	 */
	public PrometheusRegistration(String meterName, MonitoringJob job, SimpleCollector<?> prometheusMeter, 
			LinkedList<String> labelNames, LinkedList<String> labelValues){
		_job = null;
		_prometheusMeter = prometheusMeter;
		_labelNames = labelNames;
		_meterName = meterName;
		_labelsToJob = new HashMap<>();
		_labelsToJob.put(labelValues, job);
	}

	public PrometheusRegistration(String meterName, MonitoringJob job, SimpleCollector<?> prometheusMeter){
		_job = job;
		_prometheusMeter = prometheusMeter;
		_labelNames = new LinkedList<>();
		_meterName = meterName;
		_labelsToJob = new HashMap<>();
	}
	
	public MonitoringJob getNoLabelJob() {
		return _job;
	}

	public SimpleCollector<?> getPrometheusMeter() {
		return _prometheusMeter;
	}

	public LinkedList<String> getLabelNames() {
		return _labelNames;
	}
	
	/**
	 * Return the found index for a label name.
	 * @param labelName : Label name
	 * @return the according index, -1 otherwise
	 */
	public int getIndexOfLabel(String labelName){ 
		if (_labelNames == null || _labelNames.size() == 0) return -1;
		return _labelNames.contains(labelName)?_labelNames.indexOf(labelName):-1;
	}
	
	@Override
	public int hashCode() {
		return _prometheusMeter.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
	
	public boolean isLabelsExisting(LinkedList<String> labels){ 
		if (_labelNames == null || _labelNames.size() == 0) return false;
		return _labelsToJob.containsKey(labels); 
	}

	public boolean isUsingLabels(){
		return (_labelNames == null || _labelNames.size() == 0);
	}
	
	public String getMeterName(){ return _meterName; }
	
	public MonitoringJob getJob(LinkedList<String> labels){
		return _labelsToJob.get(labels);
	}
	
	public boolean isLabelsEquals(PrometheusRegistration reg){
		return isLabelsEquals(reg.getLabelNames());
	}
	
	public boolean isLabelsEquals(List<String> labelNames){
		boolean isEquals = true;
		if (labelNames.size() == _labelNames.size()){
//			for(String labelName :labelNames)
//				isEquals = isEquals && _labelNames.contains(labelName);
//			for(String labelName : _labelNames)
//				isEquals = isEquals && labelNames.contains(labelName);
			isEquals = labelNames.containsAll(_labelNames) && _labelNames.containsAll(labelNames);
		} else
			return false;
		
		return isEquals;
	}
	
	public Map<LinkedList<String>, MonitoringJob> getMonitoringJobs(){
		return _labelsToJob;
	}
	
	public void setJob(LinkedList<String> labelValues, MonitoringJob job){
		_labelsToJob.put(labelValues, job);
	}
	
	/**
	 * Stop according job and remove labeled metric (if existing)
	 * @param labels
	 */
	public void stopJob(LinkedList<String> labels){
		MonitoringJob job = getJob(labels);
		if (job != null) {
			job.stop();
			_labelsToJob.remove(labels);
		}
	}
	
	public void stopJobs(){
		_labelsToJob.values()
					.forEach(job -> {
						job.stop();	
					});
		_labelsToJob.clear();
	}
	
	/**
	 * labels format : "key=value"
	 * @param labels
	 * @return
	 */
	public KeyValueLabels getOrderedLabels(List<String> labels){
		List<String> parsedLabelNames = labels.stream()
										  .map(label -> label.split("=")[0])
										  .collect(Collectors.toList());
		if (!isLabelsEquals(parsedLabelNames)) return null;
		try {
			//Create a Map <LabelIndex, Label>
			LinkedList<String> keys = new LinkedList<>();
			LinkedList<String> values = new LinkedList<>();
			TreeMap<Integer, String> indexToLabel = new TreeMap<>();
			labels.forEach(label -> {
				String labelName = label.split("=")[0];
				int index = getIndexOfLabel(labelName);
				indexToLabel.put(index, labelName);
			});
			
			// Reorder label keys & values and add them to the according TreeSet
			SortedSet<Integer> sortedKeys = new TreeSet<Integer>(indexToLabel.keySet());
			for (Integer key : sortedKeys) { 
			   String label = indexToLabel.get(key);
			   String labelName = label.split("=")[0];
			   String labelValue = label.split("=")[1];
			   keys.add(labelName);
			   values.add(labelValue);
			}
			KeyValueLabels orderedLabels = new KeyValueLabels(keys, values);
			return orderedLabels;
			
		} catch (Exception e){
			e.printStackTrace();
			throw new IllegalArgumentException("Error, Label format: key=value");
		}
	}
	
	public KeyValueLabels getOrderedLabels(KeyValueLabels labels){
		if (!isLabelsEquals(labels.getKeys()) || labels.getKeys().size() != labels.getValues().size()) return null;
		try {
			//Create a Map <LabelIndex, Label>
			LinkedList<String> keys = new LinkedList<>();
			LinkedList<String> values = new LinkedList<>();
			TreeMap<Integer, String> indexToLabel = new TreeMap<>();
			LinkedList<String> dKeys = new LinkedList<>(labels.getKeys());
			LinkedList<String> dValues = new LinkedList<>(labels.getValues());
			
			for (int i=0; i < dKeys.size(); i++){
				String labelName = dKeys.get(i);
				String labelValue = dValues.get(i);
				int index = getIndexOfLabel(labelName);
				indexToLabel.put(index, labelName.concat("=").concat(labelValue));
			}
				
			// Reorder label keys & values and add them to the according TreeSet
			SortedSet<Integer> sortedKeys = new TreeSet<Integer>(indexToLabel.keySet());
			for (Integer key : sortedKeys) { 
			   String label = indexToLabel.get(key);
			   String labelName = label.split("=")[0];
			   String labelValue = label.split("=")[1];
			   keys.add(labelName);
			   values.add(labelValue);
			}
			KeyValueLabels orderedLabels = new KeyValueLabels(keys, values);
			return orderedLabels;
			
		} catch (Exception e){
			e.printStackTrace();
			throw new IllegalArgumentException("Error, Label format: key=value");
		}
	}
	
	@Override
	public String toString() {
		return String.format("[name=%s, labelsNames=%s, number_of_jobs=%s]", _meterName, _labelNames, _labelsToJob.size());
	}
	
}