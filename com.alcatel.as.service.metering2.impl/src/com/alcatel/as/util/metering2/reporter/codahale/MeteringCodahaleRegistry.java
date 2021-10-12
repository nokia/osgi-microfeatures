// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.metering2.reporter.codahale;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import com.codahale.metrics.MetricRegistry;

/**
 * This OSGi service maintains a Codahale Metrics Registry and stores in it all 
 * available metering2 meters.
 */
@ProviderType
public interface MeteringCodahaleRegistry {
	
	public final static String MONITORABLE_NAME= "monitorable.name";
	
	/**
	 * Returns the Codhadale registry which contains all metering2 meters
	 */
	public Map<String, MetricRegistry> getMeteringRegistries();
	
}
