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
import com.nsn.ood.cls.model.gen.licenses.License.Type;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class DeleteReservationsForExpiredClientsTest {
	private static final String EXPECTED_SQL = "delete from cls.reservations"
			+ " where clientid in (select clientid from cls.clients where expires < ?) and type = ?";
	private static final DateTime EXPIRES = new DateTime(2015, 8, 24, 14, 36);

	@Test
	public void testSql() throws Exception {
		assertEquals(EXPECTED_SQL, new DeleteReservationsForExpiredClients(null, null, null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final Converter<Timestamp, DateTime> timestamp2DateTimeConverter = createMock(Timestamp2DateTimeConverter.class);
		final Converter<License.Type, Integer> licenseType2IntegerConverter = createMock(LicenseType2IntegerConverter.class);

		expect(timestamp2DateTimeConverter.convertFrom(EXPIRES)).andReturn(new Timestamp(3456789L));
		statementMock.setTimestamp(1, new Timestamp(3456789L));
		expect(licenseType2IntegerConverter.convertTo(Type.FLOATING_POOL)).andReturn(4);
		statementMock.setInt(2, 4);

		replayAll();
		new DeleteReservationsForExpiredClients(EXPIRES, timestamp2DateTimeConverter, licenseType2IntegerConverter).prepare(statementMock);
		verifyAll();
	}
}
