/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.creator;

import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.assertReservation;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.ResultSet;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.convert.LicenseMode2IntegerConverter;
import com.nsn.ood.cls.core.convert.LicenseType2IntegerConverter;
import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.gen.licenses.License.Mode;
import com.nsn.ood.cls.model.gen.licenses.License.Type;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class ReservationCreatorTest {
	private static final DateTime TIME = new DateTime(2015, 5, 5, 12, 36);
	private static final DateTime END_DATE = new DateTime(2015, 8, 19, 12, 58);

	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter = createMock(Timestamp2DateTimeConverter.class);
	private Converter<License.Mode, Integer> licenseMode2IntegerConverter = createMock(LicenseMode2IntegerConverter.class);
	private Converter<License.Type, Integer> licenseType2IntegerConverter = createMock(LicenseType2IntegerConverter.class);
	private ReservationCreator creator;

	@Before
	public void setUp() throws Exception {
		this.creator = new ReservationCreator();
		setInternalState(creator, "timestamp2DateTimeConverter", timestamp2DateTimeConverter);
		setInternalState(creator, "licenseMode2IntegerConverter", licenseMode2IntegerConverter);
		setInternalState(creator, "licenseType2IntegerConverter", licenseType2IntegerConverter);
	}

	@Test
	public void testCreateReservation() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.getString("clientid")).andReturn("12345");
		expect(resultSetMock.getLong("featurecode")).andReturn(2345L);
		expect(resultSetMock.getString("serialnumber")).andReturn("ABC123");
		expect(resultSetMock.getLong("capacity")).andReturn(777L);
		expect(resultSetMock.getTimestamp("reservationtime")).andReturn(new Timestamp(12345678L));
		expect(this.timestamp2DateTimeConverter.convertTo(new Timestamp(12345678L))).andReturn(TIME);
		expect(resultSetMock.getInt("mode")).andReturn(1);
		expect(this.licenseMode2IntegerConverter.convertFrom(1)).andReturn(Mode.CAPACITY);
		expect(resultSetMock.getInt("type")).andReturn(2);
		expect(this.licenseType2IntegerConverter.convertFrom(2)).andReturn(Type.POOL);
		expect(resultSetMock.getTimestamp("enddate")).andReturn(new Timestamp(23456789L));
		expect(this.timestamp2DateTimeConverter.convertTo(new Timestamp(23456789L))).andReturn(
				END_DATE);
		expect(resultSetMock.getString("filename")).andReturn("file");

		replayAll();
		final Reservation reservation = this.creator.createReservation(resultSetMock);
		verifyAll();

		assertNotNull(reservation);
		assertReservation(reservation, "12345", 2345L, "ABC123", 777L, TIME, Mode.CAPACITY, Type.POOL, END_DATE, "file");
	}
}
