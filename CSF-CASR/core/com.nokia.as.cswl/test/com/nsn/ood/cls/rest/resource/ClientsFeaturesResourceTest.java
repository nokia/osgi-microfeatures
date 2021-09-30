/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource;

import static com.nsn.ood.cls.core.test.ClientTagTestUtil.clientTag;
import static com.nsn.ood.cls.core.test.ErrorExceptionTestUtil.errorExceptionsList;
import static com.nsn.ood.cls.core.test.ObjectWithTagTestUtil.featuresWithTag;
import static com.nsn.ood.cls.model.test.ErrorTestUtil.error;
import static com.nsn.ood.cls.model.test.FeatureErrorTestUtil.featureError;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.allocation;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.assertFeature;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.assertFeatureAllocations;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.feature;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featureCapacity;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featureOnOff;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.features;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featuresList;
import static com.nsn.ood.cls.model.test.LinkTestUtil.assertLink;
import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
import static com.nsn.ood.cls.util.test.AnnotationTestUtil.assertAnnotation;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.easymock.PowerMock;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.convert.Feature2StringConverter;
import com.nsn.ood.cls.core.model.FeaturesWithTag;
import com.nsn.ood.cls.core.service.FeaturesService;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.features.Features;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.model.gen.metadata.MetaData;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.rest.ValidateETag;
import com.nsn.ood.cls.rest.convert.ErrorException2StringConverter;
import com.nsn.ood.cls.rest.exception.ViolationException;
import com.nsn.ood.cls.rest.query.UriInfo2ConditionsConverter;
import com.nsn.ood.cls.rest.util.ResourceBuilder;
import com.nsn.ood.cls.rest.util.ResourceBuilderFactory.Links;
import com.nsn.ood.cls.rest.util.ResponseBuilder;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class ClientsFeaturesResourceTest extends AbstractResourceTest {
	private static final String CLIENT_ID = "ABC123";

	private ClientsFeaturesResource resource;
	private FeaturesService featuresServiceMock;
	private ServiceExceptionFactory serviceExceptionFactoryMock;
	private Converter<UriInfo, Conditions> uriInfo2ConditionsConverter;
	private Converter<Feature, String> feature2StringConverter;
	private BaseResource base;

	@Before
	public void setUp() throws Exception {
		this.featuresServiceMock = createMock(FeaturesService.class);
		this.serviceExceptionFactoryMock = createMock(ServiceExceptionFactory.class);
		this.uriInfo2ConditionsConverter = createMock(UriInfo2ConditionsConverter.class);
		this.feature2StringConverter = createMock(Feature2StringConverter.class);
		this.base = createMock(BaseResource.class);
		init(base);

		this.resource = new ClientsFeaturesResource();
		setInternalState(this.resource, this.featuresServiceMock, this.serviceExceptionFactoryMock, resourceId(CLIENT_ID.toString()), base);
		setInternalState(this.resource, "uriInfo2ConditionsConverter", uriInfo2ConditionsConverter);
		setInternalState(this.resource, "feature2StringConverter", feature2StringConverter);
		this.base.init(EasyMock.anyObject(), EasyMock.eq("clients"), EasyMock.eq("ABC123"), EasyMock.eq("features"));
	}

	@Test
	public void testReserveCapacity() throws Exception {
		final Features features = features(featureCapacity(1234L, 50L), featureOnOff(2345L));
		final Capture<Link> capturedLink = new Capture<>();
		
		this.base.logInit(EasyMock.eq("Reserve capacity"), EasyMock.anyObject(String.class));
		mockLogInit(features.getFeatures());
		final FeaturesWithTag featuresWithTag = featuresWithTag(features.getFeatures(), clientTag());
		expect(this.featuresServiceMock.reserveCapacity(CLIENT_ID, features.getFeatures())).andReturn(featuresWithTag);
		this.base.logSuccess(EasyMock.eq("Reserve capacity"), EasyMock.anyObject(String.class));
		mockLogSuccess(featuresWithTag.getObject());
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		final Resource resourceMock = mockCreateResource(features.getFeatures(), capturedLink);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.created(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.tag(clientTag())).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		expect(this.base.link(new String[0])).andReturn(new Link().withHref("/clients/ABC123/features"));
		
		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.reserveCapacity(features));
		verifyAll();

		assertLink(capturedLink.getValue(), "/clients/ABC123/features");
	}

	@Test
	public void testReserveCapacityForFeature() throws Exception {
		final Features features = features(featureCapacity(1234L, 50L), featureOnOff(1234L));
		final Capture<Link> capturedLink = new Capture<>();

		this.base.logInit(EasyMock.eq("Reserve capacity"), EasyMock.anyObject(String.class));
		mockLogInit(features.getFeatures());
		final FeaturesWithTag featuresWithTag = featuresWithTag(features.getFeatures(), clientTag());
		expect(this.featuresServiceMock.reserveCapacity(CLIENT_ID, features.getFeatures())).andReturn(featuresWithTag);
		this.base.logSuccess(EasyMock.eq("Reserve capacity"), EasyMock.anyObject(String.class));
		mockLogSuccess(featuresWithTag.getObject());
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		final Resource resourceMock = mockCreateResource(features.getFeatures(), capturedLink);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.created(resourceMock)).andReturn(responseBuilderMock);
		if (clientTag() != null) {
			expect(responseBuilderMock.tag(clientTag())).andReturn(responseBuilderMock);
		}
		expect(responseBuilderMock.build()).andReturn(responseMock);
		String[] paths = new String[1];
		paths[0] = "1234";
		expect(this.base.link(paths)).andReturn(new Link().withHref("/clients/ABC123/features/1234"));

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.reserveCapacity("1234", features));
		verifyAll();

		assertLink(capturedLink.getValue(), "/clients/ABC123/features/1234");
	}

	@Test
	public void testReserveCapacityForFeatureWithWrongFeatureCode() throws Exception {
		final ViolationException exceptionMock = createMock(ViolationException.class);

		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);
		expect(this.violationExceptionBuilderFactoryMock.valueException("features.featureCode", "xxx")).andReturn(
				exceptionMock);

		replayAll();
		try {
			this.resource.start();
			this.resource.reserveCapacity("xxx", null);
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testReserveCapacityAndExpectError() throws Exception {
		final Features features = features(featureCapacity(1234L, 50L), featureOnOff(2345L));

		final ServiceException exceptionMock = createMock(ServiceException.class);
		this.base.logInit(EasyMock.eq("Reserve capacity"), EasyMock.anyObject(String.class));
		mockLogInit(features.getFeatures());
		expect(this.featuresServiceMock.reserveCapacity(CLIENT_ID, features.getFeatures())).andThrow(exceptionMock);
		this.base.logFailure("Reserve capacity", exceptionMock);
		
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.reserveCapacity(features));
		verifyAll();
	}

	@Test
	public void testReserveCapacityForFeatureAndExpectError() throws Exception {
		final Features features = features(featureCapacity(1234L, 50L), featureOnOff(1234L));

		final ServiceException exceptionMock = createMock(ServiceException.class);
		this.base.logInit(EasyMock.eq("Reserve capacity"), EasyMock.anyObject(String.class));
		mockLogInit(features.getFeatures());
		expect(this.featuresServiceMock.reserveCapacity(CLIENT_ID, features.getFeatures())).andThrow(exceptionMock);
		this.base.logFailure("Reserve capacity", exceptionMock);
		
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.reserveCapacity("1234", features));
		verifyAll();
	}

	@Test
	public void testReserveCapacityAndExpectExceptionWhenNoFeatures() throws Exception {
		testReserveCapacityAndExpectExceptionWhenNoFeatures(null);
		testReserveCapacityAndExpectExceptionWhenNoFeatures(features((List<Feature>) null));
		testReserveCapacityAndExpectExceptionWhenNoFeatures(features(Collections.<Feature> emptyList()));
	}

	private void testReserveCapacityAndExpectExceptionWhenNoFeatures(final Features features) {
		resetAll();
		final ViolationException exceptionMock = createMock(ViolationException.class);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);
		expect(this.violationExceptionBuilderFactoryMock.pathException("features.oneOrMoreFeatures", "features"))
				.andReturn(exceptionMock);

		replayAll();
		try {
			this.resource.reserveCapacity(features);
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testReserveCapacityAndExpectExceptionForWrongFeature() throws Exception {
		testReserveCapacityAndExpectExceptionForWrongFeature(features(feature()), "features.missingFeatureCode", 0,
				"featureCode", null);
		testReserveCapacityAndExpectExceptionForWrongFeature(features(feature(1234L)), "features.missingType", 0,
				"type", null);
		testReserveCapacityAndExpectExceptionForWrongFeature(features(featureCapacity(1234L, -10L)),
				"features.missingCapacity", 0, "capacity", -10L);
		testReserveCapacityAndExpectExceptionForWrongFeature(features(featureCapacity(1234L, null)),
				"features.missingCapacity", 0, "capacity", null);
		testReserveCapacityAndExpectExceptionForWrongFeature(features(featureOnOff(1234L).withCapacity(10L)),
				"features.unexpectedCapacity", 0, "capacity", 10L);
		testReserveCapacityAndExpectExceptionForWrongFeature(
				features(featureCapacity(2345L, 10L), featureCapacity(1234L, -10L)), "features.missingCapacity", 1,
				"capacity", -10L);
	}

	private void testReserveCapacityAndExpectExceptionForWrongFeature(final Features features,
			final String expectedMessage, final int expectedIndex, final String expectedField,
			final Object expectedValue) {
		resetAll();
		final ViolationException exceptionMock = createMock(ViolationException.class);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);

		expect(
				this.violationExceptionBuilderFactoryMock.featureException(expectedMessage, expectedIndex,
						expectedField, expectedValue)).andReturn(exceptionMock);

		replayAll();
		try {
			this.resource.reserveCapacity(features);
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testReserveCapacityForFeatureAndExpectExceptionWhenNoFeatures() throws Exception {
		testReserveCapacityForFeatureAndExpectExceptionWhenNoFeatures(null);
		testReserveCapacityForFeatureAndExpectExceptionWhenNoFeatures(features((List<Feature>) null));
		testReserveCapacityForFeatureAndExpectExceptionWhenNoFeatures(features(Collections.<Feature> emptyList()));
	}

	private void testReserveCapacityForFeatureAndExpectExceptionWhenNoFeatures(final Features features) {
		resetAll();
		final ViolationException exceptionMock = createMock(ViolationException.class);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);

		expect(this.violationExceptionBuilderFactoryMock.pathException("features.oneOrMoreFeatures", "features"))
				.andReturn(exceptionMock);

		replayAll();
		try {
			this.resource.reserveCapacity("1234", features);
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testReserveCapacityForFeatureAndExpectExceptionForWrongFeature() throws Exception {
		testReserveCapacityForFeatureAndExpectExceptionForWrongFeature(features(feature()),
				"features.missingFeatureCode", 0, "featureCode", null);
		testReserveCapacityForFeatureAndExpectExceptionForWrongFeature(features(feature(1234L)),
				"features.missingType", 0, "type", null);
		testReserveCapacityForFeatureAndExpectExceptionForWrongFeature(features(featureCapacity(1234L, -10L)),
				"features.missingCapacity", 0, "capacity", -10L);
		testReserveCapacityForFeatureAndExpectExceptionForWrongFeature(features(featureCapacity(1234L, null)),
				"features.missingCapacity", 0, "capacity", null);
		testReserveCapacityForFeatureAndExpectExceptionForWrongFeature(features(featureOnOff(1234L).withCapacity(10L)),
				"features.unexpectedCapacity", 0, "capacity", 10L);
		testReserveCapacityForFeatureAndExpectExceptionForWrongFeature(
				features(featureCapacity(1234L, 10L), feature(1234L)), "features.missingType", 1, "type", null);
		testReserveCapacityForFeatureAndExpectExceptionForWrongFeature(features(featureCapacity(2345L, 50L)),
				"features.matchResource", 0, "featureCode", 2345L);
	}

	private void testReserveCapacityForFeatureAndExpectExceptionForWrongFeature(final Features features,
			final String expectedMessage, final int expectedIndex, final String expectedField,
			final Object expectedValue) {
		resetAll();
		final ViolationException exceptionMock = createMock(ViolationException.class);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);

		expect(
				this.violationExceptionBuilderFactoryMock.featureException(expectedMessage, expectedIndex,
						expectedField, expectedValue)).andReturn(exceptionMock);

		replayAll();
		try {
			this.resource.reserveCapacity("1234", features);
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testGetFeatures() throws Exception {
		final UriInfo uriInfoMock = createMock(UriInfo.class);
		final Capture<Links> capturedLinks = new Capture<>();

		final List<Feature> features = featuresList(featureCapacity(1234L, 10L, allocation(10L, "1234567890")),
				featureOnOff(2345L, allocation(1L, "2345678901")));
		final MetaData metaData = metaData(20L, 10L);

		final Conditions conditions = mockUriConditions(uriInfoMock, this.uriInfo2ConditionsConverter);
		expect(this.featuresServiceMock.getFeatures(CLIENT_ID, conditions)).andReturn(
				new MetaDataList<>(features, metaData));
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		expect(this.resourceBuilderFactoryMock.metaData(capture(capturedLinks), eq(metaData))).andReturn(resourceBuilderMock);
		final Resource resourceMock = mockFeaturesAndBuild(resourceBuilderMock, features);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.ok(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		expect(this.base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(this.base.links(conditions, metaData.getFiltered())).andReturn(new Links(new Link().withHref(("/clients/ABC123/features?query")), null, null));

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getFeatures(uriInfoMock));
		verifyAll();

		assertLinks(capturedLinks.getValue(), "/clients/ABC123/features?query");
		assertFeature(features.get(0), 1234L, Feature.Type.CAPACITY, 10L);
		assertFeatureAllocations(features.get(0), allocation(10L, "/licenses/1234567890"));
		assertFeature(features.get(1), 2345L, Feature.Type.ON_OFF, null);
		assertFeatureAllocations(features.get(1), allocation(1L, "/licenses/2345678901"));
	}

	@Test
	public void testGetFeaturesAndExpectError() throws Exception {
		final UriInfo uriInfoMock = createMock(UriInfo.class);
		final ServiceException exceptionMock = createMock(ServiceException.class);

		final Conditions conditions = mockUriConditions(uriInfoMock, this.uriInfo2ConditionsConverter);
		expect(this.featuresServiceMock.getFeatures(CLIENT_ID, conditions)).andThrow(exceptionMock);
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getFeatures(uriInfoMock));
		verifyAll();
	}

	@Test
	public void testGetFeature() throws Exception {
		final Capture<Link> capturedLink = new Capture<>();
		final Capture<Conditions> capturedConditions = new Capture<>();
		final List<Feature> features = featuresList(featureCapacity(1234L, 10L, allocation(10L, "1234567890")));

		expect(this.featuresServiceMock.getFeatures(eq(CLIENT_ID), capture(capturedConditions))).andReturn(
				new MetaDataList<>(features, metaData()));
		final Resource resourceMock = mockCreateResource(features, capturedLink);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.ok(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		expect(this.base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(this.base.link("1234")).andReturn(new Link().withHref("/clients/ABC123/features/1234"));

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getFeature("1234"));
		verifyAll();

		assertLink(capturedLink.getValue(), "/clients/ABC123/features/1234");
		assertFeature(features.get(0), 1234L, Feature.Type.CAPACITY, 10L);
		assertFeatureAllocations(features.get(0), allocation(10L, "/licenses/1234567890"));
		assertFeatureConditions(capturedConditions.getValue(), "1234");
	}

	@Test
	public void testGetFeatureWithWrongFeatureCode() throws Exception {
		final ViolationException exceptionMock = createMock(ViolationException.class);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);

		expect(this.violationExceptionBuilderFactoryMock.valueException("features.featureCode", "xxx")).andReturn(
				exceptionMock);

		replayAll();
		try {
			this.resource.start();
			this.resource.getFeature("xxx");
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testGetFeatureAndExpectError() throws Exception {
		final ServiceException exceptionMock = createMock(ServiceException.class);
		final Capture<Conditions> capturedConditions = new Capture<>();

		expect(this.featuresServiceMock.getFeatures(eq(CLIENT_ID), capture(capturedConditions)))
				.andThrow(exceptionMock);
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getFeature("1234"));
		verifyAll();

		assertFeatureConditions(capturedConditions.getValue(), "1234");
	}

	@Test
	public void testGetFeatureAndExpectNotFoundError() throws Exception {
		final ServiceException exceptionMock = createMock(ServiceException.class);
		final Capture<Conditions> capturedConditions = new Capture<>();

		expect(this.featuresServiceMock.getFeatures(eq(CLIENT_ID), capture(capturedConditions))).andReturn(
				new MetaDataList<>(Collections.<Feature> emptyList(), metaData()));
		expect(this.serviceExceptionFactoryMock.featureNotFound(featureError(1234L))).andReturn(exceptionMock);
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getFeature("1234"));
		verifyAll();

		assertFeatureConditions(capturedConditions.getValue(), "1234");
	}

	private Resource mockCreateResource(final List<Feature> features, final Capture<Link> link) {
		final ResourceBuilder resourceBuilderMock = mockResourceBuilderWithSelfLink(link);
		return mockFeaturesAndBuild(resourceBuilderMock, features);
	}
	
	private ResourceBuilder mockResourceBuilderWithSelfLink(final Capture<Link> capturedLink) {
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		expect(this.resourceBuilderFactoryMock.selfLink(capture(capturedLink))).andReturn(resourceBuilderMock);
		return resourceBuilderMock;
	}

	private Resource mockFeaturesAndBuild(final ResourceBuilder resourceBuilderMock, final List<Feature> features) {
		final Resource resourceMock = createMock(Resource.class);
		expect(resourceBuilderMock.features(features)).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resourceMock);
		return resourceMock;
	}

	private void assertFeatureConditions(final Conditions conditions, final String expectedFeatureCode) {
		assertEquals(ConditionsBuilder.createAndSkipMetaData().equalFilter("featureCode", expectedFeatureCode).build(),
				conditions);
	}

	@Test
	public void testReleaseCapacity() throws Exception {
		final HttpHeaders headersMock = createMock(HttpHeaders.class);
		this.base.logInit(EasyMock.eq("Release capacity"), EasyMock.anyObject(String.class));
		this.base.logSuccess(EasyMock.eq("Release capacity"));
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(headersMock.getHeaderString("Force")).andReturn(null);
		final Response responseMock = mockReleaseCapacityForFeatures(Collections.<Long> emptyList(), false);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.releaseCapacity(headersMock));
		verifyAll();
	}

	@Test
	public void testReleaseCapacityForFeature() throws Exception {
		final HttpHeaders headersMock = createMock(HttpHeaders.class);

		expect(headersMock.getHeaderString("Force")).andReturn("true");
		this.base.logInit(EasyMock.eq("Release capacity"), EasyMock.anyObject(String.class));
		this.base.logSuccess(EasyMock.eq("Release capacity"));
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		final Response responseMock = mockReleaseCapacityForFeatures(Arrays.asList(1234L), true);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.releaseCapacity(headersMock, "1234"));
		verifyAll();
	}

	private Response mockReleaseCapacityForFeatures(final List<Long> featureCodes, final boolean force)
			throws ServiceException {
		this.featuresServiceMock.releaseCapacity(CLIENT_ID, featureCodes, force);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.noContent()).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		return responseMock;
	}

	@Test
	public void testReleaseCapacityForFeatureWithWrongFeatureCode() throws Exception {
		final ViolationException exceptionMock = createMock(ViolationException.class);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);
		expect(this.violationExceptionBuilderFactoryMock.valueException("features.featureCode", "xxx")).andReturn(
				exceptionMock);

		replayAll();
		try {
			this.resource.start();
			this.resource.releaseCapacity(null, "xxx");
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testReleaseCapacityAndExpectError() throws Exception {
		final HttpHeaders headersMock = createMock(HttpHeaders.class);

		expect(headersMock.getHeaderString("Force")).andReturn("true");
		final Response responseMock = mockReleaseCapacityForFeaturesAndExpectError(Collections.<Long> emptyList(), true);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.releaseCapacity(headersMock));
		verifyAll();
	}

	@Test
	public void testReleaseCapacityForFeatureAndExpectError() throws Exception {
		final HttpHeaders headersMock = createMock(HttpHeaders.class);

		expect(headersMock.getHeaderString("Force")).andReturn("test");
		final Response responseMock = mockReleaseCapacityForFeaturesAndExpectError(Arrays.asList(1234L), false);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.releaseCapacity(headersMock, "1234"));
		verifyAll();
	}

	private Response mockReleaseCapacityForFeaturesAndExpectError(final List<Long> featureCodes, final boolean force)
			throws ServiceException {
		final ServiceException exceptionMock = createMock(ServiceException.class);

		this.featuresServiceMock.releaseCapacity(CLIENT_ID, featureCodes, force);
		expectLastCall().andThrow(exceptionMock);
		this.base.logInit(EasyMock.eq("Release capacity"), EasyMock.anyObject(String.class));
		this.base.logFailure("Release capacity", exceptionMock);
		Converter<ErrorException, String> converter = createMock(ErrorException2StringConverter.class);
		errorExceptionsList().forEach(e -> expect(converter.convertTo(e)).andReturn("error"));
		final Response responseMock = createMock(Response.class);
		errorExceptionsList().forEach(e -> expect(errorException2ErrorConverter.convertTo(e)).andReturn(error(2L)));
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);
		return responseMock;
	}

	private void mockLogInit(final List<Feature> features) {
		features.forEach(f -> expect(this.feature2StringConverter.convertTo(f)).andReturn("log"));
	}

	private void mockLogSuccess(final List<Feature> features) {
		features.forEach(f -> expect(this.feature2StringConverter.convertTo(f)).andReturn("log"));
	}

	@Test
	public void testAnnotations() throws Exception {
		assertAnnotation(ClientsFeaturesResource.class.getMethod("reserveCapacity", Features.class), ValidateETag.class);
		assertAnnotation(ClientsFeaturesResource.class.getMethod("reserveCapacity", String.class, Features.class),
				ValidateETag.class);
		PowerMock.resetAll();
	}
}
