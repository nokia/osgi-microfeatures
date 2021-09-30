/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.model.gen.licenses.License.Mode;
import com.nsn.ood.cls.model.gen.licenses.License.Type;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class Reservation2StringConverterTest {
	private static final DateTime TIME = new DateTime();

	private Reservation2StringConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new Reservation2StringConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals("[]", this.converter.convertTo(reservation()));
		assertEquals("[]", this.converter.convertTo(reservation("123")));
		assertEquals("[]", this.converter.convertTo(reservation("123", 12L, null, null, null, null, null, null, null)));

		assertEquals("[serialNumber=ABC]",
				this.converter.convertTo(reservation(null, null, "ABC", null, null, null, null, null, null)));
		assertEquals("[capacity=11]",
				this.converter.convertTo(reservation(null, null, null, 11L, null, null, null, null, null)));
		assertEquals("[reservationTime=" + TIME + "]",
				this.converter.convertTo(reservation(null, null, null, null, TIME, null, null, null, null)));
		assertEquals("[mode=capacity]",
				this.converter.convertTo(reservation(null, null, null, null, null, Mode.CAPACITY, null, null, null)));
		assertEquals("[type=pool]",
				this.converter.convertTo(reservation(null, null, null, null, null, null, Type.POOL, null, null)));
		assertEquals("[endDate=" + TIME + "]",
				this.converter.convertTo(reservation(null, null, null, null, null, null, null, TIME, null)));
		assertEquals("[fileName=FFF]",
				this.converter.convertTo(reservation(null, null, null, null, null, null, null, null, "FFF")));

		assertEquals("[capacity=11, serialNumber=ABC, reservationTime=" + TIME
				+ ", fileName=FFF, mode=on_off, type=floating_pool, endDate=" + TIME + "]",
				this.converter.convertTo(reservation(null, null, "ABC", 11L, TIME, Mode.ON_OFF, Type.FLOATING_POOL,
						TIME, "FFF")));
	}

	@Test
	public void testConvertToNull() throws Exception {
		try {
			this.converter.convertTo(null);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}

	@Test
	public void testConvertFrom() throws Exception {
		try {
			this.converter.convertFrom(null);
			fail();
		} catch (final CLSRuntimeException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}
}
