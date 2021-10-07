/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import java.util.Map;
import java.util.Map.Entry;

import com.nsn.ood.cls.util.resteasy.URLUtilsMock;


/**
 * @author marynows
 * 
 */
public final class QueryStringBuilder {
	private String query = "";

	private QueryStringBuilder() {
	}

	public QueryStringBuilder add(final String name, final String value) {
		this.query += (this.query.isEmpty() ? "?" : "&")//
				+ URLUtilsMock.encodeQueryNameOrValue(name) + "=" + URLUtilsMock.encodeQueryNameOrValue(value);
		return this;
	}

	public String build() {
		return this.query;
	}

	public static QueryStringBuilder create() {
		return new QueryStringBuilder();
	}

	public static QueryStringBuilder fromMap(final Map<String, String> map) {
		final QueryStringBuilder builder = create();
		if (map != null) {
			for (final Entry<String, String> e : map.entrySet()) {
				builder.add(e.getKey(), e.getValue());
			}
		}
		return builder;
	}
}
