/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.cljl.LicenseInstallOptions;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class LicenseInstallOptions2StringConverterTest {
	private LicenseInstallOptions2StringConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new LicenseInstallOptions2StringConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals("[targetId=id, user=uu, force=true]",
				this.converter.convertTo(new LicenseInstallOptionsTest("id", "uu", true)));
		assertEquals("[targetId=id, force=false]",
				this.converter.convertTo(new LicenseInstallOptionsTest("id", null, false)));
		assertEquals("[user=uu, force=false]",
				this.converter.convertTo(new LicenseInstallOptionsTest(null, "uu", false)));
		assertEquals("[force=true]", this.converter.convertTo(new LicenseInstallOptionsTest(null, null, true)));
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

	private static class LicenseInstallOptionsTest implements LicenseInstallOptions {
		private final String targetId;
		private final String username;
		private final boolean force;

		public LicenseInstallOptionsTest(final String targetId, final String username, final boolean force) {
			this.targetId = targetId;
			this.username = username;
			this.force = force;
		}

		@Override
		public boolean isForce() {
			return this.force;
		}

		@Override
		public String getTargetId() {
			return this.targetId;
		}

		@Override
		public String getUsername() {
			return this.username;
		}
	}
}
