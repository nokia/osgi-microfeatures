/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource.internal;

import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.activityDetail;
import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.activityDetailsList;
import static com.nsn.ood.cls.model.test.LinkTestUtil.assertLink;
import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.internal.ActivityService;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.metadata.MetaData;
import com.nsn.ood.cls.model.internal.ActivityDetail;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.rest.resource.AbstractResourceTest;
import com.nsn.ood.cls.rest.resource.BaseResource;
import com.nsn.ood.cls.rest.util.ResourceBuilder;
import com.nsn.ood.cls.rest.util.ResourceBuilderFactory.Links;


/**
 * @author marynows
 * 
 */
public class ActivityDetailsResourceTest extends AbstractResourceTest {
	private static final Long ACTIVITY_ID = 1L;

	private ActivityDetailsResource resource;
	private ActivityService activityServiceMock;
	private BaseResource base;

	@Before
	public void setUp() throws Exception {
		this.resource = new ActivityDetailsResource();
		this.activityServiceMock = createMock(ActivityService.class);
		this.base = createMock(BaseResource.class);
		init(base);
		
		setInternalState(this.resource, this.base, this.uriInfo2ConditionsConverter, resourceId(ACTIVITY_ID.toString()), this.activityServiceMock);
		this.base.init(EasyMock.anyObject(), EasyMock.eq("activities"), EasyMock.eq("1"), EasyMock.eq("details"));
	}

	@Test
	public void testGetDetails() throws Exception {
		final UriInfo uriInfoMock = createMock(UriInfo.class);
		Conditions conditions = mockUriConditions(uriInfoMock, this.uriInfo2ConditionsConverter);
		
		final List<ActivityDetail> activityDetails = activityDetailsList(activityDetail("1"), activityDetail("2"));
		final MetaData metaData = metaData(20L, 10L);
		final MetaDataList<ActivityDetail> metadataList = new MetaDataList<>(activityDetails, metaData);
		expect(this.activityServiceMock.getActivityDetails(ACTIVITY_ID, conditions)).andReturn(metadataList);
		
		final Capture<Links> capturedLinks = new Capture<>();
		final Response responseMock = createMock(Response.class);
		final ResourceBuilder resourceBuilderMock = mockBuilders(base, responseMock);
		
		expect(this.base.links(conditions, metaData.getFiltered())).andReturn(new Links(new Link().withHref(("/activities/1/details?query")), null, null));
		expect(this.resourceBuilderFactoryMock.metaData(capture(capturedLinks), eq(metaData))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.activityDetails(activityDetails)).andReturn(resourceBuilderMock);
		
		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getDetails(uriInfoMock));
		verifyAll();
		assertLinks(capturedLinks.getValue(), "/activities/1/details?query");
	}

	@Test
	public void testGetDetailsAndExpectError() throws Exception {
		final UriInfo uriInfoMock = createMock(UriInfo.class);
		Conditions conditions = mockUriConditions(uriInfoMock, this.uriInfo2ConditionsConverter);
		final ServiceException exceptionMock = createMock(ServiceException.class);
		expect(this.activityServiceMock.getActivityDetails(ACTIVITY_ID, conditions)).andThrow(exceptionMock);
		
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getDetails(uriInfoMock));
		verifyAll();
	}

	@Test
	public void testGetFilters() throws Exception {
		final Capture<Link> capturedLink = new Capture<>();
		final List<String> filterValues = Arrays.asList("filter1", "filter2");

		expect(this.activityServiceMock.getActivityDetailFilterValues(ACTIVITY_ID, "fff")).andReturn(filterValues);
		
		final Response responseMock = createMock(Response.class);
		final ResourceBuilder resourceBuilderMock = mockBuilders(base, responseMock);
		expect(this.base.link("filters", "fff")).andReturn(new Link().withHref("/activities/1/details/filters/fff"));
		expect(this.resourceBuilderFactoryMock.selfLink(capture(capturedLink))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.filterValues(filterValues)).andReturn(resourceBuilderMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getFilters("fff"));
		verifyAll();

		assertLink(capturedLink.getValue(), "/activities/1/details/filters/fff");
	}

	@Test
	public void testGetFiltersAndExpectError() throws Exception {
		final ServiceException exceptionMock = createMock(ServiceException.class);

		expect(this.activityServiceMock.getActivityDetailFilterValues(ACTIVITY_ID, "fff")).andThrow(exceptionMock);
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getFilters("fff"));
		verifyAll();
	}
}
