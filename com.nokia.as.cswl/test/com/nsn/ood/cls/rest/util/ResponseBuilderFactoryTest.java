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

import com.nsn.ood.cls.model.CLSMediaType;
import com.nsn.ood.cls.model.gen.hal.Resource;


/**
 * @author marynows
 * 
 */
public class ResponseBuilderFactoryTest {
	private ResponseBuilderFactory factory;
	private ResponseBuilder responseBuilderMock;
	private StatusType capturedStatus;

	@Before
	public void setUp() throws Exception {
		this.responseBuilderMock = createMock(ResponseBuilder.class);

		this.factory = new ResponseBuilderFactory() {
			@Override
			protected ResponseBuilder createResponseBuilder(final StatusType status) {
				ResponseBuilderFactoryTest.this.capturedStatus = status;
				super.createResponseBuilder(status);
				return ResponseBuilderFactoryTest.this.responseBuilderMock;
			}
		};
	}

	@Test
	public void testOk() throws Exception {
		final Resource resource = new Resource();

		expect(this.responseBuilderMock.resource(resource)).andReturn(this.responseBuilderMock);

		replayAll();
		assertEquals(this.responseBuilderMock, this.factory.ok(resource));
		verifyAll();

		assertEquals(Status.OK, this.capturedStatus);
	}

	@Test
	public void testCreated() throws Exception {
		final Resource resource = new Resource();

		expect(this.responseBuilderMock.resource(resource)).andReturn(this.responseBuilderMock);

		replayAll();
		assertEquals(this.responseBuilderMock, this.factory.created(resource));
		verifyAll();

		assertEquals(Status.CREATED, this.capturedStatus);
	}

	@Test
	public void testAccepted() throws Exception {
		final Resource resource = new Resource();

		expect(this.responseBuilderMock.resource(resource)).andReturn(this.responseBuilderMock);

		replayAll();
		assertEquals(this.responseBuilderMock, this.factory.accepted(resource));
		verifyAll();

		assertEquals(Status.ACCEPTED, this.capturedStatus);
	}

	@Test
	public void testNoContent() throws Exception {
		replayAll();
		assertEquals(this.responseBuilderMock, this.factory.noContent());
		verifyAll();

		assertEquals(Status.NO_CONTENT, this.capturedStatus);
	}

	@Test
	public void testErrorWithStatus() throws Exception {
		final Resource resource = new Resource();

		expect(this.responseBuilderMock.type(CLSMediaType.APPLICATION_ERROR_JSON)).andReturn(this.responseBuilderMock);
		expect(this.responseBuilderMock.resource(resource)).andReturn(this.responseBuilderMock);

		replayAll();
		assertEquals(this.responseBuilderMock, this.factory.error(Status.BAD_REQUEST, resource));
		verifyAll();

		assertEquals(Status.BAD_REQUEST, this.capturedStatus);
	}
}
