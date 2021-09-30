/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.nsn.ood.cls.util.CollectionUtils;


/**
 * @author marynows
 * 
 */
public abstract class InUpdate<T> implements Update {
	private final String sqlZero;
	private final String sqlOne;
	private final String sqlMore;
	private final List<T> values;

	protected abstract void prepareValue(final PreparedStatement statement, int index, T value) throws SQLException;

	protected InUpdate(final String sqlZero, final String sqlOne, final String sqlMore, final List<T> values) {
		this.sqlZero = sqlZero;
		this.sqlOne = sqlOne;
		this.sqlMore = sqlMore;
		this.values = values;
	}

	@Override
	public String sql() {
		if (CollectionUtils.isEmpty(this.values)) {
			return this.sqlZero;
		} else if (this.values.size() == 1) {
			return this.sqlOne;
		} else {
			return String.format(this.sqlMore, StringUtils.repeat("?", ",", this.values.size()));
		}
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		if (!CollectionUtils.isEmpty(this.values)) {
			for (int i = 0; i < this.values.size(); i++) {
				prepareValue(statement, i + 1, this.values.get(i));
			}
		}
	}

	@Override
	public void handle(final int affectedRows) throws SQLException {
	}

	@Override
	public Update next() {
		return null;
	}
}
