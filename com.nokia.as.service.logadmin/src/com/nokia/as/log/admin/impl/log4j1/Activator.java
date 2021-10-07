package com.nokia.as.log.admin.impl.log4j1;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.nokia.as.log.service.admin.LogAdmin;
import com.nokia.as.log.service.admin.LogHandler;

public class Activator implements BundleActivator, ServiceTrackerCustomizer<LogHandler, LogHandler> {

	private final Log4j1AdminImpl _logAdmin = new Log4j1AdminImpl();
	
	@Override
	public void start(BundleContext context) throws Exception {
		Hashtable<String, String> properties = new Hashtable<>();
		properties.put("type", "log4j1");
		context.registerService(LogAdmin.class, _logAdmin, properties);
		ServiceTracker<LogHandler, LogHandler> tracker = new ServiceTracker<>(context, LogHandler.class, this);
		tracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		_logAdmin.removeLogHandlers();
	}

	@Override
	public LogHandler addingService(ServiceReference<LogHandler> ref) {
		BundleContext bc = ref.getBundle().getBundleContext();
		LogHandler logHandler = (LogHandler) bc.getService(ref);
		Object format = ref.getProperty(LogHandler.FORMAT);
		boolean doFormat = format == null ? true : Boolean.valueOf(format.toString());
		_logAdmin.addLogHandler(logHandler, doFormat);
		return logHandler;
	}

	@Override
	public void modifiedService(ServiceReference<LogHandler> reference, LogHandler logHandler) {
	}

	@Override
	public void removedService(ServiceReference<LogHandler> reference, LogHandler logHandler) {
		_logAdmin.removeLogHandler(logHandler);
	}

}
