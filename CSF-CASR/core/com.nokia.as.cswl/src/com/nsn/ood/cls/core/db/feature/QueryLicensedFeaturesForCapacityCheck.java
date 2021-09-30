/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.feature;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTime;

import com.nsn.ood.cls.core.db.ListQuery;
import com.nsn.ood.cls.core.db.creator.LicensedFeatureCreator;
import com.nsn.ood.cls.model.internal.LicensedFeature;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class QueryLicensedFeaturesForCapacityCheck extends ListQuery<LicensedFeature> {
	private static final int THRESHOLD = 1;
	private static final int CAPACITY_CHECK_TIME = 2;

	private final long threshold;
	private final DateTime checkTime;
	private final LicensedFeatureCreator featureCreator;
	private final Converter<Timestamp, DateTime> timestamp2DateTimeConverter;

	public QueryLicensedFeaturesForCapacityCheck(final long threshold, final DateTime checkTime,
			final LicensedFeatureCreator featureCreator, final Converter<Timestamp, DateTime> timestamp2DateTimeConverter) {
		super("select * from cls.features"
				+ " where (used * 100) >= (total * ?) and (capacitychecktime is null or capacitychecktime < ?)"
				+ " order by featurecode");
		this.threshold = threshold;
		this.checkTime = checkTime;
		this.featureCreator = featureCreator;
		this.timestamp2DateTimeConverter = timestamp2DateTimeConverter;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		statement.setLong(THRESHOLD, this.threshold);
		statement.setTimestamp(CAPACITY_CHECK_TIME, timestamp2DateTimeConverter.convertFrom(this.checkTime));
	}

	@Override
	protected LicensedFeature handleRow(final ResultSet resultSet) throws SQLException {
		return this.featureCreator.createLicensedFeature(resultSet);
	}
}
