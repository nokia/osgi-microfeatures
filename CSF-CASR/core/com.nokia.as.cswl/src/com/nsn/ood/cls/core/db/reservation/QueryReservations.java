/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.db.ListConditionsQuery;
import com.nsn.ood.cls.core.db.creator.ReservationCreator;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.model.internal.Reservation;


/**
 * @author marynows
 * 
 */
public class QueryReservations extends ListConditionsQuery<Reservation> {
	private final ReservationCreator creator;

	public QueryReservations(final Conditions conditions, final ConditionsMapper mapper,
			final ReservationCreator creator) throws ConditionProcessingException {
		super("select * from cls.reservations", conditions, mapper, null);
		this.creator = creator;
	}

	@Override
	protected Reservation handleRow(final ResultSet resultSet) throws SQLException {
		return this.creator.createReservation(resultSet);
	}
}
