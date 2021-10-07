/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.rest.util.ErrorBuilderFactory;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * Error exception <-> Error
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "errorException")
@Property(name = "to", value = "error")
public class ErrorException2ErrorConverter implements Converter<ErrorException, Error> {
	
	@ServiceDependency
	private ErrorBuilderFactory errorBuilderFactory;

	@Override
	public Error convertTo(final ErrorException errorException) {
		if (errorException == null) {
			throw new CLSIllegalArgumentException("Error must not be null");
		}
		return this.errorBuilderFactory.exception(errorException).build();
	}

	@Override
	public ErrorException convertFrom(final Error error) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
