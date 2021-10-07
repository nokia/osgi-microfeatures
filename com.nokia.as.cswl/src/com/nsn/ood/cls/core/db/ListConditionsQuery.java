/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;


/**
 * @author marynows
 * 
 */
public abstract class ListConditionsQuery<T> extends ConditionsQuery<T> {
	private final List<T> list = new ArrayList<>();

	protected abstract T handleRow(ResultSet resultSet) throws SQLException;

	protected ListConditionsQuery(final String sql, final Conditions conditions, final ConditionsMapper mapper,
			final String idFilterName) throws ConditionProcessingException {
		super(sql, conditions, mapper, idFilterName);
	}

	@Override
	public void handle(final ResultSet resultSet) throws SQLException {
		while (resultSet.next()) {
			this.list.add(handleRow(resultSet));
		}
	}

	@Override
	public List<T> getList() {
		return this.list;
	}
}
