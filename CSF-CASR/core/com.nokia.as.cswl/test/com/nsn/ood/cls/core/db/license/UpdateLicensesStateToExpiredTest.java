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
public class UpdateLicensesStateToExpiredTest {
	private static final DateTime DATE = new DateTime(2015, 10, 28, 17, 4);

	@Test
	public void testSql() throws Exception {
		assertEquals("update cls.licenses set state = '" + LicenseState.EXPIRED.toString() + "'"//
				+ " where state = '" + LicenseState.ACTIVE.toString() + "' and enddate < ?",
				new UpdateLicensesStateToExpired(null, null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final Converter<Timestamp, DateTime> converterMock = createMock(Timestamp2DateTimeConverter.class);
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		expect(converterMock.convertFrom(DATE)).andReturn(new Timestamp(12345678L));
		statementMock.setTimestamp(1, new Timestamp(12345678L));

		replayAll();
		new UpdateLicensesStateToExpired(DATE, converterMock).prepare(statementMock);
		verifyAll();
	}
}
