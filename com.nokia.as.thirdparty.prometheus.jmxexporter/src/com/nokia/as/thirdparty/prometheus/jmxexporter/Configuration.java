// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.thirdparty.prometheus.jmxexporter;

import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;

public interface Configuration {	
	
	@FileDataProperty(title="JMX Exporter Configuration", dynamic=true, required=true, fileData="configJmx.txt",
			  help="This property contains the configuration of JmxExporter, please refer to https://github.com/prometheus/jmx_exporter", section="metering")
	String getConfig();
}

