package com.nokia.conf.nontypesafe.ds;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Example {

	@Activate
	void start(Map<String, Object> config) {
		System.out.println("Example.start: " + config.get("addr"));
	}

}
