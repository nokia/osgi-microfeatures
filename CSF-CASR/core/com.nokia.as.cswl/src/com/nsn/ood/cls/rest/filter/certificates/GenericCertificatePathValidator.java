/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * @author wro50095
 * 
 */
public class GenericCertificatePathValidator {

	public boolean validateCertificatePath(final List<X509Certificate> certificatesToValidate,
			final List<PublicKey> trustedPublicKeys) {
		final Set<PublicKey> confirmedPublicKeys = new HashSet<>();

		for (final X509Certificate x509Certificate : certificatesToValidate) {
			for (final PublicKey trustedCertificate : trustedPublicKeys) {
				try {
					x509Certificate.verify(trustedCertificate);
					confirmedPublicKeys.add(trustedCertificate);
				} catch (InvalidKeyException | CertificateException | NoSuchAlgorithmException
						| NoSuchProviderException | SignatureException e) {
					// nothing to do, just do not threat this certificate as trusted
				}
			}
		}
		return confirmedPublicKeys.size() == trustedPublicKeys.size();
	}
}