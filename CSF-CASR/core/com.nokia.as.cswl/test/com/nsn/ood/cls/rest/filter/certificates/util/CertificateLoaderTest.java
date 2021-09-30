/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.security.cert.Certificate;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;


/**
 * @author wro50095
 * 
 */
public class CertificateLoaderTest {

	private CertificateLoader bean;

	@Before
	public void setUp() throws Exception {
		this.bean = new CertificateLoader();
	}

	@Test
	public void testLoadCertificateFromCertFile() throws Exception {
		final InputStream is = getClass().getResourceAsStream("/certs/valid/CLS_Root_CA.crt");
		final Certificate result = this.bean.loadCertificateFromCertificate(is);
		assertNotNull(result);
		assertEquals("7bdb0400fe3b38c7eb6e8c9ab8bc49d69b20176d", DigestUtils.sha1Hex(result.getEncoded()));
	}
}
