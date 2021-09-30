package com.nokia.casr.sample.diameter.springboot.client;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nokia.as.osgi.launcher.OsgiLauncher;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class allows to start a diameter load using rest interface.
 */
@RestController
public class DiameterLoaderController {

	/**
	 * This service is the bridge between springboot world and CASR osgi world.
	 * Using this service, you can then obtain CASR services, or register your
	 * springboot classes as osgi services.
	 */
	@Autowired
	private OsgiLauncher _launcher;

	/**
	 * Our Diameter Loader.
	 */
	private DiameterLoader _loader;

	@RequestMapping("/start")
	public synchronized String start() throws InterruptedException, ExecutionException, TimeoutException {
		if (_loader != null) {
			return "Diameter loader already started";
		}
		_loader = new DiameterLoader(_launcher);
		_loader.start();
		return "DiameterLoader started";
	}

	@RequestMapping("/stop")
	public synchronized String stop() {
		if (_loader != null) {
			_loader.stop();
			_loader = null;
		}
		return "DiameterLoader stopped";
	}

}
