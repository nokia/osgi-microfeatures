/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.creator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;

import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author wro50095
 *
 */
@Component(provides = LicenseCreatorFast.class)
public class LicenseCreatorFast {
	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	
	@ServiceDependency(filter = "(&(from=licenseMode)(to=integer))")
	private Converter<License.Mode, Integer> licenseMode2IntegerConverter;
	
	@ServiceDependency(filter = "(&(from=licenseType)(to=integer))")
	private Converter<License.Type, Integer> licenseType2IntegerConverter;

	public License createLicense(final ResultSet resultSet) throws SQLException {
		final License license = new License();
		setLicenseData(license, resultSet);
		return license;
	}

	private void setLicenseData(final License license, final ResultSet resultSet) throws SQLException {
		license.setSerialNumber(resultSet.getString("serialnumber"));
		license.setEndDate(timestamp2DateTimeConverter.convertTo(resultSet.getTimestamp("enddate")));
		license.setFileName(resultSet.getString("filename"));
		license.setMode(licenseMode2IntegerConverter.convertFrom(resultSet.getInt("mode")));
		license.setTotalCapacity(resultSet.getLong("total"));
		license.setType(licenseType2IntegerConverter.convertFrom(resultSet.getInt("type")));
		license.setUsedCapacity(resultSet.getLong("used"));
		license.setTargetType(resultSet.getString("targettype"));
	}
}
