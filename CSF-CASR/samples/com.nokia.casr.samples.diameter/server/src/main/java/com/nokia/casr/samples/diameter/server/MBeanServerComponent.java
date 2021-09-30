package com.nokia.casr.samples.diameter.server;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 *  Register in the OSGI service registry an mbean server to allow jmx management.
 * Aries JMX will use it to register any components that are registered with a jmx.objectname property.
 * (notice that for simplicity, we register the mbean server from this component, but you can do this from another
 * component, possibly from another bundle).
 */
@Component
public class MBeanServerComponent {
	
	@Activate
	void start(BundleContext ctx) {
		System.out.println("Registering MBean Server into OSGI service registry.");
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ctx.registerService(MBeanServer.class.getName(), mbs, null);
	}

}
