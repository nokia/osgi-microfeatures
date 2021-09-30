/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.rest.convert.StatusType2ErrorCodeConverter;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class ErrorBuilderFactoryTest {
	private ErrorBuilderFactory factory;
	private ErrorBuilder errorBuilderMock;
	private Converter<StatusType, Long> converterMock;
	private long capturedCode;

	@Before
	public void setUp() throws Exception {
		this.errorBuilderMock = createMock(ErrorBuilder.class);
		this.converterMock = createMock(StatusType2ErrorCodeConverter.class);

		this.factory = new ErrorBuilderFactory() {
			@Override
			protected ErrorBuilder createErrorBuilder(final long code) {
				ErrorBuilderFactoryTest.this.capturedCode = code;
				super.createErrorBuilder(code);
				return ErrorBuilderFactoryTest.this.errorBuilderMock;
			}
		};
		Whitebox.setInternalState(factory, converterMock);
	}

	@Test
	public void testCode() throws Exception {
		expect(this.errorBuilderMock.message("message")).andReturn(this.errorBuilderMock);

		replayAll();
		assertEquals(this.errorBuilderMock, this.factory.code(1L, "message"));
		verifyAll();

		assertEquals(1L, this.capturedCode);
	}

	@Test
	public void testStatus() throws Exception {
		expect(this.converterMock.convertTo(Status.OK)).andReturn(2L);
		expect(this.errorBuilderMock.message("message")).andReturn(this.errorBuilderMock);

		replayAll();
		assertEquals(this.errorBuilderMock, this.factory.status(Status.OK, "message"));
		verifyAll();

		assertEquals(2L, this.capturedCode);
	}

	@Test
	public void testException() throws Exception {
		final ErrorException exceptionMock = createMock(ErrorException.class);

		expect(exceptionMock.getErrorCode()).andReturn(ErrorCode.RESOURCE_NOT_FOUND);
		expect(exceptionMock.getMessage()).andReturn("message");
		expect(this.errorBuilderMock.message("message")).andReturn(this.errorBuilderMock);
		expect(exceptionMock.getName()).andReturn("name");
		expect(exceptionMock.getValue()).andReturn("value");
		expect(this.errorBuilderMock.embedded("name", "value")).andReturn(this.errorBuilderMock);

		replayAll();
		assertEquals(this.errorBuilderMock, this.factory.exception(exceptionMock));
		verifyAll();

		assertEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), this.capturedCode);
	}
}
