// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.metering.prometheus.pull.servlet;

import org.apache.felix.dm.annotation.api.Component;

import com.nokia.as.metering.prometheus.pull.AbstractPullMetersTracker;

@Component(provides = ServletPullMetersTracker.class)
public class ServletPullMetersTracker extends AbstractPullMetersTracker {
	String getMetricsForServlet() {
		return getMetrics();
	}
}
