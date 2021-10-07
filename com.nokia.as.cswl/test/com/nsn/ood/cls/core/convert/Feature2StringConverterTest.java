/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static com.nsn.ood.cls.model.test.FeatureTestUtil.allocation;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.feature;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featureCapacity;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featureOnOff;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class Feature2StringConverterTest {
	private Feature2StringConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new Feature2StringConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals("[]", this.converter.convertTo(feature()));
		assertEquals("[featureCode=12]", this.converter.convertTo(feature(12L)));
		assertEquals("[featureCode=12, type=capacity, capacity=23]",
				this.converter.convertTo(featureCapacity(12L, 23L)));
		assertEquals("[featureCode=12, type=on_off]", this.converter.convertTo(featureOnOff(12L)));

		assertEquals("[featureCode=12, type=capacity, capacity=23, allocations=[[]]]",
				this.converter.convertTo(featureCapacity(12L, 23L, allocation())));
		assertEquals("[featureCode=12, type=capacity, capacity=23, allocations=[[capacity=34]]]",
				this.converter.convertTo(featureCapacity(12L, 23L, allocation(34L))));
		assertEquals("[featureCode=12, type=capacity, capacity=23, allocations=[[capacity=34, license=1234]]]",
				this.converter.convertTo(featureCapacity(12L, 23L, allocation(34L, "1234"))));
		assertEquals("[featureCode=12, type=capacity, capacity=23, allocations=[[capacity=34], [license=2345]]]",
				this.converter.convertTo(featureCapacity(12L, 23L, allocation(34L), allocation(null, "2345"))));
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
