/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class DeleteReservationsByLicenseType extends DeleteReservations {
	private static final int TYPE = 2;

	private final License.Type type;
	private final Converter<License.Type, Integer> licenseType2IntegerConverter;

	public DeleteReservationsByLicenseType(final Client client, final License.Type type, final Converter<License.Type, Integer> licenseType2IntegerConverter) {
		super(" and type = ?", client);
		this.type = type;
		this.licenseType2IntegerConverter = licenseType2IntegerConverter;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		super.prepare(statement);
		statement.setInt(TYPE, licenseType2IntegerConverter.convertTo(this.type));
	}
}
