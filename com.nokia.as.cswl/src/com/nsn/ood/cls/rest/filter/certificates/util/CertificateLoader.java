/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates.util;

import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.felix.dm.annotation.api.Component;


/**
 * @author wro50095
 * 
 */
@Component(provides = CertificateLoader.class)
public class CertificateLoader {

	public X509Certificate loadCertificateFromCertificate(final InputStream certificateInputStream)
			throws CertificateException {
		final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		return (X509Certificate) certificateFactory.generateCertificate(certificateInputStream);
	}
}
