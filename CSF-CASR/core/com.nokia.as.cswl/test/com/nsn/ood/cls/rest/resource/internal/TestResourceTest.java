/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource.internal;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.service.internal.TestService;
import com.nsn.ood.cls.model.internal.Tasks;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	IOUtils.class })
public class TestResourceTest {
	private TestResource resource;
	private TestService testServiceMock;

	@Before
	public void setUp() throws Exception {
		this.testServiceMock = createMock(TestService.class);

		this.resource = new TestResource();
		setInternalState(this.resource, this.testServiceMock);
	}

	@Test
	public void testUploadUri() throws Exception {
		assertEquals("/internal/test/upload", TestResource.UPLOAD_URI);
	}

	@Test
	public void testUpload() throws Exception {
		assertEquals(IOUtils.toString(getClass().getResourceAsStream("/test/upload.html")), this.resource.upload());
	}

	@Test
	public void testUploadWithException() throws Exception {
		mockStatic(IOUtils.class);

		expect(IOUtils.toString(anyObject(InputStream.class))).andThrow(new IOException("message"));

		replayAll();
		assertEquals("message", this.resource.upload());
		verifyAll();
	}

	@Test
	public void testReloadTasks() throws Exception {
		final Tasks tasks = new Tasks();

		this.testServiceMock.reloadTasks(tasks);

		replayAll();
		final Response response = this.resource.reloadTasks(tasks);
		verifyAll();

		assertEquals(Status.NO_CONTENT, response.getStatusInfo());
		assertNull(response.getEntity());
	}

	@Test
	public void testReloadTargetId() throws Exception {
		expect(this.testServiceMock.reloadTargetId()).andReturn("1234");

		replayAll();
		final Response response = this.resource.reloadTargetId();
		verifyAll();

		assertEquals(Status.OK, response.getStatusInfo());
		assertEquals("1234", response.getEntity());
	}

	@Test
	public void testRunTask() throws Exception {
		this.testServiceMock.runTask("taskName");

		replayAll();
		final Response response = this.resource.runTask("taskName");
		verifyAll();

		assertEquals(Status.NO_CONTENT, response.getStatusInfo());
		assertNull(response.getEntity());
	}
}
