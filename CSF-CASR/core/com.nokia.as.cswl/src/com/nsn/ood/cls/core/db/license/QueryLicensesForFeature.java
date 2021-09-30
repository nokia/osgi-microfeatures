/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.license;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTime;

import com.nsn.ood.cls.core.db.ListQuery;
import com.nsn.ood.cls.core.db.creator.LicenseCreator;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 *
 */
public class QueryLicensesForFeature extends ListQuery<License> {
	private static final int FEATURE_CODE = 1;
	private static final int MODE = 2;
	private static final int END_DATE = 3;
	private static final int START_DATE = 4;
	private static final int TARGET_TYPE = 5;
	private static final String SQL = "select * from cls.stored_licenses" //
			+ " where featurecode = ? and mode = ? and (enddate > ? or enddate is null) and startdate <= ?"//
			+ " and case when ? is null then 1=1 else targettype = ? end"//
			+ " order by type desc, enddate IS NULL DESC, enddate desc";

	private final Client client;
	private final Feature feature;
	private final LicenseCreator licenseCreator;
	private final Converter<Timestamp, DateTime> timestamp2DatetimeConverter;
	private final Converter<Feature.Type, Integer> featureType2IntegerConverter;

	public QueryLicensesForFeature(final Client client, final Feature feature, final LicenseCreator licenseCreator,
			final Converter<Timestamp, DateTime> timestamp2DatetimeConverter, final Converter<Feature.Type, Integer> featureType2IntegerConverter) {
		super(SQL);
		this.client = client;
		this.feature = feature;
		this.licenseCreator = licenseCreator;
		this.timestamp2DatetimeConverter = timestamp2DatetimeConverter;
		this.featureType2IntegerConverter = featureType2IntegerConverter;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		final Timestamp now = timestamp2DatetimeConverter.convertFrom(DateTime.now());
		statement.setLong(FEATURE_CODE, this.feature.getFeatureCode());
		statement.setInt(MODE, featureType2IntegerConverter.convertTo(this.feature.getType()));
		statement.setTimestamp(END_DATE, now);
		statement.setTimestamp(START_DATE, now);
		statement.setString(TARGET_TYPE, this.client.getTargetType());
		statement.setString(TARGET_TYPE + 1, this.client.getTargetType());
	}

	@Override
	protected License handleRow(final ResultSet resultSet) throws SQLException {
		return this.licenseCreator.createLicense(resultSet);
	}
}

