package com.nokia.as.gpto.agent.api;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.nokia.as.gpto.common.msg.api.GPTOMonitorable;

public interface ExecutionContext {

	/**
	 * Get the option passed during the scenario execution
	 */
	JSONObject getOpts();
	
	/**
	 * A {@link GPTOMonitorable Monitorable} for providing metrics
	 */
	GPTOMonitorable getMonitorable();
	
	/**
	 * A logger for all your logging needs during the execution
	 */
	Logger getLogger();
	
	int getExecutionId();
	
	ExecutionContext attach(Object obj);	
	
	long getCurrentIterationCount();

	<T> T attachment();
}
