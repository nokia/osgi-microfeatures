/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource.internal;

import static com.nsn.ood.cls.model.internal.test.ActivityTestUtil.activitiesList;
import static com.nsn.ood.cls.model.internal.test.ActivityTestUtil.activity;
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
import com.nsn.ood.cls.model.internal.Activity;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.rest.exception.ViolationException;
import com.nsn.ood.cls.rest.resource.AbstractResourceTest;
import com.nsn.ood.cls.rest.resource.BaseResource;
import com.nsn.ood.cls.rest.resource.ResourceContextMock;
import com.nsn.ood.cls.rest.util.ResourceBuilder;
import com.nsn.ood.cls.rest.util.ResourceBuilderFactory.Links;


/**
 * @author marynows
 * 
 */
public class ActivityResourceTest extends AbstractResourceTest {
	private ActivityResource resource;
	private ActivityService activityServiceMock;
	private BaseResource base;

	@Before
	public void setUp() throws Exception {
		this.activityServiceMock = createMock(ActivityService.class);
		this.base = createMock(BaseResource.class);
		init(base);

		this.resource = new ActivityResource();
		setInternalState(this.resource, this.base, this.activityServiceMock, this.uriInfo2ConditionsConverter, new ResourceContextMock());
	}

	@Test
	public void testGetActivities() throws Exception {
		this.base.init(EasyMock.anyObject(), EasyMock.eq("activities"));
		final UriInfo uriInfoMock = createMock(UriInfo.class);
		Conditions conditions = mockUriConditions(uriInfoMock, this.uriInfo2ConditionsConverter);
		
		final List<Activity> activities = activitiesList(activity(1L), activity(2L));
		final MetaData metaData = metaData(20L, 10L);
		final MetaDataList<Activity> metadataList = new MetaDataList<>(activities, metaData);
		expect(this.activityServiceMock.getActivities(conditions)).andReturn(metadataList);

		final Capture<Links> capturedLinks = new Capture<>();
		final Response responseMock = createMock(Response.class);
		final ResourceBuilder resourceBuilderMock = mockBuilders(base, responseMock);
		expect(this.base.links(conditions, metaData.getFiltered())).andReturn(new Links(new Link().withHref(( "/activities?query")), null, null));
		expect(this.resourceBuilderFactoryMock.metaData(capture(capturedLinks), eq(metaData))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.activities(activities)).andReturn(resourceBuilderMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getActivities(uriInfoMock));
		verifyAll();

		assertLinks(capturedLinks.getValue(), "/activities?query");
	}

	@Test
	public void testGetActivitiesAndExpectError() throws Exception {
		this.base.init(EasyMock.anyObject(), EasyMock.eq("activities"));
		final UriInfo uriInfoMock = createMock(UriInfo.class);
		Conditions conditions = mockUriConditions(uriInfoMock, this.uriInfo2ConditionsConverter);
		final ServiceException exceptionMock = createMock(ServiceException.class);
		expect(this.activityServiceMock.getActivities(conditions)).andThrow(exceptionMock);
		
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getActivities(uriInfoMock));
		verifyAll();
	}

	@Test
	public void testGetFilters() throws Exception {
		this.base.init(EasyMock.anyObject(), EasyMock.eq("activities"));
		final Capture<Link> capturedLink = new Capture<>();
		final List<String> filterValues = Arrays.asList("filter1", "filter2");
		expect(this.activityServiceMock.getActivityFilterValues("fff")).andReturn(filterValues);

		final Response responseMock = createMock(Response.class);
		final ResourceBuilder resourceBuilderMock = mockBuilders(base, responseMock);
		expect(this.base.link("filters", "fff")).andReturn(new Link().withHref("/activities/filters/fff"));
		expect(this.resourceBuilderFactoryMock.selfLink(capture(capturedLink))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.filterValues(filterValues)).andReturn(resourceBuilderMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getFilters("fff"));
		verifyAll();

		assertLink(capturedLink.getValue(), "/activities/filters/fff");
	}

	@Test
	public void testGetFiltersAndExpectError() throws Exception {
		this.base.init(EasyMock.anyObject(), EasyMock.eq("activities"));
		final ServiceException exceptionMock = createMock(ServiceException.class);

		expect(this.activityServiceMock.getActivityFilterValues("fff")).andThrow(exceptionMock);
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getFilters("fff"));
		verifyAll();
	}

	@Test
	public void testGetActivityDetailsResourceWithWrongActivityId() throws Exception {
		final ViolationException exceptionMock = createMock(ViolationException.class);

		expect(this.violationExceptionBuilderFactoryMock.valueException("activities.activityId", "xxx")).andReturn(
				exceptionMock);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);

		replayAll();
		try {
			this.resource.getActivityDetailsResource("xxx");
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}
}
