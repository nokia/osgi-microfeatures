// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.common.impl;

import java.util.concurrent.Executor;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutors;

public class Helper {
	
	/**
	 * Determine the type of executors to be used when executing jaxrs resources.
	 * @param type either "io", "cpu", or null. If null, then IO ThreadPool is returned by default.
	 */
	public static Executor getResourceExecutor(PlatformExecutors execs, String type) {
		if ("io".equals(type) || type == null) {
			return execs.getIOThreadPoolExecutor();
		} else if ("cpu".equals(type)) {
			// since the jaxrs container is itself running within the processing threadpool, it's better to reschedule in the 
			// processing tpool using a SCHEDULE_HIGH policy, in order to handle the requests before handling other concurrent socket reads.
			return execs.getProcessingThreadPoolExecutor().toExecutor(ExecutorPolicy.SCHEDULE_HIGH);
		} else {
			throw new IllegalArgumentException("Invalid property value for threadpool.type: " + type);
		}
	}
	
}
