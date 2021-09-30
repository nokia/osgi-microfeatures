/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.task;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;

import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class TaskName2EventConverterTest {
	private TaskName2EventConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new TaskName2EventConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertTrue(this.converter.convertTo("releaseCapacityForExpiredClients") instanceof Event);
		assertTrue(this.converter.convertTo("releaseCapacityForExpiredLicenses") instanceof Event);
		assertTrue(this.converter.convertTo("updateLicensesState") instanceof Event);
		assertTrue(this.converter.convertTo("sendExpiringLicensesEmail") instanceof Event);
		assertTrue(this.converter.convertTo("sendCapacityThresholdEmail") instanceof Event);
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
