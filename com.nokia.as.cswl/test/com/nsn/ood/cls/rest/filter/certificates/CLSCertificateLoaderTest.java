/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.annotation.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.nsn.ood.cls.rest.filter.certificates.util.CertificateLoader;


/**
 * @author wro50095
 * 
 */
@RunWith(PowerMockRunner.class)
public class CLSCertificateLoaderTest {
	@Mock
	private CertificateInfoProviderThankYouPowerMock certInfoProvider;

	private CLSCertificateLoader bean;

	@Before
	public void setUp() throws Exception {
		this.bean = new CLSCertificateLoader();
		Whitebox.setInternalState(this.bean, new CertificateLoader());
	}

	@Test
	public void testLoadCertificates() throws Exception {
		final List<String> certList = new ArrayList<String>();
		certList.add("test/certs/valid/CLS_Installation_CA.crt");
		certList.add("test/certs/valid/CLS_Server_CA.crt");
		
		expect(this.certInfoProvider.getRootCACrtLocation()).andReturn("/certs/valid/CLS_Root_CA.crt");
		expect(this.certInfoProvider.getIntermediateCertLocations()).andReturn(certList);
		expect(this.certInfoProvider.getRootCACertificateThumbprint()).andReturn(
				"7bdb0400fe3b38c7eb6e8c9ab8bc49d69b20176d");

		replayAll();
		final List<X509Certificate> result = this.bean.loadCertificates(this.certInfoProvider);
		verifyAll();

		assertEquals(3, result.size());

		assertEquals("7bdb0400fe3b38c7eb6e8c9ab8bc49d69b20176d", DigestUtils.sha1Hex(result.get(0).getEncoded()));
		assertEquals("3f316ab8b38595b32b07be503a18692abb064b7b", DigestUtils.sha1Hex(result.get(1).getEncoded()));
		assertEquals("28c0fdc28afc575c20f560aee17a72b502ae7469", DigestUtils.sha1Hex(result.get(2).getEncoded()));
	}

	@Test
	public void testNotLoadFakedCertificatesFakeRootCA() throws Exception {
		expect(this.certInfoProvider.getRootCACrtLocation()).andReturn("/certs/valid/CLS_Root_CA.crt");
		expect(this.certInfoProvider.getRootCACertificateThumbprint()).andReturn("badthmb");

		replayAll();
		try {
			this.bean.loadCertificates(this.certInfoProvider);
			fail("No certificates should be loaded");
		} catch (final Exception ex) {
		}
		verifyAll();
	}

	@Test
	public void testNotLoadFakedCertificatesFakeIntermediateCerts() throws Exception {
		final List<String> certList = new ArrayList<String>();
		certList.add("src/test/resources/certs/valid/CLS_Installation_CA.crt");
		certList.add("src/test/resources/certs/notValid/CLS_Server_CA.crt");

		expect(this.certInfoProvider.getRootCACrtLocation()).andReturn("/certs/valid/CLS_Root_CA.crt");
		expect(this.certInfoProvider.getIntermediateCertLocations()).andReturn(certList);
		expect(this.certInfoProvider.getRootCACertificateThumbprint()).andReturn(
				"7bdb0400fe3b38c7eb6e8c9ab8bc49d69b20176d");

		replayAll();
		try {
			this.bean.loadCertificates(this.certInfoProvider);
			fail("No certificates should be loaded");
		} catch (final Exception ex) {
		}
		verifyAll();
	}
}
