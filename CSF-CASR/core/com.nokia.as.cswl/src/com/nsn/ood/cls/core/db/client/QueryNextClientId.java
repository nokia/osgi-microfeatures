/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.client;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.nsn.ood.cls.core.db.SimpleQuery;


/**
 * @author marynows
 *
 */
public class QueryNextClientId extends SimpleQuery<Long> {

	public QueryNextClientId() {
		// super("select nextval('cls.client_id_seq') as id", 0L);
		super("select UUID_SHORT() as id", 0L);
	}

	@Override
	protected Long handleValue(final ResultSet resultSet) throws SQLException {
		return resultSet.getLong("id");
	}
}
