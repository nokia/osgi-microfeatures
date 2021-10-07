/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates.util;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.rest.exception.CLSCertificateException;


/**
 * @author wro50095
 *
 */
public class CertificatePathValidator {
	private static final Logger LOG = LoggerFactory.getLogger(CertificatePathValidator.class);

	private final List<X509Certificate> trustedIntermediateCertificates = new ArrayList<X509Certificate>();
	private final Set<TrustAnchor> trustAnchors = new HashSet<>();
	private CertStore intermediateCertStore;

	public CertificatePathValidator(final List<X509Certificate> trustedCertificates) throws CLSCertificateException {
		try {
			for (final X509Certificate x509Certificate : trustedCertificates) {
				final String certName = x509Certificate.getSubjectDN().getName();
				if (isSelfSigned(x509Certificate)) {
					this.trustAnchors.add(new TrustAnchor(x509Certificate, null));
					LOG.debug("Certificate {} has been added as root CA", certName);
				} else {
					this.trustedIntermediateCertificates.add(x509Certificate);
					LOG.debug("Certificate {} has been added as intermediate CA", certName);
				}
			}
			LOG.debug("All trusted certificates has been added");
			this.intermediateCertStore = CertStore.getInstance("Collection",
					new CollectionCertStoreParameters(this.trustedIntermediateCertificates));
			LOG.debug("Trusted intermediate certificates truststore has been created");
		} catch (CertificateException | NoSuchAlgorithmException | NoSuchProviderException
				| InvalidAlgorithmParameterException e) {
			throw new CLSCertificateException("Error occured during CertificatePathValidator iniitialization", e);
		}
	}

	/**
	 * Return true if all certificates are valid according to trusted certificates passed to constructor
	 *
	 * @param certificatesToCheck
	 *            - list of certificates to check
	 */
	public boolean checkCertificates(final List<X509Certificate> certificatesToCheck) {
		boolean result = false;
		try {
			for (final X509Certificate x509Certificate : certificatesToCheck) {
				checkCertificate(this.intermediateCertStore, x509Certificate);
				result = true;
			}
		} catch (final GeneralSecurityException e) {
			result = false;
		}
		return result;
	}

	private void checkCertificate(final CertStore intermediateCertStore, final X509Certificate x509Certificate)
			throws GeneralSecurityException {

		try {
			final CertPathBuilder builder = CertPathBuilder.getInstance(CertPathBuilder.getDefaultType());
			final X509CertSelector selector = new X509CertSelector();
			selector.setCertificate(x509Certificate);

			final PKIXParameters certParameters = new PKIXBuilderParameters(this.trustAnchors, selector);
			certParameters.addCertStore(intermediateCertStore);
			certParameters.setRevocationEnabled(false);

			builder.build(certParameters);
			LOG.debug("Certificate {} is valid", x509Certificate.getSubjectDN().getName());
		} catch (CertPathBuilderException | InvalidAlgorithmParameterException e) {
			LOG.debug("Certificate {} issued by {} is NOT VALID", x509Certificate.getSubjectDN().getName(),
					x509Certificate.getIssuerDN().getName());
			LOG.debug("Not valid status is caused by following exception: {}, caused by: {}", e.getMessage(),
					e.getCause());
			LOG.debug("Trusted root certificates:");
			for (final TrustAnchor trustAnchor : this.trustAnchors) {
				LOG.debug("CN: {}, issued by: {}", trustAnchor.getTrustedCert().getSubjectDN().getName(),
						trustAnchor.getTrustedCert().getIssuerDN().getName());
			}
			LOG.debug("Trusted intermediate certificates:");
			for (final X509Certificate intermediateTrust : this.trustedIntermediateCertificates) {
				LOG.debug("CN: {}, issued by: {}", intermediateTrust.getSubjectDN().getName(),
						intermediateTrust.getIssuerDN().getName());
			}
			throw e;
		}
	}

	private boolean isSelfSigned(final X509Certificate cert)
			throws CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
		try {
			final PublicKey key = cert.getPublicKey();
			cert.verify(key);
			return true;
		} catch (final SignatureException | InvalidKeyException ex) {
			// Invalid signature or key, so certificate is not self-signed
			return false;
		}
	}
}
