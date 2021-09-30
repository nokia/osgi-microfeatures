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

import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class DeleteReservationsForExpiredLicensesTest {
	private static final DateTime END_DATE = new DateTime(2015, 8, 24, 14, 38);

	@Test
	public void testSql() throws Exception {
		assertEquals("delete from cls.reservations where enddate < ?", new DeleteReservationsForExpiredLicenses(null,
				null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final Converter<Timestamp, DateTime> converterMock = createMock(Timestamp2DateTimeConverter.class);

		expect(converterMock.convertFrom(END_DATE)).andReturn(new Timestamp(12345678L));
		statementMock.setTimestamp(1, new Timestamp(12345678L));

		replayAll();
		new DeleteReservationsForExpiredLicenses(END_DATE, converterMock).prepare(statementMock);
		verifyAll();
	}
}
