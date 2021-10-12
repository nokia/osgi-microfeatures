// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.agent.impl;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.nokia.as.gpto.agent.api.ExecutionContext;
import com.nokia.as.gpto.common.msg.api.GPTOMonitorable;

public class ExecutionContextImpl implements ExecutionContext {

	private final int executionId;
	
	protected JSONObject opts;
	
	protected GPTOMonitorable monitorable;
	
	protected Logger logger;
	
	protected Object attachment;
	
	protected AtomicLong iterationCounter;
	
	protected long count;
	
	public ExecutionContextImpl(int executionId, JSONObject opts, GPTOMonitorable monitorable, Logger logger) {
		this.executionId = executionId;
		this.opts = opts;
		this.monitorable = monitorable;
		this.logger = logger;
		iterationCounter = new AtomicLong();
	}
	
	@Override
	public JSONObject getOpts() {
		return opts;
	}

	@Override
	public GPTOMonitorable getMonitorable() {
		return monitorable;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public ExecutionContext attach(Object obj) {
		attachment = obj;
		return this;
	}

	@Override
	public <T> T attachment() {
		return (T) attachment;
	}

	public int getExecutionId() {
		return executionId;
	}

	@Override
	public long getCurrentIterationCount() {
		return count;
	}
	
	public void incrementIterationCount() {
		count = iterationCounter.incrementAndGet();
	}

}
