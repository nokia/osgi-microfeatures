package com.nokia.as.logger.http;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.nokia.as.service.loghistory.common.RingBuffer;



public class RingBufferTest {
	
	AtomicInteger queueSize, littleSize;
	RingBuffer<String> small;
	RingBuffer<String> myqueue;
	ExecutorService executorSvc;

	public static StringBuilder getLogs(RingBuffer<String> buffer) {
		StringBuilder logs = new StringBuilder();
		buffer.stream().map(e -> {
					String more = e.length() > buffer.size()? "[...]": "";
					String entry = e.substring(0, Math.min(e.length(), buffer.size()));
					return (entry+more+ "\n");
	            }).collect(Collectors.<String> toList()).iterator().forEachRemaining(logs::append);
		return logs;
	}
	
    @Before
    public void setUp() throws Exception {
    	queueSize = new AtomicInteger(50);
    	littleSize = new AtomicInteger(3);
    	small = new RingBuffer<>(littleSize.get());
    	myqueue = new RingBuffer<>(queueSize.get());
    	executorSvc = Executors.newFixedThreadPool(8);
    	
    	small.put("test");
    }
    
    @After
    public void tearDown() {
    	executorSvc.shutdown();
    	try {
			executorSvc.awaitTermination(15, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
	
    
    @Test
    public void testSmallQueue() {
		assertEquals(1, small.size());
		small.put("test 1");
		small.put("test 2");
		small.put("test 3");
		assertEquals(littleSize.get(), small.size());
		assertEquals("test 1", small.element());
		
    }
    
    @Test
    public void testQueueOrder() {
    	RingBuffer<String> order = new RingBuffer<>(100);
    	IntStream.range(0, 900).forEach(i -> {
			executorSvc.submit(() -> {
				order.put(String.valueOf(i));
			});
		});
    	
		System.out.println(getLogs(order));
    }
    
    @Test(timeout=2000)
    public void testMultiThreadsCalls() {
		IntStream.range(0, 400).forEach(i -> {
			executorSvc.submit(() -> {
				IntStream.range(0, 800).forEach(c -> {
					myqueue.put("TEST "+String.valueOf(c));
				});
			});
		});
		executorSvc.shutdown();
		try {
			executorSvc.awaitTermination(2, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(queueSize.get(), myqueue.size());
    }
}
