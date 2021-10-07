/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource;

import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.licensesList;
import static com.nsn.ood.cls.model.test.LinkTestUtil.assertLink;
import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.service.LicensesService;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.gen.metadata.MetaData;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.rest.exception.ViolationException;
import com.nsn.ood.cls.rest.util.ResourceBuilder;
import com.nsn.ood.cls.rest.util.ResourceBuilderFactory.Links;
import com.nsn.ood.cls.rest.util.ResponseBuilder;


/**
 * @author marynows
 * 
 */
public class LicensesResourceTest extends AbstractResourceTest {
	private LicensesResource resource;
	private LicensesService licensesServiceMock;
	private ServiceExceptionFactory serviceExceptionFactoryMock;
	private BaseResource base;

	@Before
	public void setUp() throws Exception {
		this.licensesServiceMock = createMock(LicensesService.class);
		this.serviceExceptionFactoryMock = createMock(ServiceExceptionFactory.class);

		this.resource = new LicensesResource();
		this.base = createMock(BaseResource.class);
		init(base);
		setInternalState(this.resource, this.licensesServiceMock, this.serviceExceptionFactoryMock, this.base, this.uriInfo2ConditionsConverter);
		this.base.init(EasyMock.anyObject(), EasyMock.eq("licenses"));
	}

	@Test
	public void testGetLicenses() throws Exception {
		final UriInfo uriInfoMock = createMock(UriInfo.class);
		final Capture<Links> capturedLinks = new Capture<>();
		final List<License> licenses = licensesList(license("1234"), license("2345"));
		final MetaData metaData = metaData(20L, 10L);

		final Conditions conditions = mockUriConditions(uriInfoMock, this.uriInfo2ConditionsConverter);
		expect(this.licensesServiceMock.getLicenses(conditions)).andReturn(new MetaDataList<>(licenses, metaData));
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		expect(this.resourceBuilderFactoryMock.metaData(capture(capturedLinks), eq(metaData))).andReturn(resourceBuilderMock);
		final Response responseMock = mockGetLicensesResponse(resourceBuilderMock, licenses);
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(this.base.links(conditions, metaData.getFiltered())).andReturn(new Links(new Link().withHref(("/licenses?query")), null, null));

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getLicenses(uriInfoMock));
		verifyAll();

		assertLinks(capturedLinks.getValue(), "/licenses?query");
	}

	@Test
	public void testGetLicensesAndExpectError() throws Exception {
		final UriInfo uriInfoMock = createMock(UriInfo.class);
		final ServiceException exceptionMock = createMock(ServiceException.class);

		final Conditions conditions = mockUriConditions(uriInfoMock, this.uriInfo2ConditionsConverter);
		expect(this.licensesServiceMock.getLicenses(conditions)).andThrow(exceptionMock);
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getLicenses(uriInfoMock));
		verifyAll();
	}

	@Test
	public void testGetLicense() throws Exception {
		final Capture<Conditions> capturedConditions = new Capture<>();
		final Capture<Link> capturedLink = new Capture<>();
		final List<License> licenses = licensesList(license("1234"));

		expect(this.licensesServiceMock.getLicenses(capture(capturedConditions))).andReturn(
				new MetaDataList<>(licenses, metaData()));
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		expect(this.resourceBuilderFactoryMock.selfLink(capture(capturedLink))).andReturn(resourceBuilderMock);
		final Response responseMock = mockGetLicensesResponse(resourceBuilderMock, licenses);
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(this.base.link("1234")).andReturn(new Link().withHref("/licenses/1234"));

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getLicense("1234"));
		verifyAll();

		assertLink(capturedLink.getValue(), "/licenses/1234");
		assertLicenseConditions(capturedConditions.getValue(), "1234");
	}

	@Test
	public void testGetLicenseWithWrongSerialNumber() throws Exception {
		final ViolationException exceptionMock = createMock(ViolationException.class);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);
		expect(this.violationExceptionBuilderFactoryMock.valueException("licenses.serialNumber", "xxx")).andReturn(
				exceptionMock);

		replayAll();
		try {
			this.resource.start();
			this.resource.getLicense("xxx");
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testGetLicenseAndExpectError() throws Exception {
		final ServiceException exceptionMock = createMock(ServiceException.class);
		final Capture<Conditions> capturedConditions = new Capture<>();

		expect(this.licensesServiceMock.getLicenses(capture(capturedConditions))).andThrow(exceptionMock);
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getLicense("1234"));
		verifyAll();

		assertLicenseConditions(capturedConditions.getValue(), "1234");
	}

	@Test
	public void testGetLicenseAndExpectNotFoundError() throws Exception {
		final ServiceException exceptionMock = createMock(ServiceException.class);
		final Capture<Conditions> capturedConditions = new Capture<>();

		expect(this.licensesServiceMock.getLicenses(capture(capturedConditions))).andReturn(
				new MetaDataList<>(Collections.<License> emptyList(), metaData()));
		expect(this.serviceExceptionFactoryMock.licenseNotFound(license("1234").withTargets(null).withFeatures(null)))
				.andReturn(exceptionMock);
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getLicense("1234"));
		verifyAll();

		assertLicenseConditions(capturedConditions.getValue(), "1234");
	}

	private Response mockGetLicensesResponse(final ResourceBuilder resourceBuilderMock, final List<License> licenses) {
		final Resource resourceMock = createMock(Resource.class);
		expect(resourceBuilderMock.licenses(licenses)).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resourceMock);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.ok(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		return responseMock;
	}

	private void assertLicenseConditions(final Conditions conditions, final String expectedSerialNumber) {
		assertEquals(ConditionsBuilder.createAndSkipMetaData().equalFilter("serialNumber", expectedSerialNumber)
				.build(), conditions);
	}
}
