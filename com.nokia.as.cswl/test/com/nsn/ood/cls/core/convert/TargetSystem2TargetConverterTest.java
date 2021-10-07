/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static com.nsn.ood.cls.model.test.LicenseTestUtil.target;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.nokia.licensing.dtos.TargetSystem;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 *
 */
public class TargetSystem2TargetConverterTest {
	private TargetSystem2TargetConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new TargetSystem2TargetConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		final TargetSystem targetSystem = new TargetSystem();
		targetSystem.setTargetId("targetId");

		assertEquals(target("targetId"), this.converter.convertTo(targetSystem));
	}

	@Test
	public void testConvertToEmptyTarget() throws Exception {
		final TargetSystem targetSystem = new TargetSystem();
		targetSystem.setModifiedTime(new Date());
		targetSystem.setTargetSystemSignature(new byte[] {
				1 });

		assertEquals(target(), this.converter.convertTo(targetSystem));
	}

	@Test
	public void testConvertToNullTarget() throws Exception {
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
