/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;


/**
 * @author wro50095
 * 
 */
public class GenericCertificatePathValidatorTest {

	private GenericCertificatePathValidator bean;

	@Before
	public void setUp() throws Exception {
		this.bean = new GenericCertificatePathValidator();
	}

	@Test
	public void testEmptyTrustPath() throws Exception {
		final List<X509Certificate> certPath = new ArrayList<>(2);
		assertTrue(this.bean.validateCertificatePath(certPath, new ArrayList<PublicKey>()));
	}

	@Test
	public void testEmptyCertPath() throws Exception {
		assertTrue(this.bean.validateCertificatePath(new ArrayList<X509Certificate>(), new ArrayList<PublicKey>()));
	}

	@Test
	public void testMultipleCertsOneValid() throws Exception {
		final List<PublicKey> trusetedKeys = new ArrayList<>(1);
		final List<X509Certificate> certPath = new ArrayList<>(2);

		final PublicKey pk1 = createMock(PublicKey.class);
		trusetedKeys.add(pk1);

		final X509Certificate cert1 = createMock(X509Certificate.class);
		final X509Certificate cert2 = createMock(X509Certificate.class);

		certPath.add(cert1);
		certPath.add(cert2);

		cert1.verify(pk1);
		cert2.verify(pk1);
		expectLastCall().andThrow(new CertificateException());

		replayAll();
		assertTrue(this.bean.validateCertificatePath(certPath, trusetedKeys));
		verifyAll();
	}

	@Test
	public void testMultipleCertsTwoPublicKeysAllValid() throws Exception {
		final List<PublicKey> trusetedKeys = new ArrayList<PublicKey>(2);
		final List<X509Certificate> certPath = new ArrayList<X509Certificate>(3);

		final PublicKey pk1 = createMock(PublicKey.class);
		trusetedKeys.add(pk1);
		final PublicKey pk2 = createMock(PublicKey.class);
		trusetedKeys.add(pk2);

		final X509Certificate cert1 = createMock(X509Certificate.class);
		final X509Certificate cert2 = createMock(X509Certificate.class);
		final X509Certificate cert3 = createMock(X509Certificate.class);

		certPath.add(cert1);
		certPath.add(cert2);
		certPath.add(cert3);

		cert1.verify(pk1);
		cert1.verify(pk2);
		expectLastCall().andThrow(new CertificateException());

		cert2.verify(pk1);
		cert2.verify(pk2);
		expectLastCall().andThrow(new CertificateException());

		cert3.verify(pk1);
		expectLastCall().andThrow(new CertificateException());
		cert3.verify(pk2);

		replayAll();
		assertTrue(this.bean.validateCertificatePath(certPath, trusetedKeys));
		verifyAll();
	}

	@Test
	public void testMultipleCertsTwoPublicKeysOneInvalidOneValidForMultipleCerts() throws Exception {
		final List<PublicKey> trusetedKeys = new ArrayList<PublicKey>(2);
		final List<X509Certificate> certPath = new ArrayList<X509Certificate>(3);

		final PublicKey pk1 = createMock(PublicKey.class);
		trusetedKeys.add(pk1);
		final PublicKey pk2 = createMock(PublicKey.class);
		trusetedKeys.add(pk2);

		final X509Certificate cert1 = createMock(X509Certificate.class);
		final X509Certificate cert2 = createMock(X509Certificate.class);
		final X509Certificate cert3 = createMock(X509Certificate.class);

		certPath.add(cert1);
		certPath.add(cert2);
		certPath.add(cert3);

		cert1.verify(pk1);
		cert1.verify(pk2);
		expectLastCall().andThrow(new CertificateException());

		cert2.verify(pk1);
		cert2.verify(pk2);
		expectLastCall().andThrow(new CertificateException());

		cert3.verify(pk1);
		cert3.verify(pk2);
		expectLastCall().andThrow(new CertificateException());

		replayAll();
		assertFalse(this.bean.validateCertificatePath(certPath, trusetedKeys));
		verifyAll();
	}

	@Test
	public void testMultipleCertsAllInvalid() throws Exception {
		final List<PublicKey> trusetedKeys = new ArrayList<>(1);
		final List<X509Certificate> certPath = new ArrayList<>(2);

		final PublicKey pk1 = createMock(PublicKey.class);
		trusetedKeys.add(pk1);

		final X509Certificate cert1 = createMock(X509Certificate.class);
		final X509Certificate cert2 = createMock(X509Certificate.class);

		certPath.add(cert1);
		certPath.add(cert2);

		cert1.verify(pk1);
		expectLastCall().andThrow(new CertificateException());
		cert2.verify(pk1);
		expectLastCall().andThrow(new CertificateException());

		replayAll();
		assertFalse(this.bean.validateCertificatePath(certPath, trusetedKeys));
		verifyAll();
	}
}
