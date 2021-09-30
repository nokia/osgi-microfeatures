/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import com.nsn.ood.cls.core.db.SimpleDistinctQuery;


/**
 * @author marynows
 * 
 */
public abstract class QueryReservationsFeatureCodes extends SimpleDistinctQuery<Long> {

	protected QueryReservationsFeatureCodes(final String whereSql) {
		super("select distinct featurecode from cls.reservations where " + whereSql, Long.class);
	}
}
