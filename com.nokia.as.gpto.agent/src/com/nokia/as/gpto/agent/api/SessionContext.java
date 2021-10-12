// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.agent.api;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

public interface SessionContext {
	
	/**
	 * Get the {@link LocalDateTime LocalDateTime} of the start of the execution
	 */
	LocalDateTime getExecutionStart();
	
	Long getSessionId();
	
	SessionContext attach(Object obj);

	<T> T attachment();
	
	long getMainCounterValue();
	
	long getSubCounterValue();
}
