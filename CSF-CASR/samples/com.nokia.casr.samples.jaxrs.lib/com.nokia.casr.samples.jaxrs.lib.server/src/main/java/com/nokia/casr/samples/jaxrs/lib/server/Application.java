package com.nokia.casr.samples.jaxrs.lib.server;

import java.util.Hashtable;
import com.nokia.as.osgi.launcher.OsgiLauncher;
import com.nokia.casr.samples.jaxrs.lib.starter.Starter;
import org.apache.log4j.Logger;

public class Application {
        final static Logger _log = Logger.getLogger(Application.class);

	public static void main(String[] args) throws Exception {
		_log.warn("Starting Diameter Server");
		Starter starter = new Starter();
		OsgiLauncher launcher = starter.getOsgiLauncher();

		Hello hello = new Hello();
		Hashtable props = new Hashtable();
		props.put("foo", "bar");
		launcher.registerService(Object.class, hello, props);

		MyResource myResource = new MyResource();
		props = new Hashtable();
		props.put("foo", "bar");
		launcher.registerService(Object.class, myResource, props);

		_log.warn("Started");
		Thread.sleep(Integer.MAX_VALUE);
	}

}
