/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.nsn.ood.cls.core.db.SimpleUpdate;
import com.nsn.ood.cls.model.gen.clients.Client;


/**
 * @author marynows
 * 
 */
public class DeleteReservations extends SimpleUpdate {
	private static final int CLIENT_ID = 1;
	private static final String SQL = "delete from cls.reservations where clientid = ?";

	private final Client client;

	public DeleteReservations(final Client client) {
		super(SQL);
		this.client = client;
	}

	protected DeleteReservations(final String sqlQuery, final Client client) {
		super(SQL + sqlQuery);
		this.client = client;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		statement.setString(CLIENT_ID, this.client.getClientId());
	}
}
