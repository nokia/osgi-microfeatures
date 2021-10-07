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
public class UpdateLicensesStateToExpired extends SimpleUpdate {
	private static final int END_DATE = 1;
	private static final String SQL = "update cls.licenses set state = '" + LicenseState.EXPIRED.toString() + "'"//
			+ " where state = '" + LicenseState.ACTIVE.toString() + "' and enddate < ?";

	private final DateTime date;
	private final Converter<Timestamp, DateTime> timestamp2DatetimeConverter;

	public UpdateLicensesStateToExpired(final DateTime date, final Converter<Timestamp, DateTime> timestamp2DatetimeConverter) {
		super(SQL);
		this.date = date;
		this.timestamp2DatetimeConverter = timestamp2DatetimeConverter;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		statement.setTimestamp(END_DATE, timestamp2DatetimeConverter.convertFrom(this.date));
	}
}
