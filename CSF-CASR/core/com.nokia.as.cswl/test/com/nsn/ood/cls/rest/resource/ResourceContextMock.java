/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource;

import javax.ws.rs.container.ResourceContext;


/**
 * @author marynows
 * 
 */
public class ResourceContextMock implements ResourceContext {

	@Override
	public <T> T getResource(final Class<T> resourceClass) {
		try {
			return resourceClass.newInstance();
		} catch (final ReflectiveOperationException e) {
			return null;
		}
	}

	@Override
	public <T> T initResource(final T resource) {
		return resource;
	}
}
