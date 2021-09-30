/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.Test;

import com.nsn.ood.cls.core.convert.LicenseType2IntegerConverter;
import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class QueryReservationsFeatureCodesForExpiredClientsTest {
	private static final DateTime EXPIRES = new DateTime(2015, 8, 24, 16, 19);

	@Test
	public void testSql() throws Exception {
		assertEquals("select distinct featurecode from cls.reservations"
				+ " where clientid in (select clientid from cls.clients where expires < ?) and type = ?",
				new QueryReservationsFeatureCodesForExpiredClients(null, null, null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final Converter<Timestamp, DateTime> timestamp2DateTimeConverter = createMock(Timestamp2DateTimeConverter.class);
		final Converter<License.Type, Integer> licenseType2IntegerConverter = createMock(LicenseType2IntegerConverter.class);
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		expect(timestamp2DateTimeConverter.convertFrom(EXPIRES)).andReturn(new Timestamp(12345678L));
		statementMock.setTimestamp(1, new Timestamp(12345678L));
		expect(licenseType2IntegerConverter.convertTo(License.Type.FLOATING_POOL)).andReturn(4);
		statementMock.setInt(2, 4);

		replayAll();
		new QueryReservationsFeatureCodesForExpiredClients(EXPIRES, timestamp2DateTimeConverter, licenseType2IntegerConverter).prepare(statementMock);
		verifyAll();
	}
}
