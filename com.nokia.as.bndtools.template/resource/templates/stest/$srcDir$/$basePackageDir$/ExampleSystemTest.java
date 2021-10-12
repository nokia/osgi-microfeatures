// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package $basePackageName$;

import static org.junit.Assert.assertTrue;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;

@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
public class ExampleSystemTest {
	@ServiceDependency
	private LogServiceFactory logFactory;
	private LogService log;
	
	@Before
	public void initLog() {
		log = logFactory.getLogger(ExampleSystemTest.class);
	}
	
	@Test
	public void testService() {
		log.info("Hello World");
		assertTrue(true);
	}
}
