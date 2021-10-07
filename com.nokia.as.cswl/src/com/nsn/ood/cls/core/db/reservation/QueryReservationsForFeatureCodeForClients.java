/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.nsn.ood.cls.core.db.ListQuery;
import com.nsn.ood.cls.core.db.creator.ReservationCreator;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.internal.Reservation;


/**
 * @author marynows
 *
 */
public class QueryReservationsForFeatureCodeForClients extends ListQuery<Reservation> {
	private static final int FEATURE = 1;
	private static final int CLIENT_ID = 2;

	private final ReservationCreator creator;

	private final List<String> clients = new ArrayList<>();
	private final long featureCode;

	public QueryReservationsForFeatureCodeForClients(final long featureCode, final List<Client> clients,
			final ReservationCreator creator) {
		super("select * from cls.reservations where featurecode = ? and clientid in (select * from unnest(?))");
		this.featureCode = featureCode;
		this.creator = creator;
		copyClientIds(clients);
	}

	private void copyClientIds(final List<Client> clients) {
		for (final Client client : clients) {
			this.clients.add(client.getClientId());
		}
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		statement.setLong(FEATURE, this.featureCode);
		statement.setArray(CLIENT_ID, statement.getConnection().createArrayOf("varchar", this.clients.toArray()));
	}

	@Override
	protected Reservation handleRow(final ResultSet resultSet) throws SQLException {
		return this.creator.createReservation(resultSet);
	}
}
