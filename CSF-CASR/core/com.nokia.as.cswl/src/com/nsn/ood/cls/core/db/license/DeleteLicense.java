/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.license;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.nsn.ood.cls.core.db.SimpleUpdate;


/**
 * @author marynows
 * 
 */
public class DeleteLicense extends SimpleUpdate {
	private static final int SERIAL_NUMBER = 1;

	private final String serialNumber;

	public DeleteLicense(final String serialNumber) {
		super("delete from cls.licenses where serialnumber = ?");
		this.serialNumber = serialNumber;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		statement.setString(SERIAL_NUMBER, this.serialNumber);
	}
}
