package com.nokia.as.gpto.common.msg.api;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

public class MetricRegistryMessage extends GPTOMessage{
	private Map<String, AgentRegistration> registry;

	public MetricRegistryMessage() {
	}

	public MetricRegistryMessage(Map<String, AgentRegistration> registry) {
		super();
		this.registry = registry;
	}


	public Map<String, AgentRegistration> getRegistry() {
		return registry;
	}

	public void setRegistry(Map<String, AgentRegistration> registry) {
		this.registry = registry;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
		registry = (Map<String, AgentRegistration>) oi.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput oo) throws IOException {
		oo.writeObject(registry);
	}

	@Override
	public String toString() {
		return "MetricRegistryMessage [registry=" + registry.values() + "]";
	}
}
