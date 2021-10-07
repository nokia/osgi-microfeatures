/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import java.sql.SQLException;


/**
 * @author marynows
 * 
 */
public abstract class SimpleUpdate implements Update {
	private final String sql;

	protected SimpleUpdate(final String sql) {
		this.sql = sql;
	}

	@Override
	public String sql() {
		return this.sql;
	}

	@Override
	public Update next() {
		return null;
	}

	@Override
	public void handle(final int affectedRows) throws SQLException {
	}
}
