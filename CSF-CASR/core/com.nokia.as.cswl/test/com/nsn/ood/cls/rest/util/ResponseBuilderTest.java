/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import static com.nsn.ood.cls.core.test.ClientTagTestUtil.ETAG;
import static com.nsn.ood.cls.core.test.ClientTagTestUtil.EXPIRES_AS_STRING;
import static com.nsn.ood.cls.core.test.ClientTagTestUtil.clientTag;
import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Test;

import com.nsn.ood.cls.model.gen.hal.Resource;


/**
 * @author marynows
 * 
 */
public class ResponseBuilderTest {

//	@Test
//	public void testBuild() throws Exception {
//		assertResponse(new ResponseBuilder(Status.OK).build(), Status.OK, null, null, null, null);
//	}
//
//	@Test
//	public void testType() throws Exception {
//		assertResponse(new ResponseBuilder(Status.CREATED).type("text/text").build(), Status.CREATED, "text/text",
//				null, null, null);
//
//		assertResponse(new ResponseBuilder(Status.CREATED).type(null).build(), Status.CREATED, null, null, null, null);
//	}
//
//	@Test
//	public void testResource() throws Exception {
//		final Resource resource = new Resource();
//		assertResponse(new ResponseBuilder(Status.OK).resource(resource).build(), Status.OK, null, resource, null, null);
//
//		assertResponse(new ResponseBuilder(Status.OK).resource(null).build(), Status.OK, null, null, null, null);
//	}
//
//	@Test
//	public void testTag() throws Exception {
//		assertResponse(new ResponseBuilder(Status.OK).tag(clientTag()).build(), Status.OK, null, null, ETAG,
//				EXPIRES_AS_STRING);
//
//		assertResponse(new ResponseBuilder(Status.OK).tag(clientTag(null, null)).build(), Status.OK, null, null, null,
//				null);
//
//		assertResponse(new ResponseBuilder(Status.OK).tag(null).build(), Status.OK, null, null, null, null);
//	}
//
//	@Test
//	public void testHeader() throws Exception {
//		final Response response = new ResponseBuilder(Status.OK).header("name", "value").build();
//		assertResponse(response, Status.OK, null, null, null, null);
//		assertEquals("value", response.getHeaderString("name"));
//	}
//
//	private void assertResponse(final Response response, final Status expectedStatus, final String expectedType,
//			final Object expectedEntity, final String expectedETag, final String expectedExpires) {
//		assertEquals(expectedStatus, response.getStatusInfo());
//		assertEquals(expectedEntity, response.getEntity());
//		assertEquals(expectedETag, response.getHeaderString(HttpHeaders.ETAG));
//		assertEquals(expectedExpires, response.getHeaderString(HttpHeaders.EXPIRES));
//		assertEquals(expectedType, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
//	}
}
