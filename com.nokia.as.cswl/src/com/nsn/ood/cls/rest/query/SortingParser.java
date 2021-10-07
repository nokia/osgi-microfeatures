/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.query;

import org.apache.felix.dm.annotation.api.Component;

import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.condition.Field.Order;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
@Component(provides = SortingParser.class)
public class SortingParser {

	public void parse(final ConditionsBuilder builder, final String value) {
		if (value == null) {
			throw new CLSIllegalArgumentException("Parameters must not be null");
		}

		for (String field : value.split(QueryConst.SORTING_SEPARATOR)) {
			field = field.trim();
			if (!field.isEmpty()) {
				addField(builder, field);
			}
		}
	}

	private void addField(final ConditionsBuilder builder, final String field) {
		if (field.startsWith(QueryConst.SORTING_DESC_ORDER)) {
			if (field.length() > 1) {
				builder.sort(field.substring(1), Order.DESC);
			}
		} else {
			builder.sort(field, Order.ASC);
		}
	}
}
