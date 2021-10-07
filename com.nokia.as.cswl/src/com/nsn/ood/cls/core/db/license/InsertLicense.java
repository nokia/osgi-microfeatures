/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.license;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTime;

import com.nsn.ood.cls.core.db.SimpleUpdate;
import com.nsn.ood.cls.core.model.LicenseState;
import com.nsn.ood.cls.model.gen.licenses.Feature;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class InsertLicense extends SimpleUpdate {
	private static final int SERIAL_NUMBER = 1;
	private static final int FILE_NAME = 2;
	private static final int START_DATE = 3;
	private static final int END_DATE = 4;
	private static final int MODE = 5;
	private static final int TYPE = 6;
	private static final int TARGET_TYPE = 7;
	private static final int TOTAL = 8;
	private static final int REMAINING = 9;
	private static final int FEATURE_CODE = 10;
	private static final int FEATURE_NAME = 11;
	private static final int STATE = 12;

	private final License license;
	private final LicenseState state;
	private final Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	private final Converter<License.Mode, Integer> licenseMode2IntegerConverter;
	private final Converter<License.Type, Integer> licenseType2IntegerConverter;
	private final Converter<LicenseState, String> licenseState2StringConverter;

	public InsertLicense(final License license, final Converter<Timestamp, DateTime> timestamp2DateTimeConverter,
						 final Converter<License.Mode, Integer> licenseMode2IntegerConverter,
						 final Converter<License.Type, Integer> licenseType2IntegerConverter,
						 final Converter<LicenseState, String> licenseState2StringConverter) {
		super("insert into cls.licenses (serialnumber, filename, startdate, enddate, mode, type, targettype,"
				+ " total, used, remaining, featurecode, featurename, state)"
				+ " values (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?)");
		this.license = license;
		this.state = DateTime.now().isBefore(license.getStartDate()) ? LicenseState.INACTIVE : LicenseState.ACTIVE;
		this.timestamp2DateTimeConverter = timestamp2DateTimeConverter;
		this.licenseMode2IntegerConverter = licenseMode2IntegerConverter;
		this.licenseType2IntegerConverter = licenseType2IntegerConverter;
		this.licenseState2StringConverter = licenseState2StringConverter;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		final Feature feature = this.license.getFeatures().get(0);
		statement.setString(SERIAL_NUMBER, this.license.getSerialNumber());
		statement.setString(FILE_NAME, this.license.getFileName());
		statement.setTimestamp(START_DATE, timestamp2DateTimeConverter.convertFrom(this.license.getStartDate()));
		statement.setTimestamp(END_DATE, timestamp2DateTimeConverter.convertFrom(this.license.getEndDate()));
		statement.setInt(MODE, licenseMode2IntegerConverter.convertTo(this.license.getMode()));
		statement.setInt(TYPE, licenseType2IntegerConverter.convertTo(this.license.getType()));
		statement.setString(TARGET_TYPE, this.license.getTargetType());
		statement.setLong(TOTAL, this.license.getTotalCapacity());
		statement.setLong(REMAINING, this.license.getTotalCapacity());
		statement.setLong(FEATURE_CODE, feature.getFeatureCode());
		statement.setString(FEATURE_NAME, feature.getFeatureName());
		statement.setString(STATE, licenseState2StringConverter.convertTo(this.state));
	}
}
