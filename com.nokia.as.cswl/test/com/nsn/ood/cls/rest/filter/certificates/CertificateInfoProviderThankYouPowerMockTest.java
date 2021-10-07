/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.util.List;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


/**
 * @author wro50095
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
		System.class, CertificateInfoProviderThankYouPowerMock.class })
public class CertificateInfoProviderThankYouPowerMockTest {

	private CertificateInfoProviderThankYouPowerMock bean;

	@Before
	public void setUp() throws Exception {
		this.bean = new CertificateInfoProviderThankYouPowerMock();
	}

	@Test
	public void testNoPropertySet() throws Exception {
		final List<String> certLocations = this.bean.getIntermediateCertLocations();
		assertEquals("/opt/nokia/global/cls-backend/CLS-Server-CA/CLS_Installation_CA.crt", certLocations.get(0));
		assertEquals("/opt/nokia/global/cls-backend/CLS-Server-CA/CLS_Server_CA.crt", certLocations.get(1));
	}

	@Test
	public void testAllPropertiesSet() throws Exception {
		mockStatic(System.class);

		final String certInstallationPath = "installationPath1";
		final String certServerPath = "serverPath1";

		expect(System.getProperty(EasyMock.eq("com.nsn.ood.cls.cert.server"), EasyMock.anyObject(String.class))).andReturn(
				certServerPath);

		expect(System.getProperty(EasyMock.eq("com.nsn.ood.cls.cert.installation"), EasyMock.anyObject(String.class))).andReturn(
				certInstallationPath);

		replayAll();
		final List<String> certLocations = this.bean.getIntermediateCertLocations();
		verifyAll();

		assertEquals(certInstallationPath, certLocations.get(0));

		assertEquals(certServerPath, certLocations.get(1));
	}
}
