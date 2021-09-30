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
import com.nsn.ood.cls.core.db.creator.LicenseCreatorFast;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 *
 */
public class QueryLicensesForFeatureFast extends ListQuery<License> {
	private static final int FEATURE_CODE = 1;
	private static final int END_DATE = 2;
	private static final int START_DATE = 3;
	private static final String SQL = "select serialnumber, enddate, filename, mode, total, type, used, targettype from cls.stored_licenses" //
			+ " where featurecode = ? and (enddate > ? or enddate is null) and startdate <= ?"//
			+ " order by type desc, enddate desc nulls first";

	private final LicenseCreatorFast licenseCreator;
	private final Converter<Timestamp, DateTime> timestamp2DatetimeConverter;
	private final long featureCode;

	public QueryLicensesForFeatureFast(final long featureCode, final LicenseCreatorFast licenseCreator,
			final Converter<Timestamp, DateTime> timestamp2DatetimeConverter) {
		super(SQL);
		this.featureCode = featureCode;
		this.licenseCreator = licenseCreator;
		this.timestamp2DatetimeConverter = timestamp2DatetimeConverter;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		final Timestamp now = timestamp2DatetimeConverter.convertFrom(DateTime.now());
		statement.setLong(FEATURE_CODE, this.featureCode);
		statement.setTimestamp(END_DATE, now);
		statement.setTimestamp(START_DATE, now);
	}

	@Override
	protected License handleRow(final ResultSet resultSet) throws SQLException {
		return this.licenseCreator.createLicense(resultSet);
	}
}
