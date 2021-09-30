/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservation;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.Test;

import com.nsn.ood.cls.core.convert.LicenseMode2IntegerConverter;
import com.nsn.ood.cls.core.convert.LicenseType2IntegerConverter;
import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.gen.licenses.License.Mode;
import com.nsn.ood.cls.model.gen.licenses.License.Type;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class InsertReservationsTest {
	private static final DateTime TIME = new DateTime(2015, 2, 12, 10, 2);
	private static final DateTime END_DATE = new DateTime(2015, 8, 19, 13, 54);

	@Test
	public void testSql() throws Exception {
		assertEquals("insert into cls.reservations"
				+ " (clientid, featurecode, serialnumber, capacity, reservationtime, mode, type, enddate, filename)"
				+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?)", new InsertReservations(null, null, null, null).sql());
	}

	@Test
	public void testPrepareRow() throws Exception {
		final Converter<Timestamp, DateTime> timestamp2DateTimeConverter = createMock(Timestamp2DateTimeConverter.class);
		final Converter<License.Mode, Integer> licenseMode2IntegerConverter = createMock(LicenseMode2IntegerConverter.class);
		final Converter<License.Type, Integer> licenseType2IntegerConverter = createMock(LicenseType2IntegerConverter.class);
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		statementMock.setString(1, "12345");
		statementMock.setLong(2, 1234L);
		statementMock.setString(3, "abc");
		statementMock.setLong(4, 10L);
		expect(timestamp2DateTimeConverter.convertFrom(TIME)).andReturn(new Timestamp(TIME.getMillis()));
		statementMock.setTimestamp(5, new Timestamp(TIME.getMillis()));
		expect(licenseMode2IntegerConverter.convertTo(Mode.CAPACITY)).andReturn(1);
		statementMock.setInt(6, 1);
		expect(licenseType2IntegerConverter.convertTo(Type.POOL)).andReturn(2);
		statementMock.setInt(7, 2);
		expect(timestamp2DateTimeConverter.convertFrom(END_DATE)).andReturn(new Timestamp(END_DATE.getMillis()));
		statementMock.setTimestamp(8, new Timestamp(END_DATE.getMillis()));
		statementMock.setString(9, "file");

		replayAll();
		new InsertReservations(null, timestamp2DateTimeConverter, licenseMode2IntegerConverter, licenseType2IntegerConverter)//
				.prepareRow(statementMock,
						reservation("12345", 1234L, "abc", 10L, TIME, Mode.CAPACITY, Type.POOL, END_DATE, "file"));
		verifyAll();
	}
}
