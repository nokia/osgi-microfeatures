/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createStrictMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.rest.exception.ViolationException;


/**
 * @author marynows
 * 
 */
public class ViolationExceptionBuilderFactoryTest {
	private static final String MESSAGE = "message";
	private static final Object OBJECT_VALUE = "object";

	private ViolationExceptionBuilderFactory factory;
	private ViolationExceptionBuilder builderMock;
	private ViolationException exceptionMock;

	@Before
	public void setUp() throws Exception {
		this.builderMock = createStrictMock(ViolationExceptionBuilder.class);
		this.exceptionMock = createMock(ViolationException.class);

		this.factory = new ViolationExceptionBuilderFactory() {
			@Override
			protected ViolationExceptionBuilder createBuilder(final String message) {
				assertEquals(MESSAGE, message);
				assertTrue(super.createBuilder(message) instanceof ViolationExceptionBuilder);
				return ViolationExceptionBuilderFactoryTest.this.builderMock;
			}
		};
	}

	@Test
	public void testException() throws Exception {
		expect(this.builderMock.build()).andReturn(this.exceptionMock);

		replayAll();
		assertEquals(this.exceptionMock, this.factory.exception(MESSAGE));
		verifyAll();
	}

	@Test
	public void testValueException() throws Exception {
		expect(this.builderMock.value("value")).andReturn(this.builderMock);
		expect(this.builderMock.build()).andReturn(this.exceptionMock);

		replayAll();
		assertEquals(this.exceptionMock, this.factory.valueException(MESSAGE, "value"));
		verifyAll();
	}

	@Test
	public void testPathException() throws Exception {
		expect(this.builderMock.path("path", null, null)).andReturn(this.builderMock);
		expect(this.builderMock.build()).andReturn(this.exceptionMock);

		replayAll();
		assertEquals(this.exceptionMock, this.factory.pathException(MESSAGE, "path"));
		verifyAll();
	}

	@Test
	public void testFeatureException() throws Exception {
		expect(this.builderMock.path("features", 7, "field")).andReturn(this.builderMock);
		expect(this.builderMock.value(OBJECT_VALUE)).andReturn(this.builderMock);
		expect(this.builderMock.build()).andReturn(this.exceptionMock);

		replayAll();
		assertEquals(this.exceptionMock, this.factory.featureException(MESSAGE, 7, "field", OBJECT_VALUE));
		verifyAll();
	}

	@Test
	public void testClientException() throws Exception {
		expect(this.builderMock.path("clients", 0, "field")).andReturn(this.builderMock);
		expect(this.builderMock.value(OBJECT_VALUE)).andReturn(this.builderMock);
		expect(this.builderMock.build()).andReturn(this.exceptionMock);

		replayAll();
		assertEquals(this.exceptionMock, this.factory.clientException(MESSAGE, "field", OBJECT_VALUE));
		verifyAll();
	}

	@Test
	public void testSettingValueException() throws Exception {
		expect(this.builderMock.path(null, 5, "value")).andReturn(this.builderMock);
		expect(this.builderMock.value(OBJECT_VALUE)).andReturn(this.builderMock);
		expect(this.builderMock.build()).andReturn(this.exceptionMock);

		replayAll();
		assertEquals(this.exceptionMock, this.factory.settingValueException(MESSAGE, 5, OBJECT_VALUE));
		verifyAll();
	}

	@Test
	public void testSettingKeyException() throws Exception {
		expect(this.builderMock.path("key", null, null)).andReturn(this.builderMock);
		expect(this.builderMock.value(OBJECT_VALUE)).andReturn(this.builderMock);
		expect(this.builderMock.build()).andReturn(this.exceptionMock);

		replayAll();
		assertEquals(this.exceptionMock, this.factory.settingKeyException(MESSAGE, OBJECT_VALUE));
		verifyAll();
	}
}
