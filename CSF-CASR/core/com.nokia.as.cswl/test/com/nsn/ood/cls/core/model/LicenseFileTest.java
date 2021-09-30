/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class LicenseFileTest {
	private static final LicenseFile LICENSE_FILE = new LicenseFile().withSerialNumber("12345").withFileName("aaa")
			.withContent("ccc");

	@Test
	public void testLicenseFile() throws Exception {
		final LicenseFile licenseFile = new LicenseFile();
		assertNull(licenseFile.getSerialNumber());
		assertNull(licenseFile.getFileName());
		assertNull(licenseFile.getContent());

		licenseFile.setSerialNumber("serialNumber");
		assertEquals("serialNumber", licenseFile.getSerialNumber());

		licenseFile.setFileName("fileName");
		assertEquals("fileName", licenseFile.getFileName());

		licenseFile.setContent("content");
		assertEquals("content", licenseFile.getContent());
	}

	@Test
	public void testBuilders() throws Exception {
		assertEquals("12345", LICENSE_FILE.getSerialNumber());
		assertEquals("aaa", LICENSE_FILE.getFileName());
		assertEquals("ccc", LICENSE_FILE.getContent());
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(LICENSE_FILE.toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		assertFalse(LICENSE_FILE.equals(null));
		assertFalse(LICENSE_FILE.equals("test"));
		assertEquals(LICENSE_FILE, LICENSE_FILE);

		assertFalse(LICENSE_FILE.equals(new LicenseFile()));
		assertNotEquals(LICENSE_FILE.hashCode(), new LicenseFile().hashCode());

		final LicenseFile licenseFile2 = new LicenseFile().withSerialNumber("12345").withFileName("aaa")
				.withContent("ccc");
		assertEquals(LICENSE_FILE, licenseFile2);
		assertEquals(LICENSE_FILE.hashCode(), licenseFile2.hashCode());
	}
}
