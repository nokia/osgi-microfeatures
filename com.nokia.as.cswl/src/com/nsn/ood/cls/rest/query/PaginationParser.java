/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.query;

import org.apache.felix.dm.annotation.api.Component;

import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
@Component(provides = PaginationParser.class)
public class PaginationParser {

	public void parseOffset(final ConditionsBuilder builder, final String value) {
		if (value == null) {
			throw new CLSIllegalArgumentException("Parameters must not be null");
		}
		builder.offset(Math.max(tryParse(value, 0), 0));
	}

	public void parseLimit(final ConditionsBuilder builder, final String value) {
		if (value == null) {
			throw new CLSIllegalArgumentException("Parameters must not be null");
		}
		final int limit = tryParse(value, Integer.MAX_VALUE);
		builder.limit(limit < 1 ? Integer.MAX_VALUE : limit);
	}

	private int tryParse(final String value, final int deafult) {
		try {
			return Integer.parseInt(value.trim());
		} catch (final NumberFormatException e) {
			return deafult;
		}
	}
}
