/*
 * Copyright (c) 2017 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.activity;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.nsn.ood.cls.core.db.SimpleUpdate;
import com.nsn.ood.cls.core.db.Update;


/**
 * @author wro50095
 *
 */
public class SetLastInsertIDVariable extends SimpleUpdate {
	private final InsertActivityDetails activityDetailsQuery;

	/**
	 * @param activityDetailsQuery
	 * @param sql
	 */
	protected SetLastInsertIDVariable(final InsertActivityDetails activityDetailsQuery) {
		super("select @actId:= LAST_INSERT_ID()");
		this.activityDetailsQuery = activityDetailsQuery;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
	}

	@Override
	public Update next() {
		return this.activityDetailsQuery;
	}

}
