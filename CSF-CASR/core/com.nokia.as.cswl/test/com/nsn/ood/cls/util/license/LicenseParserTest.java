/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util.license;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author wro50095
 * 
 */
public class LicenseParserTest {
	private LicenseParser parser;

	@Before
	public void setUp() throws Exception {
		this.parser = new LicenseParser();
	}

	@Test
	public void testParse() throws Exception {
		final LicenseStatus status = this.parser.parse(getClass().getResourceAsStream("/licenses/K1400764.XML"));

		assertEquals("HSDPA BTS processing set 3 RLK", status.getLicenseName());
		assertTrue(status.isSupported());
	}

	@Test
	public void testParseCorrupted() throws Exception {
		try {
			this.parser.parse(new ByteArrayInputStream("test".getBytes()));
			fail();
		} catch (final CLSRuntimeException e) {
		}
	}
}
