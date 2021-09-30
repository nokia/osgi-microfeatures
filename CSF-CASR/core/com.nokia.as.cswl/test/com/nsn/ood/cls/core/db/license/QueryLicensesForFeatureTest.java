/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.license;

import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featureCapacity;
import static com.nsn.ood.cls.model.test.JodaTestUtil.assertNow;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.easymock.Capture;
import org.joda.time.DateTime;
import org.junit.Test;

import com.nsn.ood.cls.core.convert.FeatureType2IntegerConverter;
import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.core.db.creator.LicenseCreator;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class QueryLicensesForFeatureTest {
	private static final String EXPECTED_SQL = "select * from cls.stored_licenses" //
			+ " where featurecode = ? and mode = ? and (enddate > ? or enddate is null) and startdate <= ?"//
			+ " and case when ? is null then 1=1 else targettype = ? end"//
			+ " order by type desc, enddate IS NULL DESC, enddate desc";

	@Test
	public void testSql() throws Exception {
		assertEquals(EXPECTED_SQL, new QueryLicensesForFeature(null, null, null, null, null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final Converter<Timestamp, DateTime> timestamp2DatetimeConverter = createMock(Timestamp2DateTimeConverter.class);
		final Converter<Feature.Type, Integer> featureType2IntegerConverter = createMock(FeatureType2IntegerConverter.class);
		final Capture<DateTime> capturedDateTime = new Capture<>();

		expect(timestamp2DatetimeConverter.convertFrom(capture(capturedDateTime))).andReturn(
				new Timestamp(23456789L));
		statementMock.setLong(1, 1234L);
		expect(featureType2IntegerConverter.convertTo(Feature.Type.CAPACITY)).andReturn(3);
		statementMock.setInt(2, 3);
		statementMock.setTimestamp(3, new Timestamp(23456789L));
		statementMock.setTimestamp(4, new Timestamp(23456789L));
		statementMock.setString(5, "TYPE");
		statementMock.setString(6, "TYPE");

		replayAll();
		new QueryLicensesForFeature(client("12345", "TYPE"), featureCapacity(1234L, 50L), null, timestamp2DatetimeConverter, featureType2IntegerConverter)
				.prepare(statementMock);
		verifyAll();

		assertNotNull(capturedDateTime.getValue());
		assertNow(capturedDateTime.getValue());
	}

	@Test
	public void testHandleRow() throws Exception {
		final LicenseCreator licenseCreatorMock = createMock(LicenseCreator.class);
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(licenseCreatorMock.createLicense(resultSetMock)).andReturn(license("123"));

		replayAll();
		assertEquals(license("123"),
				new QueryLicensesForFeature(null, null, licenseCreatorMock, null, null).handleRow(resultSetMock));
		verifyAll();
	}
}
