/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createMockAndExpectNew;
import static org.powermock.api.easymock.PowerMock.createStrictMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.easymock.EasyMock;
import org.easymock.LogicalOperator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.annotation.MockStrict;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.nsn.ood.cls.rest.exception.CLSCertificateException;
import com.nsn.ood.cls.rest.filter.certificates.checkers.AlwaysFailChecker;
import com.nsn.ood.cls.rest.filter.certificates.checkers.ConditionChecker;
import com.nsn.ood.cls.rest.filter.certificates.checkers.CorrectPatchChecker;
import com.nsn.ood.cls.rest.filter.certificates.checkers.SSLConditionChecker;


/**
 * @author wro50095
 * 
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.security.*")
@PrepareForTest({
		System.class, KeyStore.class, SSLFilter.class, X509Certificate.class })
public class SSLFilterTest {
	@MockStrict
	private ContainerRequestContext context;

	private SSLFilter bean;

	@Before
	public void setUp() throws Exception {
		this.bean = new SSLFilter();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testInit() throws Exception {
		final CertificateInfoProviderThankYouPowerMock certInfoProviderMock = createStrictMock(CertificateInfoProviderThankYouPowerMock.class);
		final CLSCertificateLoader certificateLoader = createStrictMock(CLSCertificateLoader.class);
		final List<X509Certificate> certificates = createMock(List.class);

		expect(certificateLoader.loadCertificates(certInfoProviderMock)).andReturn(certificates);

		final CorrectPatchChecker correctPatchChecker = createMockAndExpectNew(CorrectPatchChecker.class, certificates);

		Whitebox.setInternalState(this.bean, certificateLoader);
		Whitebox.setInternalState(this.bean, certInfoProviderMock);

		replayAll();
		Whitebox.invokeMethod(this.bean, "initSSLFilter");
		verifyAll();

		final List<ConditionChecker> conditionCheckers = Whitebox.getInternalState(this.bean, List.class);

		final ConditionChecker sslConditionChecker = conditionCheckers.get(0);
		final ConditionChecker correctPathConditionChecker = conditionCheckers.get(1);

		assertTrue(sslConditionChecker instanceof SSLConditionChecker);
		assertEquals(correctPatchChecker, correctPathConditionChecker);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testInitCertificateProblem() throws Exception {
		final CertificateInfoProviderThankYouPowerMock certInfoProviderMock = createStrictMock(CertificateInfoProviderThankYouPowerMock.class);
		final CLSCertificateLoader certificateLoader = createStrictMock(CLSCertificateLoader.class);

		expect(certificateLoader.loadCertificates(certInfoProviderMock)).andThrow(new CLSCertificateException());

		Whitebox.setInternalState(this.bean, certificateLoader);
		Whitebox.setInternalState(this.bean, certInfoProviderMock);

		replayAll();
		Whitebox.invokeMethod(this.bean, "initSSLFilter");
		verifyAll();

		final List<ConditionChecker> conditionCheckers = Whitebox.getInternalState(this.bean, List.class);

		final ConditionChecker sslConditionChecker = conditionCheckers.get(0);
		final ConditionChecker alwaysFailChecker = conditionCheckers.get(1);

		assertTrue(sslConditionChecker instanceof SSLConditionChecker);
		assertTrue(alwaysFailChecker instanceof AlwaysFailChecker);
	}

	@Test
	public void testOneCheckerFails() throws Exception {
		final ConditionChecker checker1 = createMock(ConditionChecker.class);
		final ConditionChecker checker2 = createMock(ConditionChecker.class);
		final String errorMessage = "óóóę";

		expect(checker1.checkCondition(this.context)).andReturn(true);
		expect(checker2.checkCondition(this.context)).andReturn(false);
		expect(checker2.getErrorMessaage()).andReturn(errorMessage);
		this.context.setSecurityContext(EasyMock.anyObject());

		final List<ConditionChecker> conditionCheckers = new ArrayList<ConditionChecker>();
		conditionCheckers.add(checker1);
		conditionCheckers.add(checker2);

		this.context.abortWith(EasyMock.cmp(Response.status(Status.NOT_FOUND).build(), new ResponseComparator(),
				LogicalOperator.EQUAL));

		Whitebox.setInternalState(this.bean, conditionCheckers);

		replayAll();
		this.bean.filter(this.context);
		verifyAll();
	}

	@Test
	public void testFirstCheckerFails() throws Exception {
		final ConditionChecker checker1 = createMock(ConditionChecker.class);
		final ConditionChecker checker2 = createMock(ConditionChecker.class);
		final String errorMessage = "óóóę";

		expect(checker1.checkCondition(this.context)).andReturn(false);
		expect(checker1.getErrorMessaage()).andReturn(errorMessage);
		this.context.setSecurityContext(EasyMock.anyObject());

		final List<ConditionChecker> conditionCheckers = new ArrayList<ConditionChecker>();
		conditionCheckers.add(checker1);
		conditionCheckers.add(checker2);

		this.context.abortWith(EasyMock.cmp(Response.status(Status.NOT_FOUND).build(), new ResponseComparator(),
				LogicalOperator.EQUAL));

		Whitebox.setInternalState(this.bean, conditionCheckers);

		replayAll();
		this.bean.filter(this.context);
		verifyAll();
	}

	@Test
	public void testAllCheckersPass() throws Exception {
		final ConditionChecker checker1 = createMock(ConditionChecker.class);
		final ConditionChecker checker2 = createMock(ConditionChecker.class);

		expect(checker1.checkCondition(this.context)).andReturn(true);
		expect(checker2.checkCondition(this.context)).andReturn(true);
		this.context.setSecurityContext(EasyMock.anyObject());

		final List<ConditionChecker> conditionCheckers = new ArrayList<ConditionChecker>();
		conditionCheckers.add(checker1);
		conditionCheckers.add(checker2);

		Whitebox.setInternalState(this.bean, conditionCheckers);

		replayAll();
		this.bean.filter(this.context);
		verifyAll();
	}

	private static class ResponseComparator implements Comparator<Response> {

		@Override
		public int compare(final Response o1, final Response o2) {
			return (o1.getStatus() == o2.getStatus() == true) ? 0 : -1;
		}
	}
}
