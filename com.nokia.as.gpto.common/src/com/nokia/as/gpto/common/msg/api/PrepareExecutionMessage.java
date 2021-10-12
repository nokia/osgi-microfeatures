// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.common.msg.api;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class PrepareExecutionMessage extends GPTOMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6563606592490417003L;

	public int executionID;

	/**
	 * Duration in seconds
	 */
	public long duration;

	public String scenarioName;

	public String strategyName;

	public String injectionStrategyJson;
	public String scenarioJson;

	public PrepareExecutionMessage() {
		super();
	}

	public PrepareExecutionMessage(int executionID, String scenarioName, String strategyName) {
		super();
		this.executionID = executionID;
		this.scenarioName = scenarioName;
		this.strategyName = strategyName;
	}

	public String getScenarioName() {
		return scenarioName;
	}

	public void setScenarioName(String scenarioName) {
		this.scenarioName = scenarioName;
	}

	public int getExecutionID() {
		return executionID;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		executionID = in.readInt();
		scenarioName = in.readUTF();
		strategyName = in.readUTF();
		injectionStrategyJson = in.readUTF();
		scenarioJson = in.readUTF();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(executionID);
		out.writeUTF(scenarioName);
		out.writeUTF(strategyName);
		out.writeUTF(injectionStrategyJson);
		out.writeUTF(scenarioJson);
	}

	public String getStrategyName() {
		return strategyName;
	}

	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
	}

	public String getInjectionStrategyJson() {
		return injectionStrategyJson;
	}

	public void setInjectionStrategyJson(String injectionStrategyJson) {
		this.injectionStrategyJson = injectionStrategyJson;
	}

	@Override
	public String toString() {
		return "PrepareExecutionMessage [executionID=" + executionID  + ", scenarioName="
				+ scenarioName + ", strategyName=" + strategyName + ", injectionStrategyJson=" + injectionStrategyJson
				+ ", scenarioJson=" + scenarioJson + "]";
	}

	public String getScenarioJson() {
		return scenarioJson;
	}

	public void setScenarioJson(String scenarioJson) {
		this.scenarioJson = scenarioJson;
	}
}
