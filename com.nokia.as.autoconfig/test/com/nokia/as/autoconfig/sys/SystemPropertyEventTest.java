// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.sys;

import static org.junit.Assert.assertEquals;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.nokia.as.autoconfig.Configuration;
import com.nokia.as.autoconfig.ResolverTestBase;
import com.nokia.as.autoconfig.Utils;
import com.nokia.as.autoconfig.test.bundle.api.HelloService;

public class SystemPropertyEventTest extends ResolverTestBase {
    
    private SystemPropertyResolver resolver = new SystemPropertyResolver();
    
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("com.nokia.as.autoconfig.test.bundle.def.HelloConfiguration:greeting.message", "Sawubona");
    }
    
    @Test
    public void refreshSystemPropertiesTest() throws Exception {
    	framework.withBundles(Utils.url(DEF_JAR).toString());
        framework = framework.start();
        HelloService helloService = framework.getService(HelloService.class)
                .get(5, TimeUnit.SECONDS);
        assertEquals("Sawubona South Africa", helloService.sayHello("South Africa"));
        
        System.setProperty("com.nokia.as.autoconfig.test.bundle.def.HelloConfiguration:greeting.message", "Halló");
        EventAdmin admin = framework.getService(EventAdmin.class).get(5, TimeUnit.SECONDS);
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("config.pid", "com.nokia.as.autoconfig.test.bundle.def.HelloConfiguration");
        admin.postEvent(new Event("com/nokia/casr/ConfigEvent/UPDATED", properties));
        TimeUnit.SECONDS.sleep(2);
        
        helloService = framework.getService(HelloService.class)
                .get(5, TimeUnit.SECONDS);
        assertEquals("Halló Iceland", helloService.sayHello("Iceland"));
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
        System.getProperties().remove("com.nokia.as.autoconfig.test.bundle.def.HelloConfiguration:greeting.message");
    }

}