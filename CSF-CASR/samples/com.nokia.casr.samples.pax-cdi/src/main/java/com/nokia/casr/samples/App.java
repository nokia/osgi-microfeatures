package com.nokia.casr.samples;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.ops4j.pax.cdi.api.Component;
import org.ops4j.pax.cdi.api.Immediate;

/**
 * Hello world!
 */
@Component
@Immediate
public class App {
	
	final static Logger log = Logger.getLogger(App.class);

	@PostConstruct
	public void activate() {
		log.warn("Activated App");
	}
	
}
