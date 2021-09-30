package com.nokia.as.kafka.stest.producer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class KafkaEventProducer {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        if (args.length != 2) {
            System.out.println("Please provide command line arguments: numEvents schemaRegistryUrl");
            System.exit(-1);
        }
        long events = Long.parseLong(args[0]);
        String schemaUrl = args[1];

        Properties props = new Properties();
        // hardcoding the Kafka server URI for this example
        props.put("bootstrap.servers", "localhost:9092");
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("key.serializer",   org.apache.kafka.common.serialization.StringSerializer.class);
        props.put("value.serializer", org.apache.kafka.common.serialization.StringSerializer.class);
        props.put("schema.registry.url", schemaUrl);
        // Hard coding topic too.
        String topic = "clicks";

        Producer<String, String> producer = new KafkaProducer<>(props);
        for (long nEvents = 0; nEvents < events; nEvents++) {
            // Using IP as key, so events from same IP will go to same partition
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, "value-" + nEvents);
            producer.send(record);
        }
        producer.close();
    }
}

