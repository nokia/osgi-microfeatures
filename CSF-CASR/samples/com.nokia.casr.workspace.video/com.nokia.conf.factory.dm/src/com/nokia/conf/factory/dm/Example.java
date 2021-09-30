package com.nokia.conf.factory.dm;

import java.util.Dictionary;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.FactoryConfigurationAdapterService;
import org.apache.felix.dm.annotation.api.Start;

@FactoryConfigurationAdapterService(provides = Example.class, propagate=true)
public class Example {
	
	public String name;
	
	@Start
    void start() {
		
    }
	
    void updated(Dictionary<String, Object> conf) {
        name = (String) conf.get("name");
    }

} 