package com.nokia.casr.samples.diameter.server;

import java.lang.management.ManagementFactory;
import java.util.Hashtable;

import javax.management.MBeanServer;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.nextenso.proxylet.diameter.DiameterRequestProxylet;

@Component
public class Bootstrap {

	/**
	 * Our log4j logger.
	 */
	private final static Logger log = Logger.getLogger(Bootstrap.class);
	
	/**
	 * Spring application context.
	 */
	ApplicationContext ctx;
	
	/**
	 * Our bundle is staring. Initialize Spring and register our proxylet
	 */
	@Activate
	public void start(BundleContext bundleContext) {
		
		// We need to indicate to spring that it has to use our bundle classloader.
		// To do so, we temporarily set the thread context class loader to our bundle classloader.
		try {
			Thread.currentThread().setContextClassLoader(Bootstrap.class.getClassLoader());
			ctx = new AnnotationConfigApplicationContext(AppConfig.class);
		} finally {
			Thread.currentThread().setContextClassLoader(null);
		}
						
		// Create and register our proxylet in the OSGi service registry.
		TestServer pxlet = ctx.getBean(TestServer.class);
		log.warn("***************** Registering test server proxylet into the OSGI service registry: " + pxlet);
		Hashtable<String, Object> props = new Hashtable<>();
		props.put("jmx.objectname", "com.nokia.casr.samples.diameter:type=Server");
		bundleContext.registerService(DiameterRequestProxylet.class.getName(), pxlet, props);
	}
	
	/**
	 * Our bundle is stopping, deactivate spring (our proxylet will be automatically unregistered from the service registry).
	 */
	@Deactivate
	public void stop() {
		log.warn("***************** Shutting down spring");
		((ConfigurableApplicationContext) ctx).close();
	}
	
}
