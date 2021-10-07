/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import static com.nsn.ood.cls.model.test.ErrorTestUtil.error;
import static com.nsn.ood.cls.model.test.ErrorTestUtil.errorsList;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.hal.Resource;


/**
 * @author marynows
 * 
 */
public class RestUtilTest {
	private ResourceBuilderFactory resourceFactoryMock;
	private ResponseBuilderFactory responseFactoryMock;
	private ErrorBuilderFactory errorFactoryMock;

	private RestUtil util;

	@Before
	public void setUp() throws Exception {
		this.resourceFactoryMock = createMock(ResourceBuilderFactory.class);
		this.responseFactoryMock = createMock(ResponseBuilderFactory.class);
		this.errorFactoryMock = createMock(ErrorBuilderFactory.class);

		this.util = new RestUtil();
		Whitebox.setInternalState(util, resourceFactoryMock, responseFactoryMock, errorFactoryMock);
	}

	@Test
	public void testErrorResponseWithMessage() throws Exception {
		final Error error = mockError(Status.OK, "message");
		final Response responseMock = mockResponse(Status.OK, errorsList(error));

		replayAll();
		assertEquals(responseMock, this.util.errorResponse(Status.OK, "message"));
		verifyAll();
	}

	private Error mockError(final Status status, final String message) {
		final Error error = error(3L);
		final ErrorBuilder errorBuilderMock = createMock(ErrorBuilder.class);
		expect(this.errorFactoryMock.status(status, message)).andReturn(errorBuilderMock);
		expect(errorBuilderMock.build()).andReturn(error);
		return error;
	}

	@Test
	public void testErrorResponseWithError() throws Exception {
		final Response responseMock = mockResponse(Status.BAD_REQUEST, errorsList(error(5L)));

		replayAll();
		assertEquals(responseMock, this.util.errorResponse(Status.BAD_REQUEST, error(5L)));
		verifyAll();
	}

	@Test
	public void testErrorResponseWithErrorsList() throws Exception {
		final Response responseMock = mockResponse(Status.NOT_FOUND, errorsList(error(2L), error(4L)));

		replayAll();
		assertEquals(responseMock, this.util.errorResponse(Status.NOT_FOUND, errorsList(error(2L), error(4L))));
		verifyAll();
	}

	private Response mockResponse(final Status status, final List<Error> errors) {
		final Resource resource = new Resource();
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		expect(this.resourceFactoryMock.errors(errors)).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resource);

		final Response responseMock = createMock(Response.class);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		expect(this.responseFactoryMock.error(status, resource)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		return responseMock;
	}

	@Test
	public void testGetStatusForWebApplicationExceptionWithoutResponse() throws Exception {
		final WebApplicationException exceptionMock = createMock(WebApplicationException.class);

		expect(exceptionMock.getResponse()).andReturn(null);

		replayAll();
		assertEquals(Status.BAD_REQUEST, this.util.getStatus(exceptionMock));
		verifyAll();
	}

	@Test
	public void testGetStatusForWebApplicationExceptionWithResponse() throws Exception {
		final WebApplicationException exceptionMock = createMock(WebApplicationException.class);
		final Response responseMock = createMock(Response.class);

		expect(exceptionMock.getResponse()).andReturn(responseMock).times(2);
		expect(responseMock.getStatusInfo()).andReturn(Status.EXPECTATION_FAILED);

		replayAll();
		assertEquals(Status.EXPECTATION_FAILED, this.util.getStatus(exceptionMock));
		verifyAll();
	}
}
