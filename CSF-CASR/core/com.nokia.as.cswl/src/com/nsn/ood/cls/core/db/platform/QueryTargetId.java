/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.platform;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.nsn.ood.cls.core.db.SimpleQuery;


/**
 * @author marynows
 * 
 */
public class QueryTargetId extends SimpleQuery<String> {

	public QueryTargetId() {
		super("select target_id from ood_system.system_info", null);
	}

	@Override
	protected String handleValue(final ResultSet resultSet) throws SQLException {
		return resultSet.getString("target_id");
	}
}
