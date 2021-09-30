/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.cljl;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.interfaces.LicenseInstall;


/**
 * @author marynows
 *
 */
public class CLJLLicenseServiceTest {
	private CLJLLicenseService service;
	private LicenseInstallSpy licenseInstallSpy;
	private TransactionServiceStub txServiceStub = new TransactionServiceStub();

	@Before
	public void setUp() throws Exception {
		this.service = new CLJLLicenseService();
		this.licenseInstallSpy = new LicenseInstallSpy();

		final CLJLProxy cljlProxyStub = new CLJLProxy() {
			@Override
			public LicenseInstall getLicenseInstall() throws LicenseException {
				return CLJLLicenseServiceTest.this.licenseInstallSpy;
			}
		};

		setInternalState(this.service, new BasicPrincipalStub(), cljlProxyStub, txServiceStub);
	}

	@Test
	public void testInstall() throws Exception {
		final InputStream content = new ByteArrayInputStream(new byte[0]);
		final LicenseInstallOptions options = new LicenseInstallOptionsStub();

		expect(txServiceStub.txControl().notSupported(EasyMock.anyObject()))
									    .andReturn(licenseInstallSpy.installLicense(content, "filename", options.isForce(),
												   options.getTargetId(), options.getUsername()));
		replay(txServiceStub.txControl());
		
		assertSame(LicenseInstallSpy.STORED_LICENSE, this.service.install(content, "filename", options));

		assertFalse(this.licenseInstallSpy.getForceInstall());
		assertEquals("filename", this.licenseInstallSpy.getLicenseFileName());
		assertSame(content, this.licenseInstallSpy.getLicenseFileStream());
		assertEquals(LicenseInstallOptionsStub.TARGET_ID, this.licenseInstallSpy.getTargetId());
		assertEquals(LicenseInstallOptionsStub.USER_NAME, this.licenseInstallSpy.getUsername());
		reset(txServiceStub.txControl());
	}

	@Test
	public void testInstallWithoutOptions() throws Exception {
		final InputStream content = new ByteArrayInputStream(new byte[0]);

		expect(txServiceStub.txControl().notSupported(EasyMock.anyObject()))
										.andReturn(licenseInstallSpy.installLicense(content, "filename", true,
												   null, "anonymous"));
		replay(txServiceStub.txControl());
		assertSame(LicenseInstallSpy.STORED_LICENSE, this.service.install(content, "filename", null));

		assertTrue(this.licenseInstallSpy.getForceInstall());
		assertEquals("filename", this.licenseInstallSpy.getLicenseFileName());
		assertSame(content, this.licenseInstallSpy.getLicenseFileStream());
		assertNull(this.licenseInstallSpy.getTargetId());
		assertEquals(BasicPrincipalStub.user, this.licenseInstallSpy.getUsername());
		reset(txServiceStub.txControl());
	}

	@Test
	public void testCancel() throws Exception {
		final LicenseCancelInfo licenseCancelInfo = new LicenseCancelInfo();

		expect(txServiceStub.txControl().notSupported(EasyMock.anyObject()))
										.andReturn(licenseInstallSpy.cancelLicenseWithFeedback(licenseCancelInfo));
		replay(txServiceStub.txControl());
		assertSame(LicenseInstallSpy.STORED_LICENSE, this.service.cancel(licenseCancelInfo));

		assertSame(licenseCancelInfo, this.licenseInstallSpy.getLicenseCancelInfo());
		reset(txServiceStub.txControl());
	}
}
