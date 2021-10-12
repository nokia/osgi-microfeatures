// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.common.msg.api;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

public class StrategyListMessage extends GPTOMessage {

	private Set<String> strategies;

	public StrategyListMessage() {

	}

	public StrategyListMessage(Set<String> strategies) {
		super();
		this.strategies = strategies;
	}

	public Set<String> getStrategies() {
		return strategies;
	}

	public void setStrategies(Set<String> strategies) {
		this.strategies = strategies;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
		strategies = (Set<String>) oi.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput oo) throws IOException {
		oo.writeObject(strategies);
	}

	@Override
	public String toString() {
		return "StrategyListMessage [strategies=" + strategies + "]";
	}
}
