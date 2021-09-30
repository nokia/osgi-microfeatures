package com.nokia.conf.factory.dm;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

@Component
public class ExampleCall {
	
	@ServiceDependency(filter="(name=hi)")
	volatile Example conf;
	 
	@Start
	public void start() {
		System.out.println("Caller received : " + conf.name);
	}		
}
