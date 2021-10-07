/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates.checkers;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class SSLConditionCheckerTest {

	@Test
	public void testGetErrorMessaage() throws Exception {
		assertEquals("Not SSL request", new SSLConditionChecker().getErrorMessaage());
	}

	@Test
	public void testCheckCondition() throws Exception {
		final ContainerRequestContext requestContextMock = createMock(ContainerRequestContext.class);
		final SecurityContext securitycontextMock = createMock(SecurityContext.class);

		expect(requestContextMock.getSecurityContext()).andReturn(securitycontextMock);
		expect(securitycontextMock.isSecure()).andReturn(true);

		replayAll();
		assertTrue(new SSLConditionChecker().checkCondition(requestContextMock));
		verifyAll();
	}
}
