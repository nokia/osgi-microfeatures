/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.creator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;

import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
@Component(provides = ReservationCreator.class)
public class ReservationCreator {
	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	
	@ServiceDependency(filter = "(&(from=licenseMode)(to=integer))")
	private Converter<License.Mode, Integer> licenseMode2IntegerConverter;
	
	@ServiceDependency(filter = "(&(from=licenseType)(to=integer))")
	private Converter<License.Type, Integer> licenseType2IntegerConverter;

	public Reservation createReservation(final ResultSet resultSet) throws SQLException {
		return new Reservation()//
				.withClientId(resultSet.getString("clientid"))//
				.withFeatureCode(resultSet.getLong("featurecode"))//
				.withSerialNumber(resultSet.getString("serialnumber"))//
				.withCapacity(resultSet.getLong("capacity"))//
				.withReservationTime(timestamp2DateTimeConverter.convertTo(resultSet.getTimestamp("reservationtime")))//
				.withMode(licenseMode2IntegerConverter.convertFrom(resultSet.getInt("mode")))//
				.withType(licenseType2IntegerConverter.convertFrom(resultSet.getInt("type")))//
				.withEndDate(timestamp2DateTimeConverter.convertTo(resultSet.getTimestamp("enddate")))//
				.withFileName(resultSet.getString("filename"));
	}
}
