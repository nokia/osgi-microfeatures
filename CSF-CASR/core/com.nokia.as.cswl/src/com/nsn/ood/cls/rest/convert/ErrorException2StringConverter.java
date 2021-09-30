/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.model.gen.errors.FeatureError;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.DescriptionBuilder;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * Error exception <-> String
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "errorException")
@Property(name = "to", value = "string")
public class ErrorException2StringConverter implements Converter<ErrorException, String> {

	@Override
	public String convertTo(final ErrorException errorException) {
		if (errorException == null) {
			throw new CLSIllegalArgumentException("Error must not be null");
		}

		final DescriptionBuilder db = new DescriptionBuilder()//
				.append("errorCode", errorException.getErrorCode().getCode());
		if (errorException.getMessage() != null) {
			db.append("message", errorException.getMessage().replaceAll("\n", ";"));
		}
		if (errorException.getValue() != null) {
			if (errorException.getValue() instanceof FeatureError) {
				final FeatureError featureError = (FeatureError) errorException.getValue();
				db.append("featureCode", featureError.getFeatureCode())//
						.append("capacity", featureError.getCapacity())//
						.append("requestedCapacity", featureError.getRequestedCapacity())//
						.append("remainingCapacity", featureError.getRemainingCapacity());
			}
			if (errorException.getValue() instanceof License) {
				final License license = (License) errorException.getValue();
				db.append("fileName", license.getFileName());
			}
			// Client, ViolationError -> nothing
		}
		return db.build();
	}

	@Override
	public ErrorException convertFrom(final String string) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
