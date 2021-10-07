/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import com.nsn.ood.cls.core.db.Query;


/**
 * @author marynows
 *
 */
public class LockReservations implements Query {
	private static final int FEATURE_CODE = 1;

	private final List<Long> featureCodes;

	private int index = 0;

	public LockReservations(final List<Long> featureCodes) {
		Collections.sort(featureCodes);
		this.featureCodes = featureCodes;
	}

	@Override
	public String sql() {
		return "select featurecode from cls.features where featurecode = ?" + getForUpdateIfNeeded();
	}

	private String getForUpdateIfNeeded() {
		if (Boolean.valueOf(System.getProperty("com.nsn.ood.cls.lockingDisabled"))) {
			return "";
		} else {
			return " for update";
		}
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		final Long featureCode = this.featureCodes.get(this.index);
		statement.setLong(FEATURE_CODE, featureCode);
		this.index++;
	}

	@Override
	public Query next() {
		if (this.index < this.featureCodes.size()) {
			return this;
		}
		return null;
	}

	@Override
	public void handle(final ResultSet resultSet) throws SQLException {
	}
}
