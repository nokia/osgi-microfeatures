/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.feature;

import static com.nsn.ood.cls.model.internal.test.LicensedFeatureTestUtil.licensedFeature;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.Test;

import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.core.db.creator.LicensedFeatureCreator;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class QueryLicensedFeaturesForCapacityCheckTest {
	private static final String EXPECTED_SQL = "select * from cls.features"
			+ " where (used * 100) >= (total * ?) and (capacitychecktime is null or capacitychecktime < ?)"
			+ " order by featurecode";
	private static final DateTime TIME = new DateTime(2015, 8, 26, 16, 4);

	@Test
	public void testSql() throws Exception {
		assertEquals(EXPECTED_SQL, new QueryLicensedFeaturesForCapacityCheck(0, null, null, null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final Converter<Timestamp, DateTime> converterMock = createMock(Timestamp2DateTimeConverter.class);

		statementMock.setLong(1, 88L);
		expect(converterMock.convertFrom(TIME)).andReturn(new Timestamp(12345678L));
		statementMock.setTimestamp(2, new Timestamp(12345678L));

		replayAll();
		new QueryLicensedFeaturesForCapacityCheck(88L, TIME, null, converterMock).prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testHandleRow() throws Exception {
		final LicensedFeatureCreator licensedFeatureCreatorMock = createMock(LicensedFeatureCreator.class);
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(licensedFeatureCreatorMock.createLicensedFeature(resultSetMock)).andReturn(licensedFeature(234L));

		replayAll();
		assertEquals(licensedFeature(234L), new QueryLicensedFeaturesForCapacityCheck(0, null,
				licensedFeatureCreatorMock, null).handleRow(resultSetMock));
		verifyAll();
	}
}
