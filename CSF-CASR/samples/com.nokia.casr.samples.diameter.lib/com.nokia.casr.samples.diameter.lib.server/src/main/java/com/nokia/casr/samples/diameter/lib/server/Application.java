package com.nokia.casr.samples.diameter.lib.server;

import com.nextenso.proxylet.diameter.DiameterRequestProxylet;
import com.nokia.as.osgi.launcher.OsgiLauncher;
import com.nokia.casr.samples.diameter.lib.starter.Starter;

public class Application {

	public static void main(String[] args) throws Exception {
		System.out.println("Starting Diameter Server");
		Starter starter = new Starter();
		OsgiLauncher launcher = starter.getOsgiLauncher();

		TestServer server = new TestServer();
		launcher.registerService(DiameterRequestProxylet.class, server);
		Thread.sleep(Integer.MAX_VALUE);
	}

}
