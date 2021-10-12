// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.kafka.stest.consumer;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import org.apache.log4j.Logger;

public class KafkaEventConsumer {
	private final KafkaConsumer<String, String> consumer;
	private final String inputTopic;
        private final static Logger _log = Logger.getLogger(KafkaEventConsumer.class);

	public KafkaEventConsumer(String brokers, String groupId, String inputTopic, String outputTopic, String url) {
		this.consumer = new KafkaConsumer<String, String>(createConsumerConfig(brokers, groupId, url));
		this.inputTopic = inputTopic;
	}

	private Properties createConsumerConfig(String brokers, String groupId, String url) {
		Properties props = new Properties();
		props.put("bootstrap.servers", brokers);
		props.put("group.id", groupId);
		props.put("auto.commit.enable", "false");
		props.put("auto.offset.reset", "earliest");
		props.put("schema.registry.url", url);
		props.put("specific.avro.reader", true);
		props.put("key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class);
		props.put("value.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class);
		return props;
	}

	public void run() throws ExecutionException, InterruptedException {
		consumer.subscribe(Collections.singletonList(inputTopic));
		_log.warn("Reading topic:" + inputTopic);
		int events = 0;
		for (int count = 0; count < 10 && events < 10; count++) {
			ConsumerRecords<String, String> records = consumer.poll(1000);
			for (ConsumerRecord<String, String> record : records) {
				_log.warn("Received event: " + record.value());
				events++;
			}
			consumer.commitSync();

		}
		consumer.close();
		if (events != 10) {
		        _log.warn("test failed: have read " + events + " events.");
			throw new IllegalStateException("did not receive expected events: " + events);
		}
		_log.warn("test done: have read " + events + " events.");
	}

}
