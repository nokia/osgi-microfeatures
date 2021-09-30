/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource;

import static com.nsn.ood.cls.model.test.LinkTestUtil.assertLink;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.reflect.Whitebox.setInternalState;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.rest.convert.ErrorException2ErrorConverter;
import com.nsn.ood.cls.rest.query.Conditions2QueryStringConverter;
import com.nsn.ood.cls.rest.query.UriInfo2ConditionsConverter;
import com.nsn.ood.cls.rest.util.ResourceBuilder;
import com.nsn.ood.cls.rest.util.ResourceBuilderFactory;
import com.nsn.ood.cls.rest.util.ResourceBuilderFactory.Links;
import com.nsn.ood.cls.rest.util.ResponseBuilder;
import com.nsn.ood.cls.rest.util.ResponseBuilderFactory;
import com.nsn.ood.cls.rest.util.RestUtil;
import com.nsn.ood.cls.rest.util.ViolationExceptionBuilderFactory;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 *
 */
public abstract class AbstractResourceTest {
	protected Converter<Conditions, String> conditions2QueryStringConverter;
	protected Converter<ErrorException, Error> errorException2ErrorConverter;
	protected Converter<UriInfo, Conditions> uriInfo2ConditionsConverter;
	protected ResourceBuilderFactory resourceBuilderFactoryMock;
	protected ResponseBuilderFactory responseBuilderFactoryMock;
	protected ViolationExceptionBuilderFactory violationExceptionBuilderFactoryMock;
	protected RestUtil restUtilMock;

	protected void init(final BaseResource resource) {
		this.conditions2QueryStringConverter = createMock(Conditions2QueryStringConverter.class);
		this.errorException2ErrorConverter = createMock(ErrorException2ErrorConverter.class);
		this.uriInfo2ConditionsConverter = createMock(UriInfo2ConditionsConverter.class);
		this.resourceBuilderFactoryMock = createMock(ResourceBuilderFactory.class);
		this.responseBuilderFactoryMock = createMock(ResponseBuilderFactory.class);
		this.violationExceptionBuilderFactoryMock = createMock(ViolationExceptionBuilderFactory.class);
		this.restUtilMock = createMock(RestUtil.class);

		setInternalState(resource, this.resourceBuilderFactoryMock, this.responseBuilderFactoryMock,
				this.violationExceptionBuilderFactoryMock, this.restUtilMock);
		setInternalState(resource, "conditions2QueryStringConverter", this.conditions2QueryStringConverter);
		setInternalState(resource, "errorException2ErrorConverter", this.errorException2ErrorConverter);
	}
	
	protected Conditions mockUriConditions(UriInfo uriInfoMock, Converter<UriInfo, Conditions> converter) {
		final Conditions conditions = ConditionsBuilder.create().build();
		expect(converter.convertTo(uriInfoMock)).andReturn(conditions);
		return conditions;
	}
	
	protected ResourceBuilder mockBuilders(BaseResource base, Response responseMock) {
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Resource resourceMock = createMock(Resource.class);
		
		expect(base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(responseBuilderFactoryMock.ok(resourceMock)).andReturn(responseBuilderMock);
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		
		expect(resourceBuilderMock.build()).andReturn(resourceMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		
		return resourceBuilderMock;
	}

	protected static ResourceId resourceId(final String resourceId) {
		final ResourceId clientID = new ResourceId();
		clientID.setResourceId(resourceId);
		return clientID;
	}

	protected static void assertLinks(final Links links, final String selfHref) {
		assertLink(links.getSelfLink(), selfHref);
		assertNull(links.getNextLink());
		assertNull(links.getPrevLink());
	}
}
