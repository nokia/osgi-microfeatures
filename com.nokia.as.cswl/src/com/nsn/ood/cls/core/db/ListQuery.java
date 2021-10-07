/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author marynows
 * 
 */
public abstract class ListQuery<T> extends AbstractQuery {
	private final List<T> list = new ArrayList<>();

	protected abstract T handleRow(ResultSet resultSet) throws SQLException;

	protected ListQuery(final String sql) {
		super(sql);
	}

	@Override
	public void handle(final ResultSet resultSet) throws SQLException {
		while (resultSet.next()) {
			this.list.add(handleRow(resultSet));
		}
	}

	public List<T> getList() {
		return this.list;
	}
}
