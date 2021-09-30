/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.util;

import java.util.ArrayList;
import java.util.List;

import com.nsn.ood.cls.core.condition.BetweenFilter;
import com.nsn.ood.cls.core.condition.Filter;
import com.nsn.ood.cls.core.db.util.ConditionsMapper.Column;
import com.nsn.ood.cls.core.db.util.ConditionsMapper.RangePolicy;


/**
 * @author marynows
 * 
 */
class FiltersParser {
	private final List<Object> values = new ArrayList<>();
	private final String sql;

	public FiltersParser(final List<Filter> filters, final ConditionsMapper mapper) throws ConditionProcessingException {
		final StringBuilder builder = new StringBuilder();
		for (final Filter filter : filters) {
			if (mapper.hasColumn(filter.name())) {
				final String filterSql = parseFilter(filter, mapper);
				if (!filterSql.isEmpty()) {
					builder.append(builder.length() == 0 ? " where " : " and ").append(filterSql);
				}
			}
		}
		this.sql = builder.toString();
	}

	public String sql() {
		return this.sql;
	}

	public List<Object> values() {
		return this.values;
	}

	private String parseFilter(final Filter filter, final ConditionsMapper mapper) throws ConditionProcessingException {
		final Column column = mapper.getColumn(filter.name());
		switch (filter.type()) {
			case BETWEEN: {
				return processBetweenFilter((BetweenFilter) filter, column);
			}
			case WILDCARD: {
				return processWildcardFilter(filter, column);
			}
			default: {
				return processDefaultFilter(filter, column);
			}
		}
	}

	private String processDefaultFilter(final Filter filter, final Column column) throws ConditionProcessingException {
		if (filter.value().isEmpty()) {
			return processValues(column, "# is null");
		} else {
			return processValues(column, "# = ?", filter.value());
		}
	}

	private String processWildcardFilter(final Filter filter, final Column column) throws ConditionProcessingException {
		if (filter.value().isEmpty()) {
			return processValues(column, "# is null");
		} else {
			return processValues(column, "lower(#) like lower(?)", filter.value().replaceAll("\\*", "%"));
		}
	}

	private String processBetweenFilter(final BetweenFilter filter, final Column column)
			throws ConditionProcessingException {
		if (filter.from().isEmpty() && filter.to().isEmpty()) { // ","
			if (column.rangePolicy() != RangePolicy.IGNORE_NULLS) {
				return processValues(column, "# is null");
			} else {
				return "";
			}
		} else if (filter.from().isEmpty()) { // ",to"
			if (column.rangePolicy() == RangePolicy.NULLS_FIRST) {
				return processValues(column, "(# < ? or # is null)", filter.to());
			} else {
				return processValues(column, "# < ?", filter.to());
			}
		} else if (filter.to().isEmpty()) { // "from,"
			if (column.rangePolicy() == RangePolicy.NULLS_LAST) {
				return processValues(column, "(# >= ? or # is null)", filter.from());
			} else {
				return processValues(column, "# >= ?", filter.from());
			}
		} else { // "from,to"
			return processValues(column, "# >= ? and # < ?", filter.from(), filter.to());
		}
	}

	private String processValues(final Column column, final String sql, final String... values)
			throws ConditionProcessingException {
		for (final String value : values) {
			this.values.add(tryParseValue(column, value));
		}
		return sql.replaceAll("#", column.name());
	}

	private Object tryParseValue(final Column column, final String value) throws ConditionProcessingException {
		try {
			return column.prepareParse(value);
		} catch (final IllegalArgumentException e) {
			throw new ConditionProcessingException("Invalid filter value", column.field(), value, e);
		}
	}
}
