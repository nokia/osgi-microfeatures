/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.license;

import static com.nsn.ood.cls.model.test.LicenseTestUtil.feature;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.featuresList;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.Test;

import com.nsn.ood.cls.core.convert.LicenseMode2IntegerConverter;
import com.nsn.ood.cls.core.convert.LicenseState2StringConverter;
import com.nsn.ood.cls.core.convert.LicenseType2IntegerConverter;
import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.core.model.LicenseState;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.gen.licenses.License.Mode;
import com.nsn.ood.cls.model.gen.licenses.License.Type;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class InsertLicenseTest {
	private static final DateTime END_DATE = new DateTime(2016, 8, 20, 13, 52);

	@Test
	public void testSql() throws Exception {
		assertEquals("insert into cls.licenses (serialnumber, filename, startdate, enddate, mode, type, targettype,"
				+ " total, used, remaining, featurecode, featurename, state)"
				+ " values (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?)", new InsertLicense(license(), null, null, null, null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		testPrepare(DateTime.now().minusDays(1), LicenseState.ACTIVE);
		testPrepare(DateTime.now(), LicenseState.ACTIVE);
		testPrepare(DateTime.now().plusDays(1), LicenseState.INACTIVE);
	}

	private void testPrepare(final DateTime startDate, final LicenseState expectedState) throws SQLException {
		resetAll();
		final Converter<Timestamp, DateTime> timestamp2DateTimeConverter = createMock(Timestamp2DateTimeConverter.class);
		final Converter<License.Mode, Integer> licenseMode2IntegerConverter = createMock(LicenseMode2IntegerConverter.class);
		final Converter<License.Type, Integer> licenseType2IntegerConverter = createMock(LicenseType2IntegerConverter.class);
		final Converter<LicenseState, String> licenseState2StringConverter = createMock(LicenseState2StringConverter.class);
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		statementMock.setString(1, "serial");
		statementMock.setString(2, "file");
		expect(timestamp2DateTimeConverter.convertFrom(startDate)).andReturn(new Timestamp(12345678L));
		statementMock.setTimestamp(3, new Timestamp(12345678L));
		expect(timestamp2DateTimeConverter.convertFrom(END_DATE)).andReturn(new Timestamp(23456789L));
		statementMock.setTimestamp(4, new Timestamp(23456789L));
		expect(licenseMode2IntegerConverter.convertTo(Mode.CAPACITY)).andReturn(1);
		statementMock.setInt(5, 1);
		expect(licenseType2IntegerConverter.convertTo(Type.POOL)).andReturn(2);
		statementMock.setInt(6, 2);
		statementMock.setString(7, "target");
		statementMock.setLong(8, 333L);
		statementMock.setLong(9, 333L);
		statementMock.setLong(10, 1001L);
		statementMock.setString(11, "fff");
		expect(licenseState2StringConverter.convertTo(expectedState)).andReturn("state");
		statementMock.setString(12, "state");

		replayAll();
		new InsertLicense(license("serial", Type.POOL, Mode.CAPACITY, startDate, END_DATE, 333L, 0L, "unit", "code",
				"name", "file", "target").withFeatures(featuresList(feature(1001L, "fff"))), timestamp2DateTimeConverter, licenseMode2IntegerConverter,
				licenseType2IntegerConverter, licenseState2StringConverter)
				.prepare(statementMock);
		verifyAll();
	}
}
