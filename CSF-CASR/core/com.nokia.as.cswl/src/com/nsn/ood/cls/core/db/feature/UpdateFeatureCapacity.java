/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.feature;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.nsn.ood.cls.core.db.InUpdate;
import com.nsn.ood.cls.core.model.LicenseState;


/**
 * @author marynows
 *
 */
public class UpdateFeatureCapacity extends InUpdate<Long> {
	private static final String SQL_ALL = "update cls.features f,"
			+ " (select featurecode, coalesce(l.total, 0) as total, coalesce(r.used, 0) as used from cls.features"
			+ " left join (select featurecode, sum(total) as total from cls.licenses where state = '" + LicenseState.ACTIVE.toString()
			+ "' group by featurecode) l using (featurecode)"
			+ " left join (select featurecode, sum(capacity) as used from cls.reservations group by featurecode) r"
			+ " using (featurecode)) c" + " set f.total = c.total, f.used = c.used, f.remaining = c.total - c.used"
			+ " where f.featurecode = c.featurecode";

	private static final String SQL_ONE = SQL_ALL + " and f.featurecode = ?";
	private static final String SQL_MORE = SQL_ALL + " and f.featurecode in (%s)";

	public UpdateFeatureCapacity(final Long featureCode) {
		this(Arrays.asList(featureCode));
	}

	public UpdateFeatureCapacity(final List<Long> featureCodes) {
		super(SQL_ALL, SQL_ONE, SQL_MORE, featureCodes);
	}

	@Override
	protected void prepareValue(final PreparedStatement statement, final int index, final Long value) throws SQLException {
		statement.setLong(index, value);
	}
}
