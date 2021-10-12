// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.metering.prometheus.common;

import java.util.LinkedList;
import java.util.List;

public class KeyValueLabels {
	protected LinkedList<String> labelNames;
	
	protected LinkedList<String> labelValues;
	
	public KeyValueLabels(LinkedList<String> labelNames, LinkedList<String> labelValues) {
		this.labelNames = labelNames;
		this.labelValues = labelValues;
	}
	
	/**
	 * labels format : key=value
	 * @param labels
	 * @throws Exception 
	 */
	public KeyValueLabels(List<String> labels) throws Exception {
		this.labelNames = new LinkedList<>();
		this.labelValues = new LinkedList<>();
	
		try{
			labels.forEach(label -> {
				labelNames.add(label.split("=")[0]);
				labelValues.add(label.split("=")[1]);
			});
		} catch (IndexOutOfBoundsException e){
			throw new Exception("Index error on split, bad arguments -> "+ labels);
		} catch (Throwable t) {
			throw new Exception("Bad arguments -> "+ labels);
		}
		
	}
	
	public LinkedList<String> getKeys(){ return labelNames; }

	public LinkedList<String> getValues(){ return labelValues; }

}
