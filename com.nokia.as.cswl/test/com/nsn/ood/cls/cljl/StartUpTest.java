/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.cljl;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LoggingConfig;
import com.nokia.licensing.plugins.PluginRegistry;
import com.nsn.ood.cls.cljl.plugin.CLSPreferences;


/**
 * @author marynows
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
		CLSPreferences.class, PluginRegistry.class, LoggingConfig.class })
public class StartUpTest {
	private StartUp bean;

	@Before
	public void setUp() throws Exception {
		this.bean = new StartUp();

		mockStatic(CLSPreferences.class);
		mockStatic(PluginRegistry.class);
		mockStatic(LoggingConfig.class);
	}

	@Test
	public void testShutdown() throws Exception {
		LoggingConfig.shutdown();

		replayAll();
		this.bean.closeLoggers();
		verifyAll();
	}

	@Test
	public void testInit() throws Exception {
		CLSPreferences.importPreferences();
		expect(PluginRegistry.setCLJLPreferences(isA(CLSPreferences.class))).andReturn(true);
		LoggingConfig.initialize();

		replayAll();
		this.bean.init();
		verifyAll();
	}

	@Test
	public void testInitWithException() throws Exception {
		CLSPreferences.importPreferences();
		expect(PluginRegistry.setCLJLPreferences(isA(CLSPreferences.class))).andThrow(new LicenseException("message"));
		LoggingConfig.initialize();

		replayAll();
		this.bean.init();
		verifyAll();
	}
}
