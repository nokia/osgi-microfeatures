/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTime;

import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class QueryReservationsFeatureCodesForExpiredLicenses extends QueryReservationsFeatureCodes {
	private static final int END_DATE = 1;

	private final DateTime endDate;
	private final Converter<Timestamp, DateTime> timestamp2DateTimeConverter;

	public QueryReservationsFeatureCodesForExpiredLicenses(final DateTime endDate, Converter<Timestamp, DateTime> timestamp2DateTimeConverter) {
		super("enddate < ?");
		this.endDate = endDate;
		this.timestamp2DateTimeConverter = timestamp2DateTimeConverter;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		statement.setTimestamp(END_DATE, timestamp2DateTimeConverter.convertFrom(this.endDate));
	}
}
