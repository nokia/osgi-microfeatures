/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.util;

import java.util.List;

import com.nsn.ood.cls.core.condition.Field;
import com.nsn.ood.cls.core.condition.Field.Order;


/**
 * @author marynows
 * 
 */
class SortingParser {
	private String sql = "";

	public SortingParser(final List<Field> fields, final ConditionsMapper mapper) {
		for (final Field field : fields) {
			if (mapper.hasColumn(field.name())) {
				final String fieldsSql = parseField(field, mapper);
				this.sql += (this.sql.isEmpty() ? " order by " : ", ") + fieldsSql;
			}
		}
	}

	public String sql() {
		return this.sql;
	}

	private String parseField(final Field field, final ConditionsMapper mapper) {
		final String columnName = mapper.getColumn(field.name()).name();
		return columnName + (field.order() == Order.DESC ? " desc" : "");
	}
}
