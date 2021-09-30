/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.query;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.rest.util.QueryFilter;
import com.nsn.ood.cls.util.Strings;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 *
 */
@Component(provides = FilterParser.class)
public class FilterParser {
	private static Pattern BETWEEN_PATTERN = Pattern.compile("(.*?)" + QueryConst.BETWEEN_FILTER_SEPARATOR + "(.*)");

	@ServiceDependency
	private QueryFilter queryFilter;

	public void parse(final ConditionsBuilder builder, final String name, final String value) {
		if (Strings.isNullOrEmpty(name) || (value == null)) {
			throw new CLSIllegalArgumentException("Parameters must not be null");
		}

		final Matcher betweenMatcher = BETWEEN_PATTERN.matcher(value);
		if (betweenMatcher.matches()) {
			addBetweenFilter(builder, name, this.queryFilter.filterStarCharacter(betweenMatcher.group(1)),
					this.queryFilter.filterStarCharacter(betweenMatcher.group(2)));
			return;
		}

		final int pos = value.indexOf(QueryConst.WILDCARD_CHARACTER);
		if (pos >= 0) {
			addWildcardFilter(builder, name, this.queryFilter.filterStarCharacter(value));
			return;
		}

		addEqualFilter(builder, name, this.queryFilter.filterStarCharacter(value));
	}

	private void addEqualFilter(final ConditionsBuilder builder, final String name, final String value) {
		builder.equalFilter(name, value);
	}

	private void addWildcardFilter(final ConditionsBuilder builder, final String name, final String value) {
		builder.wildcardFilter(name, value);
	}

	private void addBetweenFilter(final ConditionsBuilder builder, final String name, final String from,
			final String to) {
		builder.betweenFilter(name, from, to);
	}
}
