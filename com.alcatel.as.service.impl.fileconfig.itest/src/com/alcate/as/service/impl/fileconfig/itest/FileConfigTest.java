// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcate.as.service.impl.fileconfig.itest;

import static org.junit.Assert.assertEquals;

import java.io.OutputStreamWriter;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.nokia.as.util.test.osgi.Ensure;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

@RunWith(MockitoJUnitRunner.class)
public class FileConfigTest extends IntegrationTestBase {

	/**
	 * Helper used to check if important steps are executed in the right order.
	 */
	private final Ensure _ensure = new Ensure();
	
	private final static String PID = "fileconfig";
	
	MyComponent myComp;
			
	/**
	 * We first need to be injected with the PlatformExecutors services.
	 */
	@Before
	public void before() {		
		// Set up console loggers
		ConsoleAppender ca = new ConsoleAppender();
		ca.setWriter(new OutputStreamWriter(System.out));
		ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
		org.apache.log4j.Logger.getRootLogger().addAppender(ca);
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.WARN);
		
		myComp = new MyComponent();
		component(comp -> comp
				.impl(myComp)
				.start(_ensure::inc)
				.withCnf(cnf -> cnf.update(Config.class, MyComponent::updated).pid(PID).required())
				.withSvc(EventAdmin.class, true));		
	}

	/**
	 * Validates the processing thread pool.
	 */
	@Test
	public void testFileConfig() throws InterruptedException {
		// make sure our component is started and configured
		_ensure.waitForStep(2); 
		assertEquals("password1", myComp.getPassword());

		// change system property for pwsd variable
		System.getProperties().setProperty("pswd", "password2");
		EventAdmin eventAdmin = myComp.getEventAdmin();
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put("config.pid", PID);
		Event event = new Event("com/nokia/casr/ConfigEvent/UPDATED", properties);
		eventAdmin.sendEvent(event);
		
		// check if change have been propagated
		_ensure.waitForStep(3); 
		
		// check if password has changed
		assertEquals("password2", myComp.getPassword());
	}
	
    public static interface Config {
        public String getPassword();
    }

	public class MyComponent {
		private String _password;
		private EventAdmin _eventAdmin;
		
		void updated(Config cnf) {
			_ensure.inc();
			_password = cnf.getPassword();
			System.out.println("MyFactoryComponent.updated: password=" + _password);
		}
		
		String getPassword() {
			return _password;
		}
		
		EventAdmin getEventAdmin() {
			return _eventAdmin;
		}
	}
	
}
