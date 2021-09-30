/*
 * Copyright (c) 2017 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import org.apache.felix.dm.annotation.api.Component;

/**
 * @author wro50095
 *
 */
@Component(provides = QueryFilter.class)
public class QueryFilter {
	public String filterStarCharacter(final String filterValue) {
		if (filterValue.contains("❄")) {
			return filterValue.replace("❄", ",");
		} else {
			return filterValue;
		}
	}
}
