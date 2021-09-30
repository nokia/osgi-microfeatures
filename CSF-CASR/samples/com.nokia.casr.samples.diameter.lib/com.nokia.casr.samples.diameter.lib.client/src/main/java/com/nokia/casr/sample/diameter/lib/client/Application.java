package com.nokia.casr.sample.diameter.lib.client;

import com.nokia.as.osgi.launcher.OsgiLauncher;
import com.nokia.casr.samples.diameter.lib.starter.Starter;

public class Application {

	public static void main(String[] args) throws Exception {
		System.out.println("Starting Diameter Client");
		Starter starter = new Starter();
		OsgiLauncher launcher = starter.getOsgiLauncher();
		DiameterLoader loader = new DiameterLoader(launcher);
		loader.start();
		Thread.sleep(Integer.MAX_VALUE);
	}

}
