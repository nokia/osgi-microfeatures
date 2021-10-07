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
public class QueryReservationsFeatureCodesForClient extends QueryReservationsFeatureCodes {
	private static final int CLIENT_ID = 1;

	private final Client client;

	public QueryReservationsFeatureCodesForClient(final Client client) {
		super("clientid = ?");
		this.client = client;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		statement.setString(CLIENT_ID, this.client.getClientId());
	}
}
