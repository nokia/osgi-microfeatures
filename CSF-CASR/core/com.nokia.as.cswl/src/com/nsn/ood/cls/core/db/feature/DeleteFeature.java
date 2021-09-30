/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.feature;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.nsn.ood.cls.core.db.SimpleUpdate;


/**
 * @author marynows
 * 
 */
public class DeleteFeature extends SimpleUpdate {
	private static final int FEATURE_CODE = 1;

	private final Long featureCode;

	public DeleteFeature(final Long featureCode) {
		super("delete from cls.features where featurecode = ?"
				+ " and (select count(*) from cls.licenses where featurecode = ?) = 0");
		this.featureCode = featureCode;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		statement.setLong(FEATURE_CODE, this.featureCode);
		statement.setLong(FEATURE_CODE + 1, this.featureCode);
	}
}
