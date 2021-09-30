/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.util.Arrays;
import java.util.Collections;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.media.multipart.BodyPart;
import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class HttpUtilsTest {
	private final HttpUtils httpUtils = new HttpUtils();

	@Test
	public void testExtractFileName() throws Exception {
		testExtractFileName("form-data; name=\"files\"; filename=\"file1.txt\"; test=\"test\"", "file1.txt");
		testExtractFileName("attachment; name=\"files\"; filename=\"file2.txt\"; test=\"test\"", "file2.txt");
	}

	private void testExtractFileName(final String header, final String expectedFileName) {
		final BodyPart inputPartMock = createMock(BodyPart.class);
		final MultivaluedMap<String, String> headersMock = createMultivaluedMapMock();

		expect(inputPartMock.getHeaders()).andReturn(headersMock);
		expect(headersMock.get(HttpHeaders.CONTENT_DISPOSITION)).andReturn(Arrays.asList(header));

		replayAll();
		assertEquals(expectedFileName, this.httpUtils.extractFileName(inputPartMock));
		verifyAll();
	}

	@Test
	public void testExtractFileNameWhenNoFilename() throws Exception {
		final BodyPart inputPartMock = createMock(BodyPart.class);
		final MultivaluedMap<String, String> headersMock = createMultivaluedMapMock();

		expect(inputPartMock.getHeaders()).andReturn(headersMock);
		expect(headersMock.get(HttpHeaders.CONTENT_DISPOSITION)).andReturn(Arrays.asList("form-data; name=\"files\""));

		replayAll();
		assertNull(this.httpUtils.extractFileName(inputPartMock));
		verifyAll();
	}

	@Test
	public void testExtractFileNameWhenNoFormDataHeader() throws Exception {
		final BodyPart inputPartMock = createMock(BodyPart.class);
		final MultivaluedMap<String, String> headersMock = createMultivaluedMapMock();

		expect(inputPartMock.getHeaders()).andReturn(headersMock);
		expect(headersMock.get(HttpHeaders.CONTENT_DISPOSITION)).andReturn(Arrays.asList("test1", "test2"));

		replayAll();
		assertNull(this.httpUtils.extractFileName(inputPartMock));
		verifyAll();
	}

	@Test
	public void testExtractFileNameWhenEmptyHeaders() throws Exception {
		final BodyPart inputPartMock = createMock(BodyPart.class);
		final MultivaluedMap<String, String> headersMock = createMultivaluedMapMock();

		expect(inputPartMock.getHeaders()).andReturn(headersMock);
		expect(headersMock.get(HttpHeaders.CONTENT_DISPOSITION)).andReturn(Collections.<String> emptyList());

		replayAll();
		assertNull(this.httpUtils.extractFileName(inputPartMock));
		verifyAll();
	}

	@Test
	public void testExtractFileNameWhenNoHeaders() throws Exception {
		final BodyPart inputPartMock = createMock(BodyPart.class);
		final MultivaluedMap<String, String> headersMock = createMultivaluedMapMock();

		expect(inputPartMock.getHeaders()).andReturn(headersMock);
		expect(headersMock.get(HttpHeaders.CONTENT_DISPOSITION)).andReturn(null);

		replayAll();
		assertNull(this.httpUtils.extractFileName(inputPartMock));
		verifyAll();
	}

	@SuppressWarnings("unchecked")
	private MultivaluedMap<String, String> createMultivaluedMapMock() {
		return createMock(MultivaluedMap.class);
	}
}
