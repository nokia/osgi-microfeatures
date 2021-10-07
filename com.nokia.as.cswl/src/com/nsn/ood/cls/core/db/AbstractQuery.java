/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;


/**
 * @author marynows
 * 
 */
public abstract class AbstractQuery implements Query {
	protected final String sql;

	public AbstractQuery(final String sql) {
		this.sql = sql;
	}

	@Override
	public String sql() {
		return this.sql;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
	}

	@Override
	public Query next() {
		return null;
	}
}