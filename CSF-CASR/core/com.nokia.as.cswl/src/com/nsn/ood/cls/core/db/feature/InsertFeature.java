/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.feature;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.nsn.ood.cls.core.db.SimpleUpdate;
import com.nsn.ood.cls.model.gen.licenses.Feature;
import com.nsn.ood.cls.model.gen.licenses.License;


/**
 * @author marynows
 * 
 */
public class InsertFeature extends SimpleUpdate {
	private static final int FEATURE_CODE = 1;
	private static final int FEATURE_NAME = 2;
	private static final int CAPACITY_UNIT = 3;
	private static final int TARGET_TYPE = 4;

	private final License license;

	public InsertFeature(final License license) {
		super("insert into cls.features (featurecode, featurename, capacityunit, targettype, total, used, remaining)"
				+ " values (?, ?, ?, ?, 0, 0, 0)");
		this.license = license;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		final Feature feature = this.license.getFeatures().get(0);
		statement.setLong(FEATURE_CODE, feature.getFeatureCode());
		statement.setString(FEATURE_NAME, feature.getFeatureName());
		statement.setString(CAPACITY_UNIT, this.license.getCapacityUnit());
		statement.setString(TARGET_TYPE, this.license.getTargetType());
	}
}
