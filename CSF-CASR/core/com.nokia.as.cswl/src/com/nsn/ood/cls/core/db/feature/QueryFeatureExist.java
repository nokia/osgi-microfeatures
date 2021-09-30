/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.feature;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.nsn.ood.cls.core.db.SimpleQuery;


/**
 * @author marynows
 * 
 */
public class QueryFeatureExist extends SimpleQuery<Boolean> {
	private final Long featureCode;

	public QueryFeatureExist(final Long featureCode) {
		super("select 1 from cls.features where featurecode = ?", false);
		this.featureCode = featureCode;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		statement.setLong(1, this.featureCode);
	}

	@Override
	protected Boolean handleValue(final ResultSet resultSet) throws SQLException {
		return resultSet.getBoolean(1);
	}
}
