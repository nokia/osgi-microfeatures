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
