/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import org.apache.felix.dm.annotation.api.Component;

import com.nsn.ood.cls.rest.exception.ViolationException;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = ViolationExceptionBuilderFactory.class)
@Loggable
public class ViolationExceptionBuilderFactory {

	public ViolationException exception(final String message) {
		return createBuilder(message).build();
	}

	public ViolationException valueException(final String message, final String value) {
		return createBuilder(message).value(value).build();
	}

	public ViolationException pathException(final String message, final String path) {
		return createBuilder(message).path(path, null, null).build();
	}

	public ViolationException featureException(final String message, final Integer index, final String field,
			final Object value) {
		return createBuilder(message).path("features", index, field).value(value).build();
	}

	public ViolationException clientException(final String message, final String field, final Object value) {
		return createBuilder(message).path("clients", 0, field).value(value).build();
	}

	public ViolationException settingValueException(final String message, final Integer index, final Object value) {
		return createBuilder(message).path(null, index, "value").value(value).build();
	}

	public ViolationException settingKeyException(final String message, final Object value) {
		return createBuilder(message).path("key", null, null).value(value).build();
	}

	protected ViolationExceptionBuilder createBuilder(final String message) {
		return new ViolationExceptionBuilder(message);
	}
}
