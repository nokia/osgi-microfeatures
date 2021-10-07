/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates.checkers;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMockAndExpectNew;
import static org.powermock.api.easymock.PowerMock.createStrictMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;

import org.easymock.EasyMock;
import org.easymock.LogicalOperator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.nsn.ood.cls.rest.filter.certificates.util.CertificatePathValidator;


/**
 * @author wro50095
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	CorrectPatchChecker.class })
@PowerMockIgnore("javax.security.*")
public class CorrectPatchCheckerTest {

	@Test
	public void testConstructor() throws Exception {
		final List<X509Certificate> trustedCertificates = new ArrayList<>();

		final CertificatePathValidator certificatePathValidatorMock = createMockAndExpectNew(
				CertificatePathValidator.class, trustedCertificates);

		replayAll();
		final CorrectPatchChecker patchChecker = new CorrectPatchChecker(trustedCertificates);
		verifyAll();

		final CertificatePathValidator internalPathValidator = Whitebox.getInternalState(patchChecker,
				CertificatePathValidator.class);

		assertEquals(certificatePathValidatorMock, internalPathValidator);
	}

	@Test
	public void testValidateCertificate() throws Exception {
		final ContainerRequestContext requestContext = prepareContainerRequest(true);

		replayAll();
		final CorrectPatchChecker patchChecker = new CorrectPatchChecker(null);
		assertTrue(patchChecker.checkCondition(requestContext));
		verifyAll();
	}

	@Test
	public void testValidateCertificateFails() throws Exception {
		final ContainerRequestContext requestContext = prepareContainerRequest(false);

		replayAll();
		final CorrectPatchChecker patchChecker = new CorrectPatchChecker(null);
		assertFalse(patchChecker.checkCondition(requestContext));
		verifyAll();
	}

	private ContainerRequestContext prepareContainerRequest(final boolean expectedResult) throws Exception {
		final CertificatePathValidator certificatePathValidatorMock = createMockAndExpectNew(
				CertificatePathValidator.class, (List<?>) null);

		final X509Certificate x509CertificateMock1 = createStrictMock(X509Certificate.class);
		final X509Certificate x509CertificateMock2 = createStrictMock(X509Certificate.class);
		final List<X509Certificate> x509Certificates = new ArrayList<>();
		x509Certificates.add(x509CertificateMock1);
		x509Certificates.add(x509CertificateMock2);

		final ContainerRequestContext requestContext = prepareRequestContext(x509Certificates);

		expect(
				certificatePathValidatorMock.checkCertificates(EasyMock.cmp(x509Certificates, new ListComparator(),
						LogicalOperator.EQUAL))).andReturn(expectedResult);
		return requestContext;
	}

	private ContainerRequestContext prepareRequestContext(final List<X509Certificate> singletonList) {
		final ContainerRequestContext requestContext = createStrictMock(ContainerRequestContext.class);
		expect(requestContext.getProperty("javax.servlet.request.X509Certificate")).andReturn(
				singletonList.toArray(new X509Certificate[0]));
		return requestContext;
	}

	private static class ListComparator implements Comparator<List<X509Certificate>> {

		@Override
		public int compare(final List<X509Certificate> o1, final List<X509Certificate> o2) {
			final int sizeCompare = o1.size() - o2.size();
			if (sizeCompare == 0) {
				for (final X509Certificate x509Certificate : o1) {
					if (!o2.contains(x509Certificate)) {
						return -1;
					}
				}
				return 0;
			} else {
				return sizeCompare;
			}
		}
	}
}
