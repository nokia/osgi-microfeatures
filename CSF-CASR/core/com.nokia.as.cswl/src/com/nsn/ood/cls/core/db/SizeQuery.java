/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.nsn.ood.cls.util.Strings;


/**
 * @author marynows
 * 
 */
public class SizeQuery implements Query {
	private final String sql;
	private final QueryPrepare queryPrepare;
	private final Query next;

	private long size = 0;

	public SizeQuery(final String sql, final QueryPrepare queryPrepare, final Query next) {
		this.sql = sql;
		this.queryPrepare = queryPrepare;
		this.next = next;
	}

	@Override
	public String sql() {
		if (Strings.isNullOrEmpty(this.sql)) {
			return "select 0 as count";
		} else {
			return "select count(*) as count from (" + this.sql + ") t";
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
		return this.next;
	}

	@Override
	public void handle(final ResultSet resultSet) throws SQLException {
		if (resultSet.next()) {
			this.size = resultSet.getLong("count");
		}
	}

	public long getSize() {
		return this.size;
	}

	public interface QueryPrepare {
		void prepare(final PreparedStatement statement) throws SQLException;
	}
}
