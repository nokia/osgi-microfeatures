package com.alcatel.as.service.concurrent.impl;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.service.metering2.MeteringService;

@Component(configurationPid="com.alcatel.as.service.concurrent.impl.PlatformExecutorsImpl", 
		   configurationPolicy=ConfigurationPolicy.OPTIONAL)
public class Activator {
	
	@Reference
	private volatile MeteringService _metering; // injected

	private volatile BundleContext _bctx; 
	private volatile Map<String, Object> _cnf;
	private volatile PlatformExecutorsImpl _pfExecsImpl;
	private volatile WheelTimerServiceImpl _wheelTimerImpl;
	private volatile JdkTimerServiceImpl _jdkTimerImpl;
	private volatile GogoShell _shell;

	private ServiceRegistration<ScheduledExecutorService> _processingThreadPoolReg;
	private ServiceRegistration<ScheduledExecutorService> _blockingThreadPoolReg;
	private ServiceRegistration<Object> _shellReg;
	
	@Activate
	void start(Map<String, Object> cnf, BundleContext bctx) {
		_bctx = bctx;
		_cnf = cnf;
		
		// Define the metering service for our PlatformExecutors service
		
		Meters meters = new Meters();
		meters.bindMeteringService(_metering);
		meters.start(_bctx);
		
		// Define the three implementations being part of the PlatformExecutors service.
		
		_jdkTimerImpl = new JdkTimerServiceImpl();
		_wheelTimerImpl = new WheelTimerServiceImpl();
		_pfExecsImpl = new PlatformExecutorsImpl();
		
		// Bind the implementations
		
		_jdkTimerImpl.setPlatformExecutors(_pfExecsImpl);	
		_jdkTimerImpl.bindMeters(meters);
		
		_wheelTimerImpl.setPlatformExecutors(_pfExecsImpl);	
		_wheelTimerImpl.bindMeters(meters);
		
		_pfExecsImpl.bindTimerService(_jdkTimerImpl);
		_pfExecsImpl.bindMeters(meters);
		
		// Start the implementations
		
		_pfExecsImpl.start(_cnf);		
		_wheelTimerImpl.start();
		_jdkTimerImpl.start(_cnf);
		
		// Register our PlatformExecutors service
		
		_bctx.registerService(PlatformExecutors.class , _pfExecsImpl, null);
		
		// Register our jdk (strict) timer service
		
		Hashtable<String, Object> props = new Hashtable<>();
		props.put(TimerService.STRICT, "true");
		String[] services = new String[] { TimerService.class.getName(), ScheduledExecutorService.class.getName() };
		_bctx.registerService(services, _jdkTimerImpl, props);
		
		// Register our wheel timer service
		
		props = new Hashtable<>();
		props.put(TimerService.STRICT, "false");
		services = new String[] { TimerService.class.getName(), ScheduledExecutorService.class.getName() };
		_bctx.registerService(services, _wheelTimerImpl, props);	
		
		// Register the processsing threadpool
		props = new Hashtable<>();
		props.put("type", "casr.processing");
		_processingThreadPoolReg = _bctx.registerService(ScheduledExecutorService.class, _pfExecsImpl.getProcessingThreadPoolExecutor(), props);

		// Register the blocking threadpool
		props = new Hashtable<>();
		props.put("type", "casr.blocking");
		_blockingThreadPoolReg = _bctx.registerService(ScheduledExecutorService.class, _pfExecsImpl.getIOThreadPoolExecutor(), props);
		
		// Register our Gogo Shell
		_shell = new GogoShell(_pfExecsImpl);
		_shell.start(cnf);
		props = new Hashtable<>();
		props.put("osgi.command.scope", "casr.service.concurrent");
		props.put("osgi.command.function", new String[] { "diag", "block" });
		_shellReg = _bctx.registerService(Object.class, _shell, props);
	}
	
	@Modified
	void modified(Map<String, Object> cnf) {
		_pfExecsImpl.modified(cnf);
		_shell.modified(cnf);
	}
	
	@Deactivate
	void stop() {
		ignoreException(() -> _processingThreadPoolReg.unregister());
		ignoreException(() -> _blockingThreadPoolReg.unregister());
		ignoreException(() -> _shellReg.unregister());		
		_wheelTimerImpl.stop();
		_jdkTimerImpl.stop();
		_shell.stop();
	}

	private void ignoreException(Runnable task) {
		try {
			task.run();
		} catch (Exception e) {}
	}
}
