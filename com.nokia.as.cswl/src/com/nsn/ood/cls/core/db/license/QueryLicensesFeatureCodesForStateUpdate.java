/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.license;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTime;

import com.nsn.ood.cls.core.db.SimpleDistinctQuery;
import com.nsn.ood.cls.core.model.LicenseState;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class QueryLicensesFeatureCodesForStateUpdate extends SimpleDistinctQuery<Long> {
	private static final int START_DATE = 1;
	private static final int END_DATE = 2;
	private static final String SQL = "select distinct featurecode from cls.licenses"//
			+ " where (state = '" + LicenseState.INACTIVE.toString() + "' and startdate <= ?)"//
			+ " or (state = '" + LicenseState.ACTIVE.toString() + "' and enddate < ?)";

	private final DateTime date;
	private final Converter<Timestamp, DateTime> timestamp2DatetimeConverter;

	public QueryLicensesFeatureCodesForStateUpdate(final DateTime date, final Converter<Timestamp, DateTime> timestamp2DatetimeConverter) {
		super(SQL, Long.class);
		this.date = date;
		this.timestamp2DatetimeConverter = timestamp2DatetimeConverter;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		final Timestamp timestamp = timestamp2DatetimeConverter.convertFrom(this.date);
		statement.setTimestamp(START_DATE, timestamp);
		statement.setTimestamp(END_DATE, timestamp);
	}
}
