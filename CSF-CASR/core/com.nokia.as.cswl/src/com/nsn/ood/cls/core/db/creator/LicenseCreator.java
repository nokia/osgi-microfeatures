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

import com.nsn.ood.cls.model.gen.licenses.Feature;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.gen.licenses.Target;
import com.nsn.ood.cls.model.internal.StoredLicense;
import com.nsn.ood.cls.util.Strings;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
@Component(provides = LicenseCreator.class)
public class LicenseCreator {
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

	public StoredLicense createStoredLicense(final ResultSet resultSet) throws SQLException {
		final StoredLicense storedLicense = new StoredLicense()//
				.withCustomerName(resultSet.getString("customername"))//
				.withCustomerId(resultSet.getString("customerid"))//
				.withOrderId(resultSet.getString("orderid"))//
				.withUser(resultSet.getString("user"))//
				.withImportDate(timestamp2DateTimeConverter.convertTo(resultSet.getTimestamp("importdate")))//
				.withRemainingCapacity(resultSet.getLong("remaining"));
		setLicenseData(storedLicense, resultSet);
		return storedLicense;
	}

	private void setLicenseData(final License license, final ResultSet resultSet) throws SQLException {
		license.setSerialNumber(resultSet.getString("serialnumber"));
		license.setCapacityUnit(resultSet.getString("capacityunit"));
		license.setCode(resultSet.getString("code"));
		license.setEndDate(timestamp2DateTimeConverter.convertTo(resultSet.getTimestamp("enddate")));
		license.setFileName(resultSet.getString("filename"));
		license.setMode(licenseMode2IntegerConverter.convertFrom(resultSet.getInt("mode")));
		license.setName(resultSet.getString("name"));
		license.setStartDate(timestamp2DateTimeConverter.convertTo(resultSet.getTimestamp("startdate")));
		license.setTargetType(resultSet.getString("targettype"));
		license.setTotalCapacity(resultSet.getLong("total"));
		license.setType(licenseType2IntegerConverter.convertFrom(resultSet.getInt("type")));
		license.setUsedCapacity(resultSet.getLong("used"));

		license.getFeatures().add(new Feature()//
				.withFeatureCode(resultSet.getLong("featurecode"))//
				.withFeatureName(resultSet.getString("featurename")));

		final String[] targetIds = Strings.nullToEmpty(resultSet.getString("targetid")).split(",");
		for (final String targetId : targetIds) {
			if (!targetId.isEmpty()) {
				license.getTargets().add(new Target().withTargetId(targetId));
			}
		}
	}
}
