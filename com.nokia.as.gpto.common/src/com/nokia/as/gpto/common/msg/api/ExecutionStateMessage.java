// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.common.msg.api;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ExecutionStateMessage extends GPTOMessage {

	private int executionID;

	private ExecutionState state;
	private String strategyName;
	private String scenarioName;

	public ExecutionStateMessage() {
	}

	public ExecutionStateMessage(ExecutionState isReady, int executionID) {
		super();
		this.state = isReady;
		this.executionID = executionID;
	}

	public ExecutionState getState() {
		return state;
	}

	public int getExecutionID() {
		return executionID;
	}

	public String getStrategyName() {
		return strategyName;
	}

	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
	}

	public String getScenarioName() {
		return scenarioName;
	}

	public void setScenarioName(String scenarioName) {
		this.scenarioName = scenarioName;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(state);
		out.writeInt(executionID);
		out.writeUTF(strategyName);
		out.writeUTF(scenarioName);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		state = (ExecutionState) in.readObject();
		executionID = in.readInt();
		strategyName = in.readUTF();
		scenarioName = in.readUTF();
	}

	@Override
	public String toString() {
		return "ExecutionStateMessage [executionID=" + executionID + ", state=" + state + ", strategyName=" + strategyName
				+ ", scenarioName=" + scenarioName + "]";
	}

}
