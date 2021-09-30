/*
 * Copyright (c) 2016 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.nsn.ood.cls.core.db.IterableUpdate;
import com.nsn.ood.cls.core.model.LicenseUpdate;


/**
 * @author wro50095
 *
 */
public class UpdateLicenseUsage extends IterableUpdate<LicenseUpdate> {

	/**
	 * @param license
	 */
	public UpdateLicenseUsage(final List<LicenseUpdate> license) {
		super("update cls.licenses set used=used+?, remaining=remaining-? where serialnumber=?", license);
	}

	@Override
	protected void prepareRow(final PreparedStatement statement, final LicenseUpdate value) throws SQLException {
		statement.setLong(1, value.getUsageDelta());
		statement.setLong(2, value.getUsageDelta());
		statement.setString(3, value.getSerialNumber());
	}

}
