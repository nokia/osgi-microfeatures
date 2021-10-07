/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.felix.dm.ComponentState;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.internal.ActivityService;
import com.nsn.ood.cls.model.CLSMediaType;
import com.nsn.ood.cls.model.internal.Activity;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.rest.resource.BaseResource;
import com.nsn.ood.cls.rest.resource.CLSApplication;
import com.nsn.ood.cls.rest.resource.ResourceId;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;

import io.swagger.v3.oas.annotations.Operation;


/**
 * @author wro50095
 * 
 */
@Component(provides = ActivityResource.class)
@Path(CLSApplication.INTERNAL + "/activities")
@Produces({
		CLSMediaType.APPLICATION_CLS_JSON, CLSMediaType.APPLICATION_ERROR_JSON })
@Loggable(duration = true)
public class ActivityResource {
	private static final Pattern ACTIVITY_ID_PATTERN = Pattern.compile("\\d+");
	
	@Inject
	private DependencyManager dm;

	@Context
	private ResourceContext resourceContext;
	
	@ServiceDependency
	private ActivityService activityService;
	
	@ServiceDependency
	private BaseResource baseResource;
	
	@ServiceDependency(filter = "(&(from=uriInfo)(to=conditions))")
	private Converter<UriInfo, Conditions> uriInfo2ConditionsConverter;
	
	@Start
	public void start() {
		baseResource.init(null, "activities");
	}

	@GET
	@Operation(hidden = true)
	public Response getActivities(@Context final UriInfo uriInfo) {
		final Conditions conditions = uriInfo2ConditionsConverter.convertTo(uriInfo);

		try {
			final MetaDataList<Activity> activities = this.activityService.getActivities(conditions.clone());
			return baseResource.getResponseFactory()
							   .ok(baseResource.getResourceFactory()
									   		   .metaData(baseResource.links(conditions, activities.getMetaData().getFiltered()), 
									   				     activities.getMetaData())
									   		   .activities(activities.getList())
									   		   .build())
							   .build();
		} catch (final ServiceException e) {
			return baseResource.exceptionResponse(e);
		}
	}

	@GET
	@Path("filters/{filterName}")
	@Operation(hidden = true)
	public Response getFilters(@PathParam("filterName") final String filterName) {
		try {
			final List<String> filterValues = this.activityService.getActivityFilterValues(filterName);

			return baseResource.getResponseFactory().ok(baseResource.getResourceFactory()//
					.selfLink(baseResource.link("filters", filterName)).filterValues(filterValues).build()).build();
		} catch (final ServiceException e) {
			return baseResource.exceptionResponse(e);
		}
	}

	@Path("{activityId}/details")
	@Operation(hidden = true)
	public ActivityDetailsResource getActivityDetailsResource(@PathParam("activityId") final String activityId) {
		verifyActivityId(activityId);
		ResourceId resourceId = new ResourceId();
		resourceId.setResourceId(activityId);
		
		Dictionary<String, Object> rIdProps = new Hashtable<String, Object>();
		rIdProps.put("resource", "activityDetails");
		dm.add(dm.createComponent()
		  .setInterface(ResourceId.class, rIdProps)
		  .setImplementation(resourceId));
		
		CountDownLatch cd = new CountDownLatch(1);
		org.apache.felix.dm.Component comp =	
			dm.createComponent()
			  .setInterface(ActivityDetailsResource.class, null)
			  .setImplementation(ActivityDetailsResource.class)
			  .add(dm.createServiceDependency().setService(ActivityService.class).setRequired(true))
			  .add(dm.createServiceDependency().setService(BaseResource.class).setRequired(true))
			  .add(dm.createServiceDependency().setService(ResourceId.class, "(resource=activityDetails)").setRequired(true))
			  .add(dm.createServiceDependency().setService(Converter.class, "(&(from=uriInfo)(to=conditions))").setRequired(true))		
			  .add((c, s) -> {
				  if(s.equals(ComponentState.STARTED)) cd.countDown();
			  });
		dm.add(comp);
		try {
			cd.await();
		} catch (InterruptedException e) { return null; }
		ActivityDetailsResource instance = comp.getInstance();
		return this.resourceContext.initResource(instance);
	}

	private void verifyActivityId(final String activityId) {
		if (!ACTIVITY_ID_PATTERN.matcher(activityId).matches()) {
			throw baseResource.getViolationFactory().valueException("activities.activityId", activityId);
		}
	}
}
