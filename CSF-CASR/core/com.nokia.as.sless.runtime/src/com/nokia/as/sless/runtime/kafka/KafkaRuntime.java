package com.nokia.as.sless.runtime.kafka;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.log4j.Logger;

import com.nokia.as.k8s.sless.fwk.runtime.ExecConfig;
import com.nokia.as.k8s.sless.fwk.runtime.FunctionContext;
import com.nokia.as.k8s.sless.fwk.runtime.SlessRuntime;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventBuilder;

@Component
public class KafkaRuntime implements SlessRuntime {
	private static final String RUNTIME_TYPE_KAFKA = "kafka";
	private static final String SPEC_KAFKA = "kafka.";

	static final Logger log = Logger.getLogger("sless.runtime.kafka");

	@ServiceDependency(service = FunctionContext.class, filter = "(type=" + RUNTIME_TYPE_KAFKA
			+ ")", required = false, removed = "unbindFunction")
	public void bindFunctionToConsumer(FunctionContext function, Map<String, String> properties) throws Exception {
		log.info(this + " binding kafka consumer to function " + function + " with properties " + properties);
		ClassLoader currentThread = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(StringSerializer.class.getClassLoader());
			execConsumer(function, properties); // use the client inside this block
		} catch (Throwable e) {
			log.error(this + "", e);
		} finally {
			Thread.currentThread().setContextClassLoader(currentThread);
		}
	}

	@SuppressWarnings("unchecked")
	private void execConsumer(FunctionContext function, Map<String, String> properties) {

		new Thread(() -> {
			Map<String, Object> spec = function.route().route.paramsAsMap();
			if (spec == null)
				spec = new HashMap<String, Object>();
			Map<String, Object> kafkaSpec = spec;
			String[] topics = ((String) kafkaSpec.get(SPEC_KAFKA + "subscribe")).split(",");
			spec.put("group.id", UUID.randomUUID().toString()); // for testing purposes only
			Long pollTimeout = (Long) kafkaSpec.get(SPEC_KAFKA + "poll");
			String confContentType = (String) spec.get("contentType");

			kafkaSpec.remove(SPEC_KAFKA + "subscribe");
			kafkaSpec.remove(SPEC_KAFKA + "poll");
			kafkaSpec.remove("contentType");

			Consumer<Long, String> consumer = new KafkaConsumer<>(kafkaSpec);
			consumer.subscribe(Arrays.asList(topics));
			boolean stop = false;
			while (!stop) { // TODO: add stop condition?

				// kafka polling
				ConsumerRecords<Long, String> consumerRecords = consumer.poll(pollTimeout);
				consumerRecords.forEach(record -> {
					CloudEventBuilder<String> cloudEventBuilder = new CloudEventBuilder<String>();
					cloudEventBuilder.contentType(confContentType);

					log.info(this + " read " + record);

					// binary mode mapping
					Headers headers = record.headers();
					Header contentTypeHeader = headers.lastHeader("cloudEvents_contentType");
					if (confContentType == null && contentTypeHeader != null)
						cloudEventBuilder.contentType(new String(contentTypeHeader.value()));

					// call function
					String functionId = function.route().function.name;
					CloudEvent<String> event = cloudEventBuilder.type(type())
																.id(String.valueOf(record.key()))
																.data(record.value())
																.source(URI.create(functionId))
																.build();
					CompletableFuture<CloudEvent> cf = new CompletableFuture<>();
					ExecConfig execConfig = new ExecConfig().cf(cf);
					function.exec(event, execConfig);
					cf.whenComplete((CloudEvent result, Throwable exception) -> {
						if (exception != null) {
							log.error(this + " function " + function + " canceled", exception);
						}
					});
				});
				log.info(this + " consumer polling " + topics);
			}
			consumer.close();
		}).start();
	}

	public void unbindFunction(FunctionContext function, Map<String, String> properties) {
		log.info(this + " unbinding function " + function);
	}

	@Override
	public String type() {
		return RUNTIME_TYPE_KAFKA;
	}
}
