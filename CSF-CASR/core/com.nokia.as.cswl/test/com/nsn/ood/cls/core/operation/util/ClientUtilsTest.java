/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.reflect.Whitebox.setInternalState;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.config.Configuration;
import com.nsn.ood.cls.core.platform.PlatformPreferences;


/**
 * @author marynows
 * 
 */
public class ClientUtilsTest extends ClientUtils {
	private static final DateTime NOW = new DateTime(2015, 1, 21, 11, 14, 54);

	@Before
	public void setUp() throws Exception {
		setInternalState(this, new PlatformPreferences() {
			@Override
			public String getTargetId() {
				return "4321";
			}
		}, new Configuration() {
			@Override
			public Long getDefaultFloatingReleaseTime() {
				return 1337L;
			}
		});
	}

	@Override
	protected long randomValue() {
		super.randomValue();
		return 123456L;
	}

	@Override
	protected DateTime now() {
		super.now();
		return NOW;
	}

	@Test
	public void testCreateNewId() throws Exception {
		assertEquals("4321_00039447", createNewId(234567L));
	}

	@Test
	public void testGetDefaultKeepAliveTime() throws Exception {
		assertEquals(1337L, getDefaultKeepAliveTime());
	}

	@Test
	public void testGenerateETag() throws Exception {
		assertEquals("00000001e240", generateETag());
	}

	@Test
	public void testCalculateDefaultExpiresTime() throws Exception {
		assertTrue(NOW.plusSeconds(1337).isEqual(calculateDefaultExpiresTime()));
	}

	@Test
	public void testCalculateExpiresTime() throws Exception {
		assertTrue(NOW.isEqual(calculateExpiresTime(0)));
		assertTrue(NOW.plusHours(1).isEqual(calculateExpiresTime(3600)));
	}
}
