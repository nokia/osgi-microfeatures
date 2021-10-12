// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.common.msg.api;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ControlScenarioRunMessage extends GPTOMessage {

	public enum Operation {
		START, STOP;
	}

	private Operation op;
	private int executionID;

	public ControlScenarioRunMessage() {
	}

	public ControlScenarioRunMessage(Operation op, int executionID) {
		super();
		this.op = op;
		this.executionID = executionID;
	}

	public int getExecutionID() {
		return executionID;
	}

	public void setExecutionID(int executionID) {
		this.executionID = executionID;
	}

	public Operation getOp() {
		return op;
	}

	public void setOp(Operation op) {
		this.op = op;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(executionID);
		out.writeObject(op);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		executionID = in.readInt();
		op = (Operation) in.readObject();
	}

	@Override
	public String toString() {
		return "ControlScenarioRunMessage [op=" + op + ", executionID=" + executionID
				+ "]";
	}

}
