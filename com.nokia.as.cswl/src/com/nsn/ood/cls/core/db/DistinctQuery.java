/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.nsn.ood.cls.util.Strings;


/**
 * @author marynows
 * 
 */
public class DistinctQuery implements Query {
	private final String sql;
	private final QueryPrepare queryPrepare;
	private final String columnName;
	private final Class<?> columnType;
	private final List<Object> values = new ArrayList<>();

	public DistinctQuery(final String sql, final QueryPrepare queryPrepare, final String columnName,
			final Class<?> columnType) {
		this.sql = sql;
		this.queryPrepare = queryPrepare;
		this.columnName = columnName;
		this.columnType = columnType;
	}

	@Override
	public String sql() {
		if (Strings.isNullOrEmpty(this.sql) || Strings.isNullOrEmpty(this.columnName)) {
			return "select null where 1=0";
		} else {
			return "select distinct " + this.columnName + " from (" + this.sql + ") t order by 1";
		}
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		if (this.queryPrepare != null) {
			this.queryPrepare.prepare(statement);
		}
	}

	@Override
	public Query next() {
		return null;
	}

	@Override
	public void handle(final ResultSet resultSet) throws SQLException {
		while (resultSet.next()) {
			final Object value = getValue(resultSet);
			if (!resultSet.wasNull()) {
				this.values.add(value);
			}
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

	public List<Object> getValues() {
		return this.values;
	}

	public interface QueryPrepare {
		void prepare(final PreparedStatement statement) throws SQLException;
	}
}
