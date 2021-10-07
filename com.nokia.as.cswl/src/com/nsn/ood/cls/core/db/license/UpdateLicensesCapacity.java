/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.license;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.nsn.ood.cls.core.db.InUpdate;


/**
 * @author marynows
 *
 */
public class UpdateLicensesCapacity extends InUpdate<Long> {
	private static final String SQL_ALL = "update cls.licenses l, (select serialnumber, total, coalesce(s.used, 0) as used from cls.licenses"
			+ " left join (select serialnumber, sum(capacity) as used from cls.reservations group by serialnumber) s"
			+ " using (serialnumber)) r" + " set l.used = r.used, l.remaining = r.total - r.used"
			+ " where l.serialnumber = r.serialnumber";
	private static final String SQL_ONE = SQL_ALL + " and l.featurecode = ?";
	private static final String SQL_MORE = SQL_ALL + " and l.featurecode in (%s)";

	public UpdateLicensesCapacity(final List<Long> featureCodes) {
		super(SQL_ALL, SQL_ONE, SQL_MORE, featureCodes);
	}

	@Override
	protected void prepareValue(final PreparedStatement statement, final int index, final Long value) throws SQLException {
		statement.setLong(index, value);
	}
}
