package com.nokia.as.jaxrs.jersey.sless.stest;

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
public class KafkaWriteFunction implements Function {
	private Map<String, Object> _properties;

	@Start
	public Map<String, Object> start() {
		_properties = new HashMap<String, Object>();
		_properties.put(Function.PROP_NAME, "kafka-write");
		return _properties;
	}

	@Override
	public CompletableFuture<CloudEvent> apply(CloudEvent event, EventContext ctx) {
		System.out.println(this + " APPLY ctx = " + ctx);
		CloudEvent<String> data = new CloudEventBuilder<String>()	.type("gogo")
																	.data("Writing to Kafka 'demo' topic done.")
																	.id(event.getId())
																	.source(event.getSource())
																	.build();

		System.out.println(this + " data = " + data);
		return CompletableFuture.completedFuture(data);
	}
}