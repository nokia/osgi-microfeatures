package com.nokia.conf.factory.ds;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class ExampleCallService {

	@Reference(target="(name=firstConf)")
	Example example;
	
	@Activate
	public void activate() {
		System.out.println("Name in the component calling the service = " + example.name);
	}
}
