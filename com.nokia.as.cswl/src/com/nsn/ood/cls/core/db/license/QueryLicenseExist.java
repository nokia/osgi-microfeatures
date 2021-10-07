/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.license;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.nsn.ood.cls.core.db.SimpleQuery;


/**
 * @author marynows
 * 
 */
public class QueryLicenseExist extends SimpleQuery<Boolean> {
	private final String serialNumber;

	public QueryLicenseExist(final String serialNumber) {
		super("select 1 from cls.licenses where serialnumber = ?", false);
		this.serialNumber = serialNumber;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		statement.setString(1, this.serialNumber);
	}

	@Override
	protected Boolean handleValue(final ResultSet resultSet) throws SQLException {
		return resultSet.getBoolean(1);
	}
}
