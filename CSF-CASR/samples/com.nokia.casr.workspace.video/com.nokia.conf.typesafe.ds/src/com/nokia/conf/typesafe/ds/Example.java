package com.nokia.conf.typesafe.ds;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component
public class Example {

	@Activate
	public void start(MyConf conf) {
		System.out.println("example started: " + conf.myInt());
	}
}
