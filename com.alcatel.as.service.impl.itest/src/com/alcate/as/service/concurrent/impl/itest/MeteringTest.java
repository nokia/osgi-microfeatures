package com.alcate.as.service.concurrent.impl.itest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.metering2.util.Meters;
import com.nokia.as.util.test.osgi.Ensure;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

@RunWith(MockitoJUnitRunner.class)
public class MeteringTest extends IntegrationTestBase {

	/**
	 * Helper used to check if important steps are executed in the right order.
	 */
	private final Ensure _ensure = new Ensure();
	
	/**
	 * Injected
	 */
	private volatile MeteringService _metering;
				
	/**
	 * We first need to be injected with the PlatformExecutors services.
	 */
	@Before
	public void before() {		
		component(comp -> comp.impl(this).start(_ensure::inc).withSvc(MeteringService.class, true));
		_ensure.waitForStep(1); // make sure our component is started.
	}

	/**
	 * Validates the processing thread pool.
	 */
	@Test
	public void loadMeteringService() throws InterruptedException {
	    MyMonitorable mon = new MyMonitorable("monitorable", "my monitorable");
	    Meter msgReceived = mon.createIncrementalMeter(_metering, "msg.received", null);
	    Meter rateMsgReceived = Meters.createRateMeter(_metering, msgReceived, 1000);
	    mon.addMeter(rateMsgReceived);
	    mon.stop();			
	    mon.getMeters().forEach((name, meter) -> Assert.assertTrue(meter.getJobs().isEmpty()));
	}
	
	public class MyMonitorable extends SimpleMonitorable {		
		public MyMonitorable(String monitorableName, String monitorableDesc) {
			super(monitorableName, monitorableDesc);
		}						
	}
		
}
