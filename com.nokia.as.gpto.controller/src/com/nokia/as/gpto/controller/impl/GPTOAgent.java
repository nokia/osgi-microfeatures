// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.controller.impl;

import java.util.Set;

import com.nokia.as.gpto.common.msg.api.ExecutionState;

public class GPTOAgent {

	private String _name;
	private String _remoteIP;
	private Set<String> _scenarii;
	private Set<String> availableStrategies;
	private ExecutionState executionState;

	public GPTOAgent(String _name, String remoteIP) {
		super();
		this._name = _name;
		this._remoteIP = remoteIP;
		executionState = ExecutionState.NULL;
	}

	public String getName() {
		return _name;
	}

	public String getRemoteAddress() {
		return _remoteIP;
	}
	
	public Set<String> getScenarii() {
		return _scenarii;
	}

	public ExecutionState getExecutionState() {
		return executionState;
	}

	public void setScenarii(Set<String> scenarii) {
		this._scenarii = scenarii;
	}

	public void setExecutionState(ExecutionState state) {
		this.executionState = state;
	}

	public Set<String> getAvailableStrategies() {
		return availableStrategies;
	}

	public void setAvailableStrategies(Set<String> availableStrategies) {
		this.availableStrategies = availableStrategies;
	}

	@Override
	public String toString() {
		return "GPTOAgent [_name=" + _name + ", _scenarii=" + _scenarii + ", availableStrategies=" + availableStrategies
				+ ", executionState=" + executionState + "]";
	}
}
