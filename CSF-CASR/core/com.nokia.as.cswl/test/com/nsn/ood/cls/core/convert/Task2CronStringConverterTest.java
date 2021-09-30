/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.easymock.PowerMock;

import com.nsn.ood.cls.model.internal.TaskExpression;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class Task2CronStringConverterTest {
	private static final DateTime START_DATE = new DateTime(2015, 5, 1, 0, 0);
	private static final DateTime END_DATE = new DateTime(2016, 1, 1, 0, 0);

	private Task2CronStringConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new Task2CronStringConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		final TaskExpression task = new TaskExpression().withDayOfMonth("6").withDayOfWeek("3").withEnd(END_DATE)
				.withHour("10").withMinute("48").withMonth("5").withSecond("32").withStart(START_DATE)
				.withTimezone("UTC+1").withYear("2015");

		final String cronString = this.converter.convertTo(task);

		assertEquals("32 48 10 6 5 3 2015", cronString);
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
