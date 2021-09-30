/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.query;

import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.core.condition.BetweenFilter;
import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.Field;
import com.nsn.ood.cls.core.condition.Field.Order;
import com.nsn.ood.cls.core.condition.Filter;
import com.nsn.ood.cls.core.condition.Filter.Type;
import com.nsn.ood.cls.core.condition.Pagination;
import com.nsn.ood.cls.rest.util.QueryStringBuilder;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * Conditions <-> Query string (RFC 3986)
 *
 * @author marynows
 *
 */
@Loggable
@Component
@Property(name = "from", value = "conditions")
@Property(name = "to", value = "queryString")
public class Conditions2QueryStringConverter implements Converter<Conditions, String> {

	@Override
	public String convertTo(final Conditions conditions) {
		if (conditions == null) {
			throw new CLSIllegalArgumentException("Conditions must not be null");
		}

		final QueryStringBuilder builder = QueryStringBuilder.create();
		if (conditions.hasFilters()) {
			convertFilters(conditions.filters(), builder);
		}
		if (conditions.sorting().hasFields()) {
			convertSorting(conditions.sorting().fields(), builder);
		}
		if (conditions.pagination().isLimited()) {
			convertPagination(conditions.pagination(), builder);
		}
		return builder.build();
	}

	private void convertFilters(final List<Filter> filters, final QueryStringBuilder builder) {
		for (final Filter filter : filters) {
			final String value;
			if (filter.type() == Type.BETWEEN) {
				final BetweenFilter betweenFilter = (BetweenFilter) filter;
				value = betweenFilter.from() + QueryConst.BETWEEN_FILTER_SEPARATOR + betweenFilter.to();
			} else {
				value = filter.value();
			}
			builder.add(filter.name(), value);
		}
	}

	private void convertSorting(final List<Field> fields, final QueryStringBuilder builder) {
		final StringBuilder sb = new StringBuilder();
		for (final Field field : fields) {
			if (sb.length() > 0) {
				sb.append(QueryConst.SORTING_SEPARATOR);
			}
			if (field.order() == Order.DESC) {
				sb.append(QueryConst.SORTING_DESC_ORDER);
			}
			sb.append(field.name());
		}
		builder.add(QueryConst.SORTING_PARAMETER, sb.toString());
	}

	private void convertPagination(final Pagination pagination, final QueryStringBuilder builder) {
		if (pagination.offset() != Pagination.DEFAULT_OFFSET) {
			builder.add(QueryConst.OFFSET_PARAMETER, String.valueOf(pagination.offset()));
		}
		if (pagination.limit() != Pagination.DEFAULT_LIMIT) {
			builder.add(QueryConst.LIMIT_PARAMETER, String.valueOf(pagination.limit()));
		}
	}

	@Override
	public Conditions convertFrom(final String query) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
