/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.feature;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.joda.time.DateTime;

import com.nsn.ood.cls.core.db.IterableUpdate;
import com.nsn.ood.cls.model.internal.LicensedFeature;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class UpdateLicensedFeaturesForCapacityCheck extends IterableUpdate<LicensedFeature> {
	private static final int CAPACITY_CHECK_TIME = 1;
	private static final int FEATURE_CODE = 2;

	private final DateTime checkTime;
	private final Converter<Timestamp, DateTime> timestamp2DateTimeConverter;

	public UpdateLicensedFeaturesForCapacityCheck(final List<LicensedFeature> features, final DateTime checkTime,
			final Converter<Timestamp, DateTime> timestamp2DateTimeConverter) {
		// TODO: change it to "featurecode in (...)"
		super("update cls.features set capacitychecktime = ? where featurecode = ?", features);
		this.checkTime = checkTime;
		this.timestamp2DateTimeConverter = timestamp2DateTimeConverter;
	}

	@Override
	protected void prepareRow(final PreparedStatement statement, final LicensedFeature feature) throws SQLException {
		statement.setTimestamp(CAPACITY_CHECK_TIME, timestamp2DateTimeConverter.convertFrom(this.checkTime));
		statement.setLong(FEATURE_CODE, feature.getFeatureCode());
	}
}
