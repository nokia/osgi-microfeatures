/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource;

import static com.nsn.ood.cls.model.test.LinkTestUtil.link;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.util.ApiVersionChooser;
import com.nsn.ood.cls.core.util.ApiVersionChooser.API_VERSION;
import com.nsn.ood.cls.model.CLSMediaType;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.rest.util.ResourceBuilder;
import com.nsn.ood.cls.rest.util.ResourceBuilderFactory;
import com.nsn.ood.cls.rest.util.ResponseBuilder;
import com.nsn.ood.cls.rest.util.ResponseBuilderFactory;


/**
 * @author marynows
 *
 */
public class ApiResourceTest {
	private ApiResource resource;
	private ResourceBuilderFactory resourceBuilderFactoryMock;
	private ResponseBuilderFactory responseBuilderFactoryMock;
	private ApiVersionChooser apiMock;

	@Before
	public void setUp() throws Exception {
		this.resourceBuilderFactoryMock = createMock(ResourceBuilderFactory.class);
		this.responseBuilderFactoryMock = createMock(ResponseBuilderFactory.class);
		this.apiMock = createMock(ApiVersionChooser.class);
		this.resource = new ApiResource();
		setInternalState(this.resource, this.resourceBuilderFactoryMock, this.responseBuilderFactoryMock, this.apiMock);
	}

	@Test
	public void testGetVersions1_1() throws Exception {
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		final Resource resourceMock = createMock(Resource.class);

		expect(this.apiMock.getCurrentVersion()).andReturn(API_VERSION.VERSION_1_1);

		expect(this.resourceBuilderFactoryMock.create()).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.links("versions", link(CLSApplication.VERSION, CLSMediaType.APPLICATION_CLS_JSON)))
				.andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resourceMock);
		expect(this.responseBuilderFactoryMock.ok(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.header("Feature-Level", "v1_1")).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);

		replayAll();
		assertEquals(responseMock, this.resource.getVersions());
		verifyAll();
	}

	@Test
	public void testGetVersions1_0() throws Exception {
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		final Resource resourceMock = createMock(Resource.class);

		expect(this.apiMock.getCurrentVersion()).andReturn(API_VERSION.VERSION_1_0);

		expect(this.resourceBuilderFactoryMock.create()).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.links("versions", link(CLSApplication.VERSION, CLSMediaType.APPLICATION_CLS_JSON)))
				.andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resourceMock);
		expect(this.responseBuilderFactoryMock.ok(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);

		replayAll();
		assertEquals(responseMock, this.resource.getVersions());
		verifyAll();
	}

	@Test
	public void testGetResources() throws Exception {
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		final Resource resourceMock = createMock(Resource.class);

		expect(this.resourceBuilderFactoryMock.create()).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.links("clients", link("/clients", CLSMediaType.APPLICATION_CLIENT_JSON)))
				.andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.links("licenses", link("/licenses", CLSMediaType.APPLICATION_LICENSE_JSON)))
				.andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resourceMock);
		expect(this.responseBuilderFactoryMock.ok(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);

		replayAll();
		assertEquals(responseMock, this.resource.getResources());
		verifyAll();
	}
}
