/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.joda.time.DateTime;

import com.nsn.ood.cls.core.db.IterableUpdate;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
class InsertReservations extends IterableUpdate<Reservation> {
	private static final int CLIENT_ID = 1;
	private static final int FEATURE_CODE = 2;
	private static final int SERIAL_NUMBER = 3;
	private static final int CAPACITY = 4;
	private static final int RESERVATION_TIME = 5;
	private static final int MODE = 6;
	private static final int TYPE = 7;
	private static final int END_DATE = 8;
	private static final int FILE_NAME = 9;

	private final Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	private final Converter<License.Mode, Integer> licenseMode2IntegerConverter;
	private final Converter<License.Type, Integer> licenseType2IntegerConverter;

	public InsertReservations(final List<Reservation> reservations, final Converter<Timestamp, DateTime> timestamp2DateTimeConverter, 
			final Converter<License.Mode, Integer> licenseMode2IntegerConverter,
			final Converter<License.Type, Integer> licenseType2IntegerConverter) {
		super("insert into cls.reservations"
				+ " (clientid, featurecode, serialnumber, capacity, reservationtime, mode, type, enddate, filename)"
				+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?)", reservations);
		this.timestamp2DateTimeConverter = timestamp2DateTimeConverter;
		this.licenseMode2IntegerConverter = licenseMode2IntegerConverter;
		this.licenseType2IntegerConverter = licenseType2IntegerConverter;
	}

	@Override
	protected void prepareRow(final PreparedStatement statement, final Reservation reservation) throws SQLException {
		statement.setString(CLIENT_ID, reservation.getClientId());
		statement.setLong(FEATURE_CODE, reservation.getFeatureCode());
		statement.setString(SERIAL_NUMBER, reservation.getSerialNumber());
		statement.setLong(CAPACITY, reservation.getCapacity());
		statement.setTimestamp(RESERVATION_TIME, timestamp2DateTimeConverter.convertFrom(reservation.getReservationTime()));
		statement.setInt(MODE, licenseMode2IntegerConverter.convertTo(reservation.getMode()));
		statement.setInt(TYPE, licenseType2IntegerConverter.convertTo(reservation.getType()));
		statement.setTimestamp(END_DATE, timestamp2DateTimeConverter.convertFrom(reservation.getEndDate()));
		statement.setString(FILE_NAME, reservation.getFileName());
	}
}
