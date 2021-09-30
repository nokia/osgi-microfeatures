/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.license;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.nsn.ood.cls.core.db.SimpleQuery;
import com.nsn.ood.cls.core.model.LicenseFile;


/**
 * @author marynows
 * 
 */
public class QueryLicenseFile extends SimpleQuery<LicenseFile> {
	private static final int SERIAL_NUMBER = 1;

	private final String serialNumber;

	public QueryLicenseFile(final String serialNumber) {
		super("select licensefilename, licensefilepath from cls.storedlicense where serialnumber = ?", null);
		this.serialNumber = serialNumber;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		statement.setString(SERIAL_NUMBER, this.serialNumber);
	}

	@Override
	protected LicenseFile handleValue(final ResultSet resultSet) throws SQLException {
		return new LicenseFile()//
				.withSerialNumber(this.serialNumber)//
				.withFileName(resultSet.getString("licensefilename"))//
				.withContent(resultSet.getString("licensefilepath"));
	}
}
