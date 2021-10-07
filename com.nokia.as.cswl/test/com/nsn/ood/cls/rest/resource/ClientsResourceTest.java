/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource;

import static com.nsn.ood.cls.core.test.ClientTagTestUtil.clientTag;
import static com.nsn.ood.cls.core.test.ErrorExceptionTestUtil.errorExceptionsList;
import static com.nsn.ood.cls.core.test.ObjectWithTagTestUtil.clientWithTag;
import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static com.nsn.ood.cls.model.test.ClientTestUtil.clients;
import static com.nsn.ood.cls.model.test.ClientTestUtil.clientsList;
import static com.nsn.ood.cls.model.test.ErrorTestUtil.error;
import static com.nsn.ood.cls.model.test.LinkTestUtil.assertLink;
import static com.nsn.ood.cls.util.test.AnnotationTestUtil.assertAnnotation;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.easymock.PowerMock;

import com.nsn.ood.cls.core.convert.Client2StringConverter;
import com.nsn.ood.cls.core.model.ClientWithTag;
import com.nsn.ood.cls.core.service.ClientsService;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.clients.Clients;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.rest.ValidateETag;
import com.nsn.ood.cls.rest.convert.ErrorException2StringConverter;
import com.nsn.ood.cls.rest.exception.ViolationException;
import com.nsn.ood.cls.rest.util.ResourceBuilder;
import com.nsn.ood.cls.rest.util.ResponseBuilder;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 *
 */
public class ClientsResourceTest extends AbstractResourceTest {
	private static final String LONG_CLIENT_ID = "123456789012345678901234567890123456789012345678901";
	private static final String LONG_TARGET_TYPE = "123456789012345678901";

	private ClientsResource resource;
	private ClientsService clientsServiceMock;
	private BaseResource base;
	private Converter<Client, String> client2StringConverter;

	@Before
	public void setUp() throws Exception {
		this.clientsServiceMock = createMock(ClientsService.class);

		this.resource = new ClientsResource();
		this.client2StringConverter = createMock(Client2StringConverter.class);
		this.base = createMock(BaseResource.class);
		init(base);
		setInternalState(this.resource, this.clientsServiceMock, new ResourceContextMock(), client2StringConverter, base);
		this.base.init(EasyMock.anyObject(), EasyMock.eq("clients"));
	}

	@Test
	public void testReserveClientId() throws Exception {
		testReserveClientId(client());
	}
	
	@Test
	public void testReserveClientId2() throws Exception {
		testReserveClientId(client(null, 1L, null));
	}
	
	private void testReserveClientId(final Client client) throws ServiceException {
		final Resource resourceMock = createMock(Resource.class);
		final Capture<Link> capturedLink = new Capture<>();

		expect(this.client2StringConverter.convertTo(client)).andReturn("log");
		this.base.logInit(EasyMock.eq("Reserve new client ID"), EasyMock.anyObject(String.class));
		
		ClientWithTag clientWithTag = clientWithTag(client, clientTag());
		expect(this.clientsServiceMock.reserveClientId(client)).andReturn(clientWithTag);
		
		this.base.logSuccess(EasyMock.eq("Reserve new client ID"), EasyMock.anyObject(String.class));
		expect(this.client2StringConverter.convertTo(client)).andReturn("log");

		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		expect(this.resourceBuilderFactoryMock.selfLink(capture(capturedLink))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.clients(clientsList(client))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resourceMock);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.created(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.tag(clientTag())).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		expect(this.base.link()).andReturn(new Link().withHref("/clients"));

		final HttpHeaders httpHeaders = createHttpHeaders();
		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.reserveClientIdOrBulkOperation(clients(client), httpHeaders));
		verifyAll();

		assertLink(capturedLink.getValue(), "/clients");
	}

	@Test
	public void testReserveClientIdAndExpectError() throws Exception {
		final ServiceException exceptionMock = createMock(ServiceException.class);
		
		expect(this.client2StringConverter.convertTo(client())).andReturn("log");
		this.base.logInit(EasyMock.eq("Reserve new client ID"), EasyMock.anyObject(String.class));
		this.base.logFailure("Reserve new client ID", exceptionMock);
		expect(this.clientsServiceMock.reserveClientId(client())).andThrow(exceptionMock);
		Converter<ErrorException, String> converter = createMock(ErrorException2StringConverter.class);
		errorExceptionsList().forEach(e -> expect(converter.convertTo(e)).andReturn("error"));
		final Response responseMock = createMock(Response.class);
		errorExceptionsList().forEach(e -> expect(errorException2ErrorConverter.convertTo(e)).andReturn(error(2L)));
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		final HttpHeaders httpHeaders = createHttpHeaders();
		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.reserveClientIdOrBulkOperation(clients(client()), httpHeaders));
		verifyAll();
	}

	@Test
	public void testReserveClientIdAndExpectExceptionWhenNotOneClient() throws Exception {
		testReserveClientIdAndExpectExceptionWhenNotOneClient(null);
		testReserveClientIdAndExpectExceptionWhenNotOneClient(clients((List<Client>) null));
		testReserveClientIdAndExpectExceptionWhenNotOneClient(clients(Collections.<Client> emptyList()));
		testReserveClientIdAndExpectExceptionWhenNotOneClient(clients(client(), client()));
	}

	private void testReserveClientIdAndExpectExceptionWhenNotOneClient(final Clients clients) {
		resetAll();
		final ViolationException exceptionMock = createMock(ViolationException.class);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);
		expect(this.violationExceptionBuilderFactoryMock.pathException("clients.oneClient", "clients"))
				.andReturn(exceptionMock);

		final HttpHeaders httpHeaders = createHttpHeaders();
		replayAll();
		try {
			this.resource.reserveClientIdOrBulkOperation(clients, httpHeaders);
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testReserveClientIdAndExpectExceptionForWrongClient() throws Exception {
		testReserveClientIdAndExpectExceptionForWrongClient(clients(client("ABC123")), "clients.unexpectedClientId",
				"clientId", "ABC123");
		testReserveClientIdAndExpectExceptionForWrongClient(clients(client(null, LONG_TARGET_TYPE)),
				"clients.targetType", "targetType", LONG_TARGET_TYPE);
		testReserveClientIdAndExpectExceptionForWrongClient(clients(client(null, 0L, "WBTS")),
				"clients.positiveKeepAliveTime", "keepAliveTime", 0L);
	}

	private void testReserveClientIdAndExpectExceptionForWrongClient(final Clients clients,
			final String expectedMessage, final String expectedField, final Object expectedValue) {
		resetAll();
		final ViolationException exceptionMock = createMock(ViolationException.class);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);
		expect(this.violationExceptionBuilderFactoryMock.clientException(expectedMessage, expectedField, expectedValue))
				.andReturn(exceptionMock);

		final HttpHeaders httpHeaders = createHttpHeaders();
		replayAll();
		try {
			this.resource.reserveClientIdOrBulkOperation(clients, httpHeaders);
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testKeepReservationAlive() throws Exception {
		testKeepReservationAlive(client("ABC123", 1L));
		testKeepReservationAlive(client("ABC123"));
	}

	private void testKeepReservationAlive(final Client client) throws Exception {
		resetAll();

		expect(this.client2StringConverter.convertTo(client)).andReturn("log");
		this.base.logInit(EasyMock.eq("Keep reservation alive"), EasyMock.anyObject(String.class));
		this.base.logSuccess(EasyMock.eq("Keep reservation alive"));
		expect(this.clientsServiceMock.keepReservationAlive(client)).andReturn(clientTag());
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.noContent()).andReturn(responseBuilderMock);
		expect(responseBuilderMock.tag(clientTag())).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);

		replayAll();
		assertEquals(responseMock, this.resource.keepReservationAlive("ABC123", clients(client)));
		verifyAll();
	}

	@Test
	public void testKeepReservationAliveAndExpectError() throws Exception {
		final ServiceException exceptionMock = createMock(ServiceException.class);

		expect(this.client2StringConverter.convertTo(client("ABC123"))).andReturn("log");
		this.base.logInit(EasyMock.eq("Keep reservation alive"), EasyMock.anyObject(String.class));
		this.base.logFailure("Keep reservation alive", exceptionMock);
		expect(this.clientsServiceMock.keepReservationAlive(client("ABC123"))).andThrow(exceptionMock);
		Converter<ErrorException, String> converter = createMock(ErrorException2StringConverter.class);
		errorExceptionsList().forEach(e -> expect(converter.convertTo(e)).andReturn("error"));
		final Response responseMock = createMock(Response.class);
		errorExceptionsList().forEach(e -> expect(errorException2ErrorConverter.convertTo(e)).andReturn(error(2L)));
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);


		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.keepReservationAlive("ABC123", clients(client("ABC123"))));
		verifyAll();
	}

	@Test
	public void testKeepReservationAliveAndExpectExceptionWhenNotOneClient() throws Exception {
		testKeepReservationAliveAndExpectExceptionWhenNotOneClient(null);
		testKeepReservationAliveAndExpectExceptionWhenNotOneClient(clients((List<Client>) null));
		testKeepReservationAliveAndExpectExceptionWhenNotOneClient(clients(Collections.<Client> emptyList()));
		testKeepReservationAliveAndExpectExceptionWhenNotOneClient(clients(client(), client()));
	}

	private void testKeepReservationAliveAndExpectExceptionWhenNotOneClient(final Clients clients) {
		resetAll();
		final ViolationException exceptionMock = createMock(ViolationException.class);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);
		expect(this.violationExceptionBuilderFactoryMock.pathException("clients.oneClient", "clients"))
				.andReturn(exceptionMock);

		replayAll();
		try {
			this.resource.keepReservationAlive("ABC123", clients);
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testKeepReservationAliveAndExpectExceptionForWrongClient() throws Exception {
		testKeepReservationAliveAndExpectExceptionForWrongClient(clients(client()), "clients.matchResource", "clientId",
				null);
		testKeepReservationAliveAndExpectExceptionForWrongClient(clients(client("ABC123", null, "WBTS")),
				"clients.unexpectedTargetType", "targetType", "WBTS");
		testKeepReservationAliveAndExpectExceptionForWrongClient(clients(client("ABC123", 0L)),
				"clients.positiveKeepAliveTime", "keepAliveTime", 0L);
	}

	private void testKeepReservationAliveAndExpectExceptionForWrongClient(final Clients clients,
			final String expectedMessage, final String expectedField, final Object expectedValue) {
		resetAll();
		final ViolationException exceptionMock = createMock(ViolationException.class);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);
		expect(this.violationExceptionBuilderFactoryMock.clientException(expectedMessage, expectedField, expectedValue))
				.andReturn(exceptionMock);

		replayAll();
		try {
			this.resource.keepReservationAlive("ABC123", clients);
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testGetFeaturesResourceWithWrongClientId() throws Exception {
		final ViolationException exceptionMock = createMock(ViolationException.class);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);
		expect(this.violationExceptionBuilderFactoryMock.valueException("clients.clientId", LONG_CLIENT_ID))
				.andReturn(exceptionMock);

		replayAll();
		try {
			this.resource.start();
			this.resource.getFeaturesResource(LONG_CLIENT_ID);
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testAnnotations() throws Exception {
		assertAnnotation(ClientsResource.class.getMethod("keepReservationAlive", String.class, Clients.class),
				ValidateETag.class);
		PowerMock.resetAll();
	}

	/**
	 * @return
	 */
	private HttpHeaders createHttpHeaders() {
		final List<MediaType> mediaTypes = new ArrayList<>();
		final HttpHeaders headerMock = createMock(HttpHeaders.class);
		expect(headerMock.getAcceptableMediaTypes()).andReturn(mediaTypes).anyTimes();
		return headerMock;
	}

}
