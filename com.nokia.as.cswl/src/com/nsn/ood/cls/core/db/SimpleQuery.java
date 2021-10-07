/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * @author marynows
 * 
 */
public abstract class SimpleQuery<T> extends AbstractQuery {
	private T value;

	protected abstract T handleValue(ResultSet resultSet) throws SQLException;

	protected SimpleQuery(final String sql, final T deafultValue) {
		super(sql);
		this.value = deafultValue;
	}

	@Override
	public void handle(final ResultSet resultSet) throws SQLException {
		if (resultSet.next()) {
			this.value = handleValue(resultSet);
		}
	}

	public T getValue() {
		return this.value;
	}
}
