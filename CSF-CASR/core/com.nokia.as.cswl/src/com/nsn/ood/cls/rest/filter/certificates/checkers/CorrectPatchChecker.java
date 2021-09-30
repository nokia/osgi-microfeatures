/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates.checkers;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;

import org.apache.commons.lang3.ObjectUtils;

import com.nsn.ood.cls.rest.exception.CLSCertificateException;
import com.nsn.ood.cls.rest.filter.certificates.util.CertificatePathValidator;


/**
 * @author wro50095
 * 
 */
public class CorrectPatchChecker implements ConditionChecker {
	private final CertificatePathValidator certValidator;

	public CorrectPatchChecker(final List<X509Certificate> trustedCertificates) throws CLSCertificateException {
		this.certValidator = new CertificatePathValidator(trustedCertificates);
	}

	@Override
	public boolean checkCondition(final ContainerRequestContext requestContext) {
		return this.certValidator.checkCertificates(getConnectionCertificates(requestContext));
	}

	private List<X509Certificate> getConnectionCertificates(final ContainerRequestContext requestContext) {
		return Arrays.asList(ObjectUtils.firstNonNull(
				(X509Certificate[]) requestContext.getProperty("javax.servlet.request.X509Certificate"),
				new X509Certificate[0]));
	}

	@Override
	public String getErrorMessaage() {
		return "No proper CLS certificate";
	}
}
