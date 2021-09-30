package com.nokia.as.jaxrs.jersey.stest;

import org.apache.felix.dm.annotation.api.Component;

@Component(provides = ExampleClassForInjection.class)
public class ExampleClassForInjection {

	public String getInfo() {
		return "info";
	}
}