/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.convert;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.junit.Before;
import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class StatusType2ErrorCodeConverterTest {
	private StatusType2ErrorCodeConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new StatusType2ErrorCodeConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertStatusToCode(null, 0L);

		assertStatusToCode(Status.OK, 200L);
		assertStatusToCode(Status.CREATED, 201L);
		assertStatusToCode(Status.ACCEPTED, 202L);
		assertStatusToCode(Status.NO_CONTENT, 204L);
		assertStatusToCode(Status.RESET_CONTENT, 205L);
		assertStatusToCode(Status.PARTIAL_CONTENT, 206L);

		assertStatusToCode(Status.MOVED_PERMANENTLY, 301L);
		assertStatusToCode(Status.FOUND, 302L);
		assertStatusToCode(Status.SEE_OTHER, 303L);
		assertStatusToCode(Status.NOT_MODIFIED, 304L);
		assertStatusToCode(Status.USE_PROXY, 305L);
		assertStatusToCode(Status.TEMPORARY_REDIRECT, 307L);

		assertStatusToCode(Status.BAD_REQUEST, 0L);
		assertStatusToCode(Status.UNAUTHORIZED, 1L);
		assertStatusToCode(Status.PAYMENT_REQUIRED, 2L);
		assertStatusToCode(Status.FORBIDDEN, 3L);
		assertStatusToCode(Status.NOT_FOUND, 4L);
		assertStatusToCode(Status.METHOD_NOT_ALLOWED, 5L);
		assertStatusToCode(Status.NOT_ACCEPTABLE, 6L);
		assertStatusToCode(Status.PROXY_AUTHENTICATION_REQUIRED, 7L);
		assertStatusToCode(Status.REQUEST_TIMEOUT, 8L);
		assertStatusToCode(Status.CONFLICT, 9L);
		assertStatusToCode(Status.GONE, 10L);
		assertStatusToCode(Status.LENGTH_REQUIRED, 11L);
		assertStatusToCode(Status.PRECONDITION_FAILED, 12L);
		assertStatusToCode(Status.REQUEST_ENTITY_TOO_LARGE, 13L);
		assertStatusToCode(Status.REQUEST_URI_TOO_LONG, 14L);
		assertStatusToCode(Status.UNSUPPORTED_MEDIA_TYPE, 15L);
		assertStatusToCode(Status.REQUESTED_RANGE_NOT_SATISFIABLE, 16L);
		assertStatusToCode(Status.EXPECTATION_FAILED, 17L);

		assertStatusToCode(Status.INTERNAL_SERVER_ERROR, 500L);
		assertStatusToCode(Status.NOT_IMPLEMENTED, 501L);
		assertStatusToCode(Status.BAD_GATEWAY, 502L);
		assertStatusToCode(Status.SERVICE_UNAVAILABLE, 503L);
		assertStatusToCode(Status.GATEWAY_TIMEOUT, 504L);
		assertStatusToCode(Status.HTTP_VERSION_NOT_SUPPORTED, 505L);
	}

	private void assertStatusToCode(final StatusType status, final Long code) {
		assertEquals(code, this.converter.convertTo(status));
	}

	@Test
	public void testConvertFrom() throws Exception {
		assertCodeToStatus(-1L, Status.BAD_REQUEST);

		assertCodeToStatus(0L, Status.BAD_REQUEST);
		assertCodeToStatus(1L, Status.UNAUTHORIZED);
		assertCodeToStatus(2L, Status.PAYMENT_REQUIRED);
		assertCodeToStatus(3L, Status.FORBIDDEN);
		assertCodeToStatus(4L, Status.NOT_FOUND);
		assertCodeToStatus(5L, Status.METHOD_NOT_ALLOWED);
		assertCodeToStatus(6L, Status.NOT_ACCEPTABLE);
		assertCodeToStatus(7L, Status.PROXY_AUTHENTICATION_REQUIRED);
		assertCodeToStatus(8L, Status.REQUEST_TIMEOUT);
		assertCodeToStatus(9L, Status.CONFLICT);
		assertCodeToStatus(10L, Status.GONE);
		assertCodeToStatus(11L, Status.LENGTH_REQUIRED);
		assertCodeToStatus(12L, Status.PRECONDITION_FAILED);
		assertCodeToStatus(13L, Status.REQUEST_ENTITY_TOO_LARGE);
		assertCodeToStatus(14L, Status.REQUEST_URI_TOO_LONG);
		assertCodeToStatus(15L, Status.UNSUPPORTED_MEDIA_TYPE);
		assertCodeToStatus(16L, Status.REQUESTED_RANGE_NOT_SATISFIABLE);
		assertCodeToStatus(17L, Status.EXPECTATION_FAILED);

		assertCodeToStatus(100L, Status.BAD_REQUEST);
		assertCodeToStatus(101L, Status.BAD_REQUEST);
		assertCodeToStatus(102L, Status.BAD_REQUEST);
		assertCodeToStatus(103L, Status.PRECONDITION_FAILED);
		assertCodeToStatus(104L, Status.BAD_REQUEST);
		assertCodeToStatus(105L, Status.BAD_REQUEST);

		assertCodeToStatus(120L, Status.BAD_REQUEST);
		assertCodeToStatus(121L, Status.BAD_REQUEST);
		assertCodeToStatus(122L, Status.BAD_REQUEST);
		assertCodeToStatus(123L, Status.BAD_REQUEST);
		assertCodeToStatus(124L, Status.BAD_REQUEST);

		assertCodeToStatus(140L, Status.BAD_REQUEST);

		assertCodeToStatus(150L, Status.BAD_REQUEST);
		assertCodeToStatus(151L, Status.BAD_REQUEST);
		assertCodeToStatus(152L, Status.BAD_REQUEST);
		assertCodeToStatus(153L, Status.BAD_REQUEST);
		assertCodeToStatus(154L, Status.BAD_REQUEST);
		assertCodeToStatus(155L, Status.BAD_REQUEST);

		assertCodeToStatus(199L, Status.BAD_REQUEST);

		assertCodeToStatus(200L, Status.OK);
		assertCodeToStatus(201L, Status.CREATED);
		assertCodeToStatus(202L, Status.ACCEPTED);
		assertCodeToStatus(204L, Status.NO_CONTENT);
		assertCodeToStatus(205L, Status.RESET_CONTENT);
		assertCodeToStatus(206L, Status.PARTIAL_CONTENT);

		assertCodeToStatus(301L, Status.MOVED_PERMANENTLY);
		assertCodeToStatus(302L, Status.FOUND);
		assertCodeToStatus(303L, Status.SEE_OTHER);
		assertCodeToStatus(304L, Status.NOT_MODIFIED);
		assertCodeToStatus(305L, Status.USE_PROXY);
		assertCodeToStatus(307L, Status.TEMPORARY_REDIRECT);

		assertCodeToStatus(500L, Status.INTERNAL_SERVER_ERROR);
		assertCodeToStatus(501L, Status.NOT_IMPLEMENTED);
		assertCodeToStatus(502L, Status.BAD_GATEWAY);
		assertCodeToStatus(503L, Status.SERVICE_UNAVAILABLE);
		assertCodeToStatus(504L, Status.GATEWAY_TIMEOUT);
		assertCodeToStatus(505L, Status.HTTP_VERSION_NOT_SUPPORTED);

		assertCodeToStatus(555L, Status.BAD_REQUEST);
		assertCodeToStatus(600L, Status.BAD_REQUEST);
	}

	private void assertCodeToStatus(final Long code, final StatusType status) {
		assertEquals(status, this.converter.convertFrom(code));
	}
}
