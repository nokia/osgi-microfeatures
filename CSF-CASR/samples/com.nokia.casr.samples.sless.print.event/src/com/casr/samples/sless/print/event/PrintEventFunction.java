package com.casr.samples.sless.print.event;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Start;

import com.nokia.as.k8s.sless.EventContext;
import com.nokia.as.k8s.sless.Function;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventBuilder;

@Component(provides = Function.class, propagate = true)
public class PrintEventFunction implements Function {
	private Map<String, Object> _properties;

	@Start
	public Map<String, Object> start() {
		_properties = new HashMap<String, Object>();
		_properties.put(Function.PROP_NAME, "print");
		return _properties;
	}

	@Override
	public CompletableFuture<CloudEvent> apply(CloudEvent event, EventContext ctx) {
		System.out.println(this + "\u001B[33m Printing event = " + event + "\u001B[0m");
		return CompletableFuture.completedFuture(event);
	}
}