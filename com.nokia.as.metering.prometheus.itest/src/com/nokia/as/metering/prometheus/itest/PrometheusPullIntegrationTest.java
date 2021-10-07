package com.nokia.as.metering.prometheus.itest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.metering2.ValueSupplier;
import com.nokia.as.metering.prometheus.pull.test.api.TestPullMetersTracker;
import com.nokia.as.util.test.osgi.Ensure;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

@RunWith(MockitoJUnitRunner.class)
public class PrometheusPullIntegrationTest extends IntegrationTestBase {

	private final Ensure _ensure = new Ensure();
	private final CountDownLatch latch = new CountDownLatch(1);
	private volatile MeteringService meteringService;
	private volatile TestPullMetersTracker metersTracker;
	private Meter absoluteMeter, counterMeter, counterMeter2, valueSupplied;
	
	private volatile long suppliedMeterValue;
	
	@Before
	public void before() {
		component(comp -> comp.impl(this).start(_ensure::inc)
				.withSvc(MeteringService.class, true)
				.withSvc(TestPullMetersTracker.class, true));
		_ensure.waitForStep(1);
		
		registerTestMonitorable(meteringService);
		
		metersTracker.forceExportMetrics("exportMeters -m test_monitorable_" 
			+ hashCode() + " -mts *\n" +
			"exportMeters -m test_monitorable_" + hashCode() + " -mt counter -t GAUGE " + 
			"-a counter_overriden_" + hashCode() + " \n" +
			"exportMeters -m test_monitorable_" + hashCode() + " -mt counter -a test_counter_label_" + hashCode()  + " -lb test1=a -lb test2=b \n"
		  + "exportMeters -m test_monitorable_" + hashCode() + " -mt counter2 -a test_counter_label_" + hashCode()  + " -lb test1=c -lb test2=d \n",
				Collections.emptyList(), 
				100);
		try {
			latch.await();
			Thread.sleep(2_000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void registerTestMonitorable(MeteringService meteringService) {
		suppliedMeterValue = 12;
		
		SimpleMonitorable smon = new SimpleMonitorable("test_monitorable_" + hashCode(),
				"Test Monitorable");
		absoluteMeter = smon.createAbsoluteMeter(meteringService, "absolute");
		absoluteMeter.set(4);
		counterMeter = smon.createIncrementalMeter(meteringService, "counter", null);
		counterMeter2 = smon.createIncrementalMeter(meteringService, "counter2", null);
		valueSupplied = smon.createValueSuppliedMeter(meteringService, "supplied", new ValueSupplier() {
			
			@Override
			public long getValue() {
				latch.countDown();
				return suppliedMeterValue * 2;
			}
		});
		
		smon.start(_context);
		counterMeter.inc(1);
		counterMeter2.inc(1);
	}
	
	
	@Test
	public void testMeterValues() throws Exception {
		Map<String, Double> meters = parsePrometheusMetrics(metersTracker.getMetrics());
		Assert.assertTrue(meters.get("test_monitorable_" + hashCode() + "_absolute").equals(4.0D));
		Assert.assertTrue(meters.get("test_monitorable_" + hashCode() + "_counter").equals(1.0D));
		Assert.assertTrue(meters.get("test_monitorable_" + hashCode() + "_supplied").equals(24.0D));
		Assert.assertTrue(meters.get("counter_overriden_" + hashCode()).equals(1.0D));
		Assert.assertTrue(meters.get("test_counter_label_" + hashCode() + "{test1=\"a\",test2=\"b\",}").equals(1.0D));


		suppliedMeterValue = 20;
		absoluteMeter.set(8);
		counterMeter.inc(5);
		
		try {
			Thread.sleep(1_000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		meters = parsePrometheusMetrics(metersTracker.getMetrics());

		Assert.assertTrue(meters.get("test_monitorable_" + hashCode() + "_absolute").equals(8.0D));
		Assert.assertTrue(meters.get("test_monitorable_" + hashCode() + "_counter").equals(6.0D));
		Assert.assertTrue(meters.get("test_monitorable_" + hashCode() + "_supplied").equals(40.0D));
		Assert.assertTrue(meters.get("counter_overriden_" + hashCode()).equals(6.0D));
		Assert.assertTrue(meters.get("test_counter_label_" + hashCode() + "{test1=\"a\",test2=\"b\",}").equals(6.0D));
	}
	
	@Test
	public void testMeterType() throws Exception {
		String rawMetrics = metersTracker.getMetrics();
		Map<String, String> types = parsePrometheusType(rawMetrics);
		System.out.println(rawMetrics);
		Assert.assertTrue(types.get("test_monitorable_" + hashCode() + "_counter").equals("counter"));
		Assert.assertTrue(types.get("test_monitorable_" + hashCode() + "_absolute").equals("gauge"));
		Assert.assertTrue(types.get("test_monitorable_" + hashCode() + "_supplied").equals("gauge"));
		Assert.assertTrue(types.get("counter_overriden_" + hashCode()).equals("gauge"));

	}
	
	@Test
	public void testSimpleCounter() throws Exception {
		Map<String, Double> meters = parsePrometheusMetrics(metersTracker.getMetrics());
		counterMeter.inc(5);
		counterMeter.getAndReset();
		try {
			Thread.sleep(1_000L);
			meters = parsePrometheusMetrics(metersTracker.getMetrics());
			Assert.assertTrue(meters.get("test_monitorable_" + hashCode() + "_counter").equals(0.0D));
	
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		counterMeter.inc(5);
		counterMeter.dec(5);
		try {
			Thread.sleep(1_000L);
			meters = parsePrometheusMetrics(metersTracker.getMetrics());
			Assert.assertTrue(meters.get("test_monitorable_" + hashCode() + "_counter").equals(0.0D));
	
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
	
	@Test
	public void testLabeledCounter() throws Exception {
		Map<String, Double> meters = parsePrometheusMetrics(metersTracker.getMetrics());
		Assert.assertTrue(meters.get("test_monitorable_" + hashCode() + "_counter").equals(1.0D));
		Assert.assertTrue(meters.get("test_counter_label_" + hashCode() + "{test1=\"a\",test2=\"b\",}").equals(1.0D));

		
		counterMeter.inc(10L);
		counterMeter2.inc(3L);
		expectCounterValue(11.0D, 4.0D, "counterMeter.inc(10L) & counterMeter2.inc(3L)");
		
		counterMeter.dec(3L);
		counterMeter2.dec(2L);
		expectCounterValue(8.0D, 2.0D, "counterMeter.dec(2L) & counterMeter2.dec(2L)");
		
		counterMeter.getAndReset();
		expectCounterValue(0.0D, 2.0D, "counterMeter.getAndReset() & counterMeter2 = 2");
		
		
		counterMeter.inc(1);
		counterMeter2.inc(5);
		counterMeter.dec(1);
		expectCounterValue(0.0D, 7.0D, "counterMeter.inc(1) & counterMeter2.inc(5) & counterMeter.dec(1);");
	}
	
	private void expectCounterValue(Double l1, Double l2, String testing) {
		try {
			Thread.sleep(1_000L);
		
			Map<String, Double> meters = parsePrometheusMetrics(metersTracker.getMetrics());
			System.out.println("\ntest_counter_label_" + hashCode() + "{test1=\"a\",test2=\"b\",}="+meters.get("test_counter_label_" + hashCode() + "{test1=\"a\",test2=\"b\",}")
					+ "\ntest_counter_label_" + hashCode() + "{test1=\"c\",test2=\"d\",}="+meters.get("test_counter_label_" + hashCode() + "{test1=\"c\",test2=\"d\",}"));
			System.out.println("counter1 = " + counterMeter.getValue() + "   &   counter2 = " + counterMeter2.getValue());
			System.out.println("Testing: "+ testing+ "/ Expecting counter1="+l1+" & counter2 ="+l2);
			Assert.assertTrue(meters.get("test_counter_label_" + hashCode() + "{test1=\"a\",test2=\"b\",}").equals(l1));
			Assert.assertTrue(meters.get("test_counter_label_" + hashCode() + "{test1=\"c\",test2=\"d\",}").equals(l2));
	
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private Map<String, Double> parsePrometheusMetrics(String meters) {
		Map<String, Double> map = new HashMap<>();
		System.out.println(meters);
		
		Arrays.stream(meters.split("\n"))
			.filter(line -> !line.startsWith("#"))
			.forEach(line -> {
				String[] tokens = line.split(" ");
				map.put(tokens[0], Double.parseDouble(tokens[1]));
			});
		
		return map;
	}
	
	private Map<String, String> parsePrometheusType(String meters) {
		Map<String, String> map = new HashMap<>();
		
		Arrays.stream(meters.split("\n"))
			.filter(line -> line.startsWith("# TYPE"))
			.forEach(line -> {
				String[] tokens = line.split(" ");
				Assert.assertTrue("unexpected format for line " + line, tokens.length == 4);
				map.put(tokens[2], tokens[3]);
			});
		
		return map;
	}
	
	
}
