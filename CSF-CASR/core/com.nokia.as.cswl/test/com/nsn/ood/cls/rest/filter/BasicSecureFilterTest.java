/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter;

import static com.nsn.ood.cls.model.test.ErrorTestUtil.error;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.security.BasicPrincipal;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.rest.util.ErrorBuilder;
import com.nsn.ood.cls.rest.util.ErrorBuilderFactory;
import com.nsn.ood.cls.rest.util.ResourceBuilder;
import com.nsn.ood.cls.rest.util.ResourceBuilderFactory;
import com.nsn.ood.cls.rest.util.ResponseBuilder;
import com.nsn.ood.cls.rest.util.ResponseBuilderFactory;


/**
 * @author marynows
 * 
 */
public class BasicSecureFilterTest {
	private BasicSecureFilter filter;
	private BasicPrincipal basicPrincipal;
	private ResourceBuilderFactory resourceBuilderFactoryMock;
	private ResponseBuilderFactory responseBuilderFactoryMock;
	private ErrorBuilderFactory errorBuilderFactoryMock;

	@Before
	public void setUp() throws Exception {
		this.filter = new BasicSecureFilter();
		this.basicPrincipal = new BasicPrincipal();
		this.resourceBuilderFactoryMock = createMock(ResourceBuilderFactory.class);
		this.responseBuilderFactoryMock = createMock(ResponseBuilderFactory.class);
		this.errorBuilderFactoryMock = createMock(ErrorBuilderFactory.class);

		setInternalState(this.filter, this.basicPrincipal, this.resourceBuilderFactoryMock,
				this.responseBuilderFactoryMock, this.errorBuilderFactoryMock);
	}

	@Test
	public void testFilter() throws Exception {
		final ContainerRequestContext requestContextMock = createMock(ContainerRequestContext.class);

		expect(requestContextMock.getHeaderString(HttpHeaders.AUTHORIZATION)).andReturn("Basic dXNlcjpwYXNzd29yZA==");

		replayAll();
		this.filter.filter(requestContextMock);
		verifyAll();

		assertEquals("user", this.basicPrincipal.getUser());
	}

	@Test
	public void testFilterNoHeader() throws Exception {
		final ContainerRequestContext requestContextMock = createMock(ContainerRequestContext.class);
		final Response responseMock = createMock(Response.class);
		final ErrorBuilder errorBuilderMock = createMock(ErrorBuilder.class);
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Resource resource = new Resource();

		expect(requestContextMock.getHeaderString(HttpHeaders.AUTHORIZATION)).andReturn(null);
		expect(requestContextMock.getHeaderString("referer")).andReturn("host:port/CLS/internal/test/upload");

		expect(this.errorBuilderFactoryMock.status(Status.UNAUTHORIZED, null)).andReturn(errorBuilderMock);
		expect(errorBuilderMock.build()).andReturn(error(7L));
		expect(this.resourceBuilderFactoryMock.errors(error(7L))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resource);
		expect(this.responseBuilderFactoryMock.error(Status.UNAUTHORIZED, resource)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"CLS\"")).andReturn(
				responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		requestContextMock.abortWith(responseMock);

		replayAll();
		this.filter.filter(requestContextMock);
		verifyAll();
	}

	@Test
	public void testFilterWithWrongCredentials() throws Exception {
		final ContainerRequestContext requestContextMock = createMock(ContainerRequestContext.class);
		final Response responseMock = createMock(Response.class);
		final ErrorBuilder errorBuilderMock = createMock(ErrorBuilder.class);
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Resource resource = new Resource();

		expect(requestContextMock.getHeaderString(HttpHeaders.AUTHORIZATION)).andReturn("test");
		expect(requestContextMock.getHeaderString("referer")).andReturn(null);

		expect(this.errorBuilderFactoryMock.status(eq(Status.NOT_FOUND), notNull(String.class))).andReturn(
				errorBuilderMock);
		expect(errorBuilderMock.build()).andReturn(error(7L));
		expect(this.resourceBuilderFactoryMock.errors(error(7L))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resource);
		expect(this.responseBuilderFactoryMock.error(Status.NOT_FOUND, resource)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		requestContextMock.abortWith(responseMock);

		replayAll();
		this.filter.filter(requestContextMock);
		verifyAll();
	}
}
