// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.kafka.stest.consumer;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;

@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
public class KafkaConsumerTest {
	@ServiceDependency
	private LogServiceFactory logFactory;
	private LogService log;

	@Before
	public void initLog() {
		log = logFactory.getLogger(KafkaConsumerTest.class);
	}

	@Test
	public void testKafkaConsumer() {
		log.warn("test: waiting for some kafka events");
		
		ClassLoader current = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
	        // currently hardcoding a lot of parameters, for simplicity
	        String groupId = "AvroClicksSessionizer";
	        String inputTopic = "clicks";
	        String outputTopic = "sessionized_clicks";
	        String url = "http://localhost:8081";
	        String brokers = "localhost:9092";

	        KafkaEventConsumer sessionizer = new KafkaEventConsumer(brokers, groupId, inputTopic, outputTopic, url);
	        sessionizer.run();
	        System.out.println("Test done");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Thread.currentThread().setContextClassLoader(current);
		}
	}
}
