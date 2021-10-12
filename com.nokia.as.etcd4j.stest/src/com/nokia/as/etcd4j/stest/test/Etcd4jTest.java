// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.etcd4j.stest.test;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;

import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdKeysResponse;
import java.net.URI;
import static org.junit.Assert.assertEquals;

@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
public class Etcd4jTest {
	@ServiceDependency
	private LogServiceFactory logFactory;
	private LogService log;

	@Before
	public void initLog() {
		log = logFactory.getLogger(Etcd4jTest.class);
	}

	@Test
	public void testEtcd4j() {
		log.warn("testing etcd4");

		try (EtcdClient etcd = new EtcdClient(new URI("http://127.0.0.1:2379"))) {
			// Logs etcd version
			log.warn(etcd.getVersion());

			EtcdKeysResponse response = etcd.put("foo", "bar").send().get();
			// Prints out: bar
			log.warn("ETCD response: " + response.node.value);
			assertEquals("bar", response.node.value);			
		} catch (Exception e) {
			log.warn("test failed",  e);
		}
	}
}
