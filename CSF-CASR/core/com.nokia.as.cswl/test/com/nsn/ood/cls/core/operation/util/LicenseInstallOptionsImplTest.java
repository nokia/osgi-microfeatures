/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.util;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.platform.PlatformPreferences;
import com.nsn.ood.cls.core.security.BasicPrincipal;


/**
 * @author marynows
 * 
 */
public class LicenseInstallOptionsImplTest {

	private LicenseInstallOptionsImpl options;

	@Before
	public void setUp() throws Exception {
		this.options = new LicenseInstallOptionsImpl();
	}

	@Test
	public void testIsForce() throws Exception {
		assertTrue(this.options.isForce());
	}

	@Test
	public void testGetTargetId() throws Exception {
		final PlatformPreferences platformPreferencesMock = createMock(PlatformPreferences.class);

		expect(platformPreferencesMock.getTargetId()).andReturn("1234");

		replayAll();
		setInternalState(this.options, platformPreferencesMock);
		assertEquals("1234", this.options.getTargetId());
		verifyAll();
	}

	@Test
	public void testGetUsername() throws Exception {
		final BasicPrincipal basicPrincipalMock = createMock(BasicPrincipal.class);

		expect(basicPrincipalMock.getUser()).andReturn("user");

		replayAll();
		setInternalState(this.options, basicPrincipalMock);
		assertEquals("user", this.options.getUsername());
		verifyAll();
	}
}
