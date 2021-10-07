/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter;

import static com.nsn.ood.cls.model.test.ErrorTestUtil.error;
import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.service.ClientsService;
import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.model.gen.errors.ETagError;
import com.nsn.ood.cls.rest.util.ErrorBuilder;
import com.nsn.ood.cls.rest.util.ErrorBuilderFactory;
import com.nsn.ood.cls.rest.util.RestUtil;


/**
 * @author marynows
 *
 */
public class ValidateETagFilterTest {
	private ValidateETagFilter filter;
	private ClientsService clientsServiceMock;
	private ErrorBuilderFactory errorBuilderFactoryMock;
	private RestUtil restUtilMock;

	@Before
	public void setUp() throws Exception {
		this.filter = new ValidateETagFilter();
		this.clientsServiceMock = createMock(ClientsService.class);
		this.errorBuilderFactoryMock = createMock(ErrorBuilderFactory.class);
		this.restUtilMock = createMock(RestUtil.class);

		setInternalState(this.filter, this.clientsServiceMock, this.errorBuilderFactoryMock, this.restUtilMock);
	}

	@Test
	public void testFilter() throws Exception {
		final ContainerRequestContext requestContextMock = createMock(ContainerRequestContext.class);

		mockGetClientId(requestContextMock, "abc123");
		mockGetProvidedETag(requestContextMock, "etag1");
		mockGetCurrentETag("abc123", "etag1");

		replayAll();
		this.filter.filter(requestContextMock);
		verifyAll();
	}

	@Test
	public void testFilterForNotEqualETags() throws Exception {
		final ContainerRequestContext requestContextMock = createMock(ContainerRequestContext.class);
		final ErrorBuilder errorBuilderMock = createMock(ErrorBuilder.class);
		final Response responseMock = createMock(Response.class);

		mockGetClientId(requestContextMock, "abc123");
		mockGetProvidedETag(requestContextMock, "etag1");
		mockGetCurrentETag("abc123", "etag2");

		expect(this.errorBuilderFactoryMock.code(ErrorCode.DUPLICATED_CLIENT_ID.getCode(), null))
				.andReturn(errorBuilderMock);
		expect(errorBuilderMock.embedded("etags", new ETagError().withProvidedETag("etag1").withCurrentETag("etag2")))
				.andReturn(errorBuilderMock);
		expect(errorBuilderMock.build()).andReturn(error(3L));
		expect(this.restUtilMock.errorResponse(Status.PRECONDITION_FAILED, error(3L))).andReturn(responseMock);
		requestContextMock.abortWith(responseMock);

		replayAll();
		this.filter.filter(requestContextMock);
		verifyAll();
	}

	@Test
	public void testFilterForNullClientId() throws Exception {
		final ContainerRequestContext requestContextMock = createMock(ContainerRequestContext.class);

		mockGetClientId(requestContextMock, null);
		mockGetProvidedETag(requestContextMock, "etag1");

		replayAll();
		this.filter.filter(requestContextMock);
		verifyAll();
	}

	@Test
	public void testFilterForNullProvidedETag() throws Exception {
		final ContainerRequestContext requestContextMock = createMock(ContainerRequestContext.class);

		mockGetClientId(requestContextMock, "abc123");
		mockGetProvidedETag(requestContextMock, null);

		replayAll();
		this.filter.filter(requestContextMock);
		verifyAll();
	}

	@SuppressWarnings("unchecked")
	private void mockGetClientId(final ContainerRequestContext requestContextMock, final String clientId) {
		final UriInfo uriInfoMock = createMock(UriInfo.class);
		final MultivaluedMap<String, String> pathParametersMock = createMock(MultivaluedMap.class);

		expect(requestContextMock.getUriInfo()).andReturn(uriInfoMock);
		expect(uriInfoMock.getPathParameters()).andReturn(pathParametersMock);
		expect(pathParametersMock.getFirst("clientId")).andReturn(clientId);
	}

	private void mockGetProvidedETag(final ContainerRequestContext requestContextMock, final String etag) {
		expect(requestContextMock.getHeaderString(HttpHeaders.IF_MATCH)).andReturn(etag);
	}

	private void mockGetCurrentETag(final String clientId, final String etag) throws ServiceException {
		expect(this.clientsServiceMock.getETag(clientId)).andReturn(etag);
	}
}
