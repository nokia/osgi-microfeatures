/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static com.nsn.ood.cls.model.test.LicenseTestUtil.feature;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.nokia.licensing.dtos.FeatureInfo;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 *
 */
public class FeatureInfo2FeatureConverterTest {
	private FeatureInfo2FeatureConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new FeatureInfo2FeatureConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		final FeatureInfo featureInfo = new FeatureInfo();
		featureInfo.setFeatureCode(1234L);
		featureInfo.setFeatureName("featureName");

		assertEquals(feature(1234L, "featureName"), this.converter.convertTo(featureInfo));
	}

	@Test
	public void testConvertToFeatureWithoutName() throws Exception {
		final FeatureInfo featureInfo = new FeatureInfo();
		featureInfo.setFeatureCode(2345L);
		featureInfo.setFeatureInfoSignature(new byte[] {
				1 });
		featureInfo.setModifiedTime(new Date());

		assertEquals(feature(2345L, null), this.converter.convertTo(featureInfo));
	}

	@Test
	public void testConvertToNullFeature() throws Exception {
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
