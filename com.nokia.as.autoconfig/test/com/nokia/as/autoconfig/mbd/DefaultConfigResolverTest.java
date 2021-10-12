// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.mbd;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.nokia.as.autoconfig.ResolverTestBase;
import com.nokia.as.autoconfig.Utils;
import com.nokia.as.autoconfig.test.bundle.api.HelloService;

public class DefaultConfigResolverTest extends ResolverTestBase {
    
    @Test
    public void resolverTest() throws Exception {
        framework.withBundles(Utils.url(DEF_JAR).toString());
        framework = framework.start();
        HelloService helloService = framework.getService(HelloService.class)
                .get(5, TimeUnit.SECONDS);
        assertEquals("Hello English", helloService.sayHello("English"));
    }
    
}
