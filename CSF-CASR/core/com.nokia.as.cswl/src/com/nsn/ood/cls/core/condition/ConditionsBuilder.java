/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.condition;

import com.nsn.ood.cls.core.condition.Field.Order;
import com.nsn.ood.cls.core.condition.Filter.Type;
import com.nsn.ood.cls.util.Strings;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
public final class ConditionsBuilder {
	private final Conditions conditions;

	private Sorting sorting;

	public static ConditionsBuilder create() {
		return new ConditionsBuilder(new Conditions(false));
	}

	public static ConditionsBuilder createAndSkipMetaData() {
		return new ConditionsBuilder(new Conditions(true));
	}

	public static ConditionsBuilder use(final Conditions conditions) {
		return new ConditionsBuilder(conditions);
	}

	private ConditionsBuilder(final Conditions conditions) {
		this.conditions = conditions;
	}

	public Conditions build() {
		if (this.sorting != null) {
			this.conditions.setSorting(this.sorting);
		}
		return this.conditions;
	}

	public ConditionsBuilder equalFilter(final String name, final String value) {
		filter(Type.EQUAL, name, value);
		return this;
	}

	public ConditionsBuilder wildcardFilter(final String name, final String value) {
		filter(Type.WILDCARD, name, value);
		return this;
	}

	private void filter(final Type type, final String name, final String value) {
		if (Strings.isNullOrEmpty(name) || value == null) {
			throw new CLSIllegalArgumentException("Filter parameters must be set");
		}
		final Filter filter = new Filter(type, name, value);
		this.conditions.addFilter(filter);
	}

	public ConditionsBuilder betweenFilter(final String name, final String from, final String to) {
		if (Strings.isNullOrEmpty(name) || from == null || to == null) {
			throw new CLSIllegalArgumentException("Filter parameters must be set");
		}
		final BetweenFilter filter = new BetweenFilter(name, from, to);
		this.conditions.addFilter(filter);
		return this;
	}

	public ConditionsBuilder sort(final String name, final Order order) {
		if (Strings.isNullOrEmpty(name) || order == null) {
			throw new CLSIllegalArgumentException("Sorting parameters must be set");
		}
		if (this.sorting == null) {
			this.sorting = new Sorting();
		}
		this.sorting.addField(name, order);
		return this;
	}

	public ConditionsBuilder offset(final int offset) {
		if (offset < 0) {
			throw new CLSIllegalArgumentException("Offset must not be negative");
		}
		this.conditions.setPaginationOffset(offset);
		return this;
	}

	public ConditionsBuilder limit(final int limit) {
		if (limit < 1) {
			throw new CLSIllegalArgumentException("Limit must be positive");
		}
		this.conditions.setPaginationLimit(limit);
		return this;
	}
}
