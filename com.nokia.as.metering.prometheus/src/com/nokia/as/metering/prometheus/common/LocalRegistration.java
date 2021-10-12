// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.metering.prometheus.common;

import java.util.LinkedList;

/**
 * Local Registration in ExportedMeter entry. When the entry is stopped
 * all the local registrations are removed
 *
 */
public class LocalRegistration {
	
	protected String meterName;
	
	protected LinkedList<String> labelValues;

	public LocalRegistration(String meterName, LinkedList<String> labelValues) {
		super();
		this.meterName = meterName;
		this.labelValues = labelValues;
	}

	public String getMeterName() {
		return meterName;
	}

	public LinkedList<String> getLabelValues() {
		return labelValues;
	}
	
}
