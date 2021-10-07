/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.cljl;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertSame;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nokia.licensing.factories.LicenseInstallFactory;
import com.nokia.licensing.interfaces.LicenseInstall;


/**
 * @author marynows
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(LicenseInstallFactory.class)
public class CLJLProxyTest {

	@Test
	public void testGetLicenseInstall() throws Exception {
		mockStatic(LicenseInstallFactory.class);

		final LicenseInstall licenseInstallMock = createMock(LicenseInstall.class);

		expect(LicenseInstallFactory.getInstance(null, "postgres", null)).andReturn(licenseInstallMock);

		replayAll();
		assertSame(licenseInstallMock, new CLJLProxy().getLicenseInstall());
		verifyAll();
	}
}
