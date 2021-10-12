// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.service.loghistory.common;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

@Config(section = "logs")
public interface Configuration {

	public final static int DEF_BUFFER_SIZE = 100;
	public final static int DEF_MAX_LINE_SIZE = 10000;
	public final static String DEF_LAYOUT = "%d{ISO8601} %p %c %x %t - %m%n";

	
	@IntProperty(min = 1, max = Integer.MAX_VALUE, title = "Max number of log entries displayed", 
			help = "Specifies the maximum log entries provided by the service", 
			required = false, dynamic = false, defval = DEF_BUFFER_SIZE)
	int getBufferSize();
	
	@IntProperty(min = 1, max = Integer.MAX_VALUE, title = "Max number character by log entries", 
			help = "Specifies the maximum character per log entries, when the log entry has more than"
					+ "max characters allowed, then it will end the log entry with: \"[...]\"", 
			required = false, dynamic = false, defval = DEF_MAX_LINE_SIZE)
	int getMaxLineSize();
	
	@StringProperty(title = "Log Layout", 
			help = "Specifies the log layout", 
			required = false, dynamic = false, defval = DEF_LAYOUT)
	String getLayout();
}
