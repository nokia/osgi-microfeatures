/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


/**
 * @author marynows
 * 
 */
public abstract class SimpleDistinctQuery<T> extends AbstractQuery {
	private final Class<T> columnType;
	private final List<T> values = new ArrayList<>();

	protected SimpleDistinctQuery(final String sql, final Class<T> columnType) {
		super(sql);
		this.columnType = columnType;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handle(final ResultSet resultSet) throws SQLException {
		while (resultSet.next()) {
			this.values.add((T) getValue(resultSet));
		}
	}

	private Object getValue(final ResultSet resultSet) throws SQLException {
		if (this.columnType.equals(Integer.class)) {
			return resultSet.getInt(1);
		} else if (this.columnType.equals(Long.class)) {
			return resultSet.getLong(1);
		} else if (this.columnType.equals(Timestamp.class)) {
			return resultSet.getTimestamp(1);
		} else {
			return resultSet.getString(1);
		}
	}

	public List<T> getValues() {
		return this.values;
	}
}
