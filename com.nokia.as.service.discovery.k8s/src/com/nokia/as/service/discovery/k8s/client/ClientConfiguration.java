// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.service.discovery.k8s.client;

import java.util.List;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

@Config(section = "Kubernetes discovery configuration")
public interface ClientConfiguration {
	@StringProperty(title = "Namespaces", 
					help = "List of namespaces that will be watched",
					defval = "${DISCOVERY_NAMESPACES}{$CURRENT$}", 
					required = true)
	public List<String> getNamespaces();
	
}