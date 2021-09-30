/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.license;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.Test;

import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.core.model.LicenseState;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class QueryLicensesFeatureCodesForStateUpdateTest {
	private static final DateTime DATE = new DateTime(2015, 8, 24, 16, 19);
	private static final Timestamp TIMESTAMP = new Timestamp(12345678L);

	@Test
	public void testSql() throws Exception {
		assertEquals("select distinct featurecode from cls.licenses"//
				+ " where (state = '" + LicenseState.INACTIVE.toString() + "' and startdate <= ?)"//
				+ " or (state = '" + LicenseState.ACTIVE.toString() + "' and enddate < ?)",
				new QueryLicensesFeatureCodesForStateUpdate(null, null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final Converter<Timestamp, DateTime> converterMock = createMock(Timestamp2DateTimeConverter.class);
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		expect(converterMock.convertFrom(DATE)).andReturn(TIMESTAMP);
		statementMock.setTimestamp(1, TIMESTAMP);
		statementMock.setTimestamp(2, TIMESTAMP);

		replayAll();
		new QueryLicensesFeatureCodesForStateUpdate(DATE, converterMock).prepare(statementMock);
		verifyAll();
	}
}
