/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;


/**
 * @author wro50095
 * 
 */
public class CertificatePathValidatorTest {

	private CertificatePathValidator bean;

	@Before
	public void setUp() throws Exception {
		final List<X509Certificate> trustedCertificates = new ArrayList<>();

		trustedCertificates.add(loadRootCACertificate());
		trustedCertificates.add(loadServerCACertificate());
		trustedCertificates.add(loadInstallationCACertificate());

		this.bean = new CertificatePathValidator(trustedCertificates);
	}

	@Test
	public void testNoCertificates() throws Exception {
		replayAll();
		assertFalse(this.bean.checkCertificates(Collections.<X509Certificate> emptyList()));
		verifyAll();
	}

	@Test
	public void testClientCertificateFromServerAndRootCa() throws Exception {
		final List<X509Certificate> certificates = new ArrayList<>();
		certificates.add(loadValidClientCertificateSignedByServer());
		certificates.add(loadServerCACertificate());
		certificates.add(loadInstallationCACertificate());
		certificates.add(loadRootCACertificate());

		replayAll();
		assertTrue(this.bean.checkCertificates(certificates));
		verifyAll();
	}

	@Test
	public void testClientCertificateFromServerAndServerCa() throws Exception {
		final List<X509Certificate> certificates = new ArrayList<>();
		certificates.add(loadValidClientCertificateSignedByServer());
		certificates.add(loadServerCACertificate());

		replayAll();
		assertTrue(this.bean.checkCertificates(certificates));
		verifyAll();
	}

	@Test
	public void testGenericClientCertificateAndRootCaMutlipleTimes() throws Exception {
		final List<X509Certificate> certificates = new ArrayList<>();
		certificates.add(loadValidClientCertificateSignedByRoot());
		certificates.add(loadRootCACertificate());

		replayAll();
		assertTrue(this.bean.checkCertificates(certificates));
		assertTrue(this.bean.checkCertificates(certificates));
		verifyAll();
	}

	@Test
	public void testGenericClientCertificateAndRootCa() throws Exception {
		final List<X509Certificate> certificates = new ArrayList<>();
		certificates.add(loadValidClientCertificateSignedByRoot());
		certificates.add(loadRootCACertificate());

		replayAll();
		assertTrue(this.bean.checkCertificates(certificates));
		verifyAll();
	}

	@Test
	public void testClientCertificateSignedByAnotherCLS() throws Exception {
		final List<X509Certificate> certificates = new ArrayList<>();
		certificates.add(loadNotValidClientCertificate());
		certificates.add(loadAnotherCLServerCACertificate());
		certificates.add(loadAnotherCLSServerCertificate());
		certificates.add(loadRootCACertificate());

		replayAll();
		assertFalse(this.bean.checkCertificates(certificates));
		verifyAll();
	}

	private X509Certificate loadAnotherCLSServerCertificate() throws CertificateException, FileNotFoundException,
			IOException {
		return loadCertificateFromResource("/certs/notValid/CLS_Server.crt");
	}

	private X509Certificate loadAnotherCLServerCACertificate() throws CertificateException, FileNotFoundException,
			IOException {
		return loadCertificateFromResource("/certs/notValid/CLS_Server_CA.crt");
	}

	private X509Certificate loadNotValidClientCertificate() throws CertificateException, FileNotFoundException,
			IOException {
		return loadCertificateFromResource("/certs/notValid/CLS_Client.crt");
	}

	private X509Certificate loadValidClientCertificateSignedByServer() throws CertificateException,
			FileNotFoundException, IOException {
		return loadCertificateFromResource("/certs/valid/CLS_Client.crt");
	}

	private X509Certificate loadValidClientCertificateSignedByRoot() throws CertificateException,
			FileNotFoundException, IOException {
		return loadCertificateFromResource("/certs/valid/CLS_Client_Root.crt");
	}

	private X509Certificate loadRootCACertificate() throws FileNotFoundException, IOException, CertificateException {
		return loadCertificateFromResource("/certs/valid/CLS_Root_CA.crt");
	}

	private X509Certificate loadInstallationCACertificate() throws FileNotFoundException, IOException,
			CertificateException {
		return loadCertificateFromResource("/certs/valid/CLS_Installation_CA.crt");
	}

	private X509Certificate loadServerCACertificate() throws FileNotFoundException, IOException, CertificateException {
		return loadCertificateFromResource("/certs/valid/CLS_Server_CA.crt");
	}

	@SuppressWarnings("unused")
	private X509Certificate loadCertificate(final String path) throws CertificateException, FileNotFoundException,
			IOException {
		try (final InputStream is = new FileInputStream(path)) {
			return loadCertificate(is);
		}
	}

	private X509Certificate loadCertificateFromResource(final String path) throws CertificateException,
			FileNotFoundException, IOException {
		try (final InputStream is = this.getClass().getResourceAsStream(path)) {
			return loadCertificate(is);
		}
	}

	private X509Certificate loadCertificate(final InputStream is) throws CertificateException {
		final CertificateFactory fact = CertificateFactory.getInstance("X.509");
		return (X509Certificate) fact.generateCertificate(is);
	}
}
