/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;


/**
 * @author marynows
 * 
 */
public abstract class IterableUpdate<T> extends SimpleUpdate {
	private final List<T> values;

	private int index = 0;
	private int lastIndex = 0;

	protected abstract void prepareRow(final PreparedStatement statement, T value) throws SQLException;

	protected IterableUpdate(final String sql, final List<T> values) {
		super(sql);
		this.values = values;
	}

	@Override
	public Update next() {
		if (this.index < this.values.size()) {
			return this;
		}
		return null;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		this.lastIndex = this.index;
		final T value = this.values.get(this.index);
		prepareRow(statement, value);
		this.index++;
	}

	public int getIndex() {
		return this.lastIndex;
	}
}
