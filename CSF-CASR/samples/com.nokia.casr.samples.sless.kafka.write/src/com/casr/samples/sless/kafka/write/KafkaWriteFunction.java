package com.casr.samples.sless.kafka.write;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringSerializer;

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

		ClassLoader currentThread = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(StringSerializer.class.getClassLoader());
			execProducer(); // use the client inside this block
		} catch (Throwable e) {
			System.out.println(this + "\u001B[33m e = " + e + "\u001B[0m");
		} finally {
			Thread.currentThread().setContextClassLoader(currentThread);
		}
		CloudEvent<String> data = new CloudEventBuilder<String>()	.type("gogo")
																	.data("Writing to Kafka 'demo' topic done.")
																	.id(event.getId())
																	.source(event.getSource())
																	.build();
		return CompletableFuture.completedFuture(data);
	}

	private void execProducer() {
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringSerializer");
//		System.out.println(this + "\u001B[33m ctx.properties() = " + ctx.properties() + "\u001B[0m"); TODO: get kafka conf from ctx.properties
		Producer<Long, String> producer = new KafkaProducer<>(props);
		ProducerRecord<Long, String> record = new ProducerRecord<Long, String>("demo", "Hello World " + new Date());
		Headers headers = record.headers();
		headers.add("ExampleHeader", "headerValue".getBytes());
		headers.add("cloudEvents_contentType", "contentTypeValue".getBytes());

		try {
			producer.send(record).get();
		} catch (ExecutionException | InterruptedException e) {
			System.out.println(this + "\u001B[33m error in sending record = " + e + "\u001B[0m");
		} finally {
			producer.close();
		}

	}
}