package com.nokia.conf.nontypesafe.dm;

import java.util.Dictionary;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Start;

@Component
public class Example {

	@Start
	void start() {
		System.out.println("Example.start");
	}

	@ConfigurationDependency()
	void updated(Dictionary<String, Object> conf) {
		String foo = (String) conf.get("foo");
		System.out.println("Example.updated: " + foo);
	}
}
