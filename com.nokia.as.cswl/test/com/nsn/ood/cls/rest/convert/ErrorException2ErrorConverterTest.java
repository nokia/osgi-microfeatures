/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.convert;

import static com.nsn.ood.cls.model.test.ErrorTestUtil.error;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.rest.util.ErrorBuilder;
import com.nsn.ood.cls.rest.util.ErrorBuilderFactory;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class ErrorException2ErrorConverterTest {
	private ErrorBuilderFactory errorBuilderFactoryMock;
	private ErrorException2ErrorConverter converter;

	@Before
	public void setUp() throws Exception {
		this.errorBuilderFactoryMock = createMock(ErrorBuilderFactory.class);
		this.converter = new ErrorException2ErrorConverter();
		Whitebox.setInternalState(converter, errorBuilderFactoryMock);
	}

	@Test
	public void testConvertTo() throws Exception {
		final ErrorException errorExceptionMock = createMock(ErrorException.class);
		final ErrorBuilder errorBuilderMock = createMock(ErrorBuilder.class);

		expect(this.errorBuilderFactoryMock.exception(errorExceptionMock)).andReturn(errorBuilderMock);
		expect(errorBuilderMock.build()).andReturn(error(2L));

		replayAll();
		assertEquals(error(2L), this.converter.convertTo(errorExceptionMock));
		verifyAll();
	}

	@Test
	public void testConvertToNull() throws Exception {
		try {
			this.converter.convertTo(null);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}

	@Test
	public void testConvertFrom() throws Exception {
		try {
			this.converter.convertFrom(null);
			fail();
		} catch (final CLSRuntimeException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}
}
