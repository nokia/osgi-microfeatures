/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.nsn.ood.cls.model.gen.clients.Client;


/**
 * @author marynows
 * 
 */
public class DeleteReservationsByFeatureCode extends DeleteReservations {
	private static final int FEATURE_CODE = 2;

	private final long featureCode;

	public DeleteReservationsByFeatureCode(final Client client, final long featureCode) {
		super(" and featurecode = ?", client);
		this.featureCode = featureCode;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		super.prepare(statement);
		statement.setLong(FEATURE_CODE, this.featureCode);
	}
}
