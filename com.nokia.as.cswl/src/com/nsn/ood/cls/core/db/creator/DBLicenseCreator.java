// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nsn.ood.cls.core.db.creator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;

import com.nsn.ood.cls.model.gen.licenses.DBLicense;
import com.nsn.ood.cls.util.convert.Converter;

@Component(provides = DBLicenseCreator.class)
public class DBLicenseCreator {
	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	
	@ServiceDependency(filter = "(&(from=dbLicenseMode)(to=integer))")
	private Converter<DBLicense.LicenseMode, Integer> dbLicenseMode2IntegerConverter;
	
	@ServiceDependency(filter = "(&(from=dbLicenseType)(to=integer))")
	private Converter<DBLicense.LicenseType, Integer> dbLicenseType2IntegerConverter;

    public DBLicense createDBLicense(final ResultSet resultSet) throws SQLException {
        final DBLicense dbLicense = new DBLicense()
                .withSerialNumber(resultSet.getString("serialnumber"))
                .withLicenseFileName(resultSet.getString("licensefilename"))
                .withLicenseCode(resultSet.getString("licensecode"))
                .withLicenseMode(dbLicenseMode2IntegerConverter.convertFrom(resultSet.getInt("licensemode")))
                .withLicenseName(resultSet.getString("licensename"))
                .withLicenseType(dbLicenseType2IntegerConverter.convertFrom(resultSet.getInt("licensetype")))
                .withAdditionalInfo(resultSet.getString("additionalinfo"))
                .withTargetNeType(resultSet.getString("targetnetype"))
                .withCapacityUnit(resultSet.getString("capacityunit"))
                .withStartTime(timestamp2DateTimeConverter.convertTo(resultSet.getTimestamp("starttime")))
                .withEndTime(timestamp2DateTimeConverter.convertTo(resultSet.getTimestamp("endtime")));


        return dbLicense;
    }

}
