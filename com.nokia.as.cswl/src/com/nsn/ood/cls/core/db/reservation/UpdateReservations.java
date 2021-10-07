/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import java.sql.Timestamp;
import java.util.List;

import org.joda.time.DateTime;

import com.nsn.ood.cls.core.db.Update;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.util.CollectionUtils;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class UpdateReservations extends DeleteReservationsByFeatureCode {
	private final List<Reservation> reservations;
	private final Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	private final Converter<License.Mode, Integer> licenseMode2IntegerConverter;
	private final Converter<License.Type, Integer> licenseType2IntegerConverter;

	public UpdateReservations(final Client client, final long featureCode, final List<Reservation> reservations,
			final Converter<Timestamp, DateTime> timestamp2DateTimeConverter, final Converter<License.Mode, Integer> licenseMode2IntegerConverter, final Converter<License.Type, Integer> licenseType2IntegerConverter) {
		super(client, featureCode);
		this.reservations = reservations;
		this.timestamp2DateTimeConverter = timestamp2DateTimeConverter;
		this.licenseMode2IntegerConverter = licenseMode2IntegerConverter;
		this.licenseType2IntegerConverter = licenseType2IntegerConverter;
	}

	@Override
	public Update next() {
		if (CollectionUtils.isEmpty(this.reservations)) {
			return null;
		} else {
			return new InsertReservations(this.reservations, timestamp2DateTimeConverter, licenseMode2IntegerConverter, licenseType2IntegerConverter);
		}
	}
}
