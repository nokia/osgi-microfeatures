/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.model.gen.features.Allocation.Usage;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class AllocationUsage2IntegerConverterTest {
	private AllocationUsage2IntegerConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new AllocationUsage2IntegerConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		try {
			this.converter.convertTo(null);
			fail();
		} catch (final CLSRuntimeException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}

	@Test
	public void testConvertFrom() throws Exception {
		assertEquals(Usage.POOL, this.converter.convertFrom(2));
		assertEquals(Usage.FLOATING_POOL, this.converter.convertFrom(4));

		try {
			this.converter.convertFrom(0);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}
}
