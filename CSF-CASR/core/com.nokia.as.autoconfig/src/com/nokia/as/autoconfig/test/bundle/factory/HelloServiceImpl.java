package com.nokia.as.autoconfig.test.bundle.factory;

import org.apache.felix.dm.annotation.api.Component;

import com.nokia.as.autoconfig.test.bundle.api.HelloService;

@Component(factoryPid="com.nokia.as.autoconfig.test.bundle.factory.HelloConfiguration", propagate = true)
public class HelloServiceImpl implements HelloService {
    
    private String greeting;
    
    public void updated(HelloConfiguration conf) {
        greeting = conf.getGreetingMessage();
    }
    
    public String sayHello(String name) {
        return greeting + " " + name;
    }
    
}
