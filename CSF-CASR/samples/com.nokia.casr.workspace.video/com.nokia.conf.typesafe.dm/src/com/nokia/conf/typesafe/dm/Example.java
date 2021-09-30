package com.nokia.conf.typesafe.dm;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Start;

@Component
public class Example {

	@Start
	void start() {
		System.out.println("Example.start");
	}

	@ConfigurationDependency(required=true)
	void updated(MyConf conf) {
		System.out.println("Example: " + conf.foo());
	}
}
