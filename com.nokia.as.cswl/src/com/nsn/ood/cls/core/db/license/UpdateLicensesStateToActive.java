/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.license;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTime;

import com.nsn.ood.cls.core.db.SimpleUpdate;
import com.nsn.ood.cls.core.model.LicenseState;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class UpdateLicensesStateToActive extends SimpleUpdate {
	private static final int START_DATE = 1;
	private static final String SQL = "update cls.licenses set state = '" + LicenseState.ACTIVE.toString() + "'"//
			+ " where state = '" + LicenseState.INACTIVE.toString() + "' and startdate <= ?";

	private final DateTime date;
	private final Converter<Timestamp, DateTime> timestamp2DatetimeConverter;

	public UpdateLicensesStateToActive(final DateTime date, final Converter<Timestamp, DateTime> timestamp2DatetimeConverter) {
		super(SQL);
		this.date = date;
		this.timestamp2DatetimeConverter = timestamp2DatetimeConverter;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		statement.setTimestamp(START_DATE, timestamp2DatetimeConverter.convertFrom(this.date));
	}
}
