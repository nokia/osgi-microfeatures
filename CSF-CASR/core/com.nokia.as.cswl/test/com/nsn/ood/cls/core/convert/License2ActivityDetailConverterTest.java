/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.assertActivityDetail;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.feature;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.featuresList;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.model.internal.ActivityDetail.Status;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class License2ActivityDetailConverterTest {
	private License2ActivityDetailConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new License2ActivityDetailConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertActivityDetail(this.converter.convertTo(license()), null, Status.SUCCESS, null, null, null, null, null);
		assertActivityDetail(this.converter.convertTo(license("file", featuresList(feature()))), null, Status.SUCCESS,
				"file", null, null, null, null);
		assertActivityDetail(this.converter.convertTo(license("file", featuresList(feature(1234L, "name")))), null,
				Status.SUCCESS, "file", "name", 1234L, null, null);
		assertActivityDetail(this.converter.convertTo(license("file",
				featuresList(feature(1234L, "name1"), feature(2345L, "name2")))), null, Status.SUCCESS, "file",
				"name1", 1234L, null, null);
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
