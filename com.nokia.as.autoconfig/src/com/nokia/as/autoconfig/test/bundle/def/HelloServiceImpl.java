package com.nokia.as.autoconfig.test.bundle.def;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;

import com.nokia.as.autoconfig.test.bundle.api.HelloService;

@Component
public class HelloServiceImpl implements HelloService {
    
    private String greeting;
    
    @ConfigurationDependency
    public void updated(HelloConfiguration conf) {
        greeting = conf.getGreetingMessage();
    }
    
    public String sayHello(String name) {
        return greeting + " " + name;
    }
    
}
