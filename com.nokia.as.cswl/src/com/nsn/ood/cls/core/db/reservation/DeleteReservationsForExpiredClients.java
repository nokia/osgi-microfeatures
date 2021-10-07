/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTime;

import com.nsn.ood.cls.core.db.SimpleUpdate;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class DeleteReservationsForExpiredClients extends SimpleUpdate {
	private static final int EXPIRES = 1;
	private static final int TYPE = 2;

	private final DateTime expires;
	private final Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	private final Converter<License.Type, Integer> licenseType2IntegerConverter;

	public DeleteReservationsForExpiredClients(final DateTime expires, final Converter<Timestamp, DateTime> timestamp2DateTimeConverter, final Converter<License.Type, Integer> licenseType2IntegerConverter) {
		super("delete from cls.reservations"
				+ " where clientid in (select clientid from cls.clients where expires < ?) and type = ?");
		this.expires = expires;
		this.timestamp2DateTimeConverter = timestamp2DateTimeConverter;
		this.licenseType2IntegerConverter = licenseType2IntegerConverter;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		statement.setTimestamp(EXPIRES, timestamp2DateTimeConverter.convertFrom(this.expires));
		statement.setInt(TYPE, licenseType2IntegerConverter.convertTo(License.Type.FLOATING_POOL));
	}
}
