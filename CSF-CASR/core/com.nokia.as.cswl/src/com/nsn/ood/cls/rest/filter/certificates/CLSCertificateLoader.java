/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.rest.exception.CLSCertificateException;
import com.nsn.ood.cls.rest.filter.certificates.util.CertificateLoader;
import com.nsn.ood.cls.rest.filter.certificates.util.CertificatePathValidator;
import com.nsn.ood.cls.util.exception.CLSException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author wro50095
 * 
 */
@Component(provides = CLSCertificateLoader.class)
public class CLSCertificateLoader {
	private static final Logger LOG = LoggerFactory.getLogger(CLSCertificateLoader.class);
	@ServiceDependency
	private CertificateLoader certificateLoader;

	public List<X509Certificate> loadCertificates(final CertificateInfoProviderThankYouPowerMock certInfoProvider)
			throws CLSCertificateException {
		try {
			final List<X509Certificate> result = new ArrayList<X509Certificate>();

			final X509Certificate rootCertificate = loadRootCertificate(certInfoProvider);

			for (final String certificatePath : certInfoProvider.getIntermediateCertLocations()) {
				try (final InputStream is = new FileInputStream(certificatePath)) {
					result.add(this.certificateLoader.loadCertificateFromCertificate(is));
				}
			}

			if (!isCertificatePathOk(result, rootCertificate)) {
				throw new CLSRuntimeException("Bad truested certificate chain!");
			}
			result.add(0, rootCertificate);

			return result;
		} catch (CertificateException | IOException | CLSException e) {
			throw new CLSCertificateException("Error occured during certificate loading", e);
		}
	}

	private boolean isCertificatePathOk(final List<X509Certificate> certificates, final X509Certificate rootCertificate)
			throws CLSCertificateException {
		final List<X509Certificate> trustedList = new ArrayList<X509Certificate>();
		trustedList.add(rootCertificate);
		trustedList.addAll(certificates);

		final CertificatePathValidator pathValidator = new CertificatePathValidator(trustedList);

		for (final X509Certificate x509Certificate : certificates) {
			if (!pathValidator.checkCertificates(Collections.singletonList(x509Certificate))) {
				LOG.warn("Certificate {} is not valid", x509Certificate.getSubjectDN().getName());
				return false;
			}
		}
		return true;
	}

	private X509Certificate loadRootCertificate(final CertificateInfoProviderThankYouPowerMock certInfoProvider)
			throws CertificateException, CLSException {
		final X509Certificate rootCertificate = this.certificateLoader.loadCertificateFromCertificate(this.getClass()
				.getResourceAsStream(certInfoProvider.getRootCACrtLocation()));
		if (isSignatureValid(certInfoProvider.getRootCACertificateThumbprint(), rootCertificate)) {
			return rootCertificate;
		} else {
			throw new CLSException("Error occured during certificate load (E_1981)");
		}
	}

	private boolean isSignatureValid(final String validSignature, final X509Certificate rootCertificate)
			throws CertificateEncodingException {
		return DigestUtils.sha1Hex(rootCertificate.getEncoded()).equals(validSignature);
	}
}
