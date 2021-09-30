/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.activity;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.nsn.ood.cls.core.db.SimpleQuery;


/**
 * @author marynows
 * 
 */
public class QueryNextActivityId extends SimpleQuery<Long> {

	public QueryNextActivityId() {
		super("select nextval('cls.activities_id_seq') as id", 0L);
	}

	@Override
	protected Long handleValue(final ResultSet resultSet) throws SQLException {
		return resultSet.getLong("id");
	}
}
