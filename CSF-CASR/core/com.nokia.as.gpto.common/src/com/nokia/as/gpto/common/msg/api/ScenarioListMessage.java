package com.nokia.as.gpto.common.msg.api;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

public class ScenarioListMessage extends GPTOMessage {

	private Set<String> scenarii;

	public ScenarioListMessage() {

	}

	public ScenarioListMessage(Set<String> scenarii) {
		super();
		this.scenarii = scenarii;
	}

	public Set<String> getScenarii() {
		return scenarii;
	}

	public void setScenarii(Set<String> scenarii) {
		this.scenarii = scenarii;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
		scenarii = (Set<String>) oi.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput oo) throws IOException {
		oo.writeObject(scenarii);
	}

	@Override
	public String toString() {
		return "ScenarioListMessage [scenarii=" + scenarii + "]";
	}

}
