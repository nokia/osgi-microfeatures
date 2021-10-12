// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.cjdi.stest.client;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.junit.Before;
import org.junit.runner.RunWith;

import com.nokia.as.util.junit4osgi.OsgiJunitRunner;

/**
 * Sends an http request to the "MyProxylet" through the http io handler.
 */
@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
public class BasicClientSctpTest extends BasicClientTest {
	
	@Before
	public void before() {
		_log = _logFactory.getLogger(BasicClientSctpTest.class);
	}

}
