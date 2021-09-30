package com.nokia.as.kafka.stest.producer;

import org.osgi.framework.*;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext ctx) {
		System.out.println("Starting kafka avro producer");
		try {
			KafkaEventProducer.main(new String[] { "10", "http://localhost:8081" });
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void stop(BundleContext ctx) {
	}

}
