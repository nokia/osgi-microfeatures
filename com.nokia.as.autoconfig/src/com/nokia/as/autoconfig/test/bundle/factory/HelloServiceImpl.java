// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

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
