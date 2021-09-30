package com.nokia.conf.factory.ds;

import java.util.Map;

import org.osgi.service.component.annotations.*;

@Component(service = Example.class)
public class Example {
	
	public String name;

	@Activate
	public void start(Map<String, Object> conf) {
		System.out.println("Test component started with conf " + conf.get("name"));
		name = (String) conf.get("name"); 
	}
	
	@Deactivate
	public void stop() {
		System.out.println("Example service was stopped");
	}

}
 