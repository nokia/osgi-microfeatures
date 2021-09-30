/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.Field;
import com.nsn.ood.cls.core.condition.Filter;
import com.nsn.ood.cls.core.condition.Pagination;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
public class ConditionsProcessor {
	private final String sql;
	private final List<Object> values;

	public ConditionsProcessor(final Conditions conditions, final ConditionsMapper mapper)
			throws ConditionProcessingException {
		if (mapper == null) {
			throw new CLSIllegalArgumentException("Conditions mapper must not be null");
		}

		String sql = "";
		List<Object> values = Collections.emptyList();

		if (conditions != null) {
			if (conditions.hasFilters()) {
				final FiltersParser parser = createFiltersParser(conditions.filters(), mapper);
				sql += parser.sql();
				values = parser.values();
			}

			if (conditions.sorting().hasFields()) {
				sql += createSortingParser(conditions.sorting().fields(), mapper).sql();
			}

			if (conditions.pagination().isLimited()) {
				sql += createPaginationParser(conditions.pagination()).sql();
			}
		}

		this.sql = sql;
		this.values = values;
	}

	public String sql() {
		return this.sql;
	}

	public void prepare(final PreparedStatement statement) throws SQLException {
		for (int index = 0; index < this.values.size(); index++) {
			setPreparedValue(statement, index + 1, this.values.get(index));
		}
	}

	protected FiltersParser createFiltersParser(final List<Filter> filters, final ConditionsMapper mapper)
			throws ConditionProcessingException {
		return new FiltersParser(filters, mapper);
	}

	protected SortingParser createSortingParser(final List<Field> fields, final ConditionsMapper mapper) {
		return new SortingParser(fields, mapper);
	}

	protected PaginationParser createPaginationParser(final Pagination pagination) {
		return new PaginationParser(pagination);
	}

	private void setPreparedValue(final PreparedStatement statement, final int index, final Object value)
			throws SQLException {
		if (value instanceof Integer) {
			statement.setInt(index, (Integer) value);
		} else if (value instanceof Long) {
			statement.setLong(index, (Long) value);
		} else if (value instanceof Timestamp) {
			statement.setTimestamp(index, (Timestamp) value);
		} else {
			statement.setString(index, (String) value);
		}
	}
}
