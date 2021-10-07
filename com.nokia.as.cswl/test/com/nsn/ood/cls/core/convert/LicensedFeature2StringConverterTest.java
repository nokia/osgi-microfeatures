/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static com.nsn.ood.cls.model.internal.test.LicensedFeatureTestUtil.licensedFeature;
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
public class LicensedFeature2StringConverterTest {
	private LicensedFeature2StringConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new LicensedFeature2StringConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals("[]", this.converter.convertTo(licensedFeature()));

		assertEquals("[featureCode=1234]", this.converter.convertTo(licensedFeature(1234L)));
		assertEquals("[featureName=name]", this.converter.convertTo(licensedFeature().withFeatureName("name")));
		assertEquals("[totalCapacity=77]", this.converter.convertTo(licensedFeature().withTotalCapacity(77L)));
		assertEquals("[usedCapacity=44]", this.converter.convertTo(licensedFeature().withUsedCapacity(44L)));

		assertEquals(
				"[featureCode=4321, featureName=NNN, totalCapacity=333, usedCapacity=222]",
				this.converter.convertTo(licensedFeature(4321L).withFeatureName("NNN").withTotalCapacity(333L)
						.withUsedCapacity(222L)));
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
