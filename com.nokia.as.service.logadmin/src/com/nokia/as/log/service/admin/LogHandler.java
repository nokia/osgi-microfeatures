// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.log.service.admin;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Interface tracked from the OSGi registry that is used to notify log messages.
 * a LogHandler must be an OSGi service and may provide a serice property which indicates
 * if LogMessage passed to the handled should be formatted or not.
 */
@ConsumerType
public interface LogHandler {
	
	/**
	 * Optional OSGi service property indicating if the log message passed to the handleLog method 
	 * should be formatted. By default, logs are formatted.
	 */
	public final static String FORMAT = "format";
	
	public void handleLog(LogMessage message);
	
}
