/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.error;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
public class ServiceExceptionTest {

	@Test
	public void testCreate() throws Exception {
		testCreate(true);
		testCreate(false);
	}

	private void testCreate(final boolean notFound) {
		final ErrorException errorExceptionMock = createMock(ErrorException.class);

		replayAll();
		final ServiceException exception = new ServiceException(errorExceptionMock, notFound);
		verifyAll();

		assertEquals(Arrays.asList(errorExceptionMock), exception.getExceptions());
		assertEquals(notFound, exception.isNotFound());
	}

	@Test
	public void testCreateWithList() throws Exception {
		final List<ErrorException> exceptionsList = Arrays.asList(createMock(ErrorException.class));

		replayAll();
		final ServiceException exception = new ServiceException(exceptionsList);
		verifyAll();

		assertEquals(exceptionsList, exception.getExceptions());
		assertFalse(exception.isNotFound());
	}

	@Test
	public void testCreateWithEmptyList() throws Exception {
		try {
			new ServiceException(Collections.<ErrorException> emptyList());
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}
}
