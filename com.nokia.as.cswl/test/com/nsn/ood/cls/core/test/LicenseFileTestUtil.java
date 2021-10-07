/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.test;

import static org.junit.Assert.assertEquals;

import com.nsn.ood.cls.core.model.LicenseFile;


/**
 * @author marynows
 * 
 */
public class LicenseFileTestUtil {

	public static LicenseFile licenseFile() {
		return new LicenseFile();
	}

	public static LicenseFile licenseFile(final String fileName, final String content) {
		return licenseFile().withFileName(fileName).withContent(content);
	}

	public static LicenseFile licenseFile(final String serialNumber) {
		return licenseFile().withSerialNumber(serialNumber);
	}

	public static LicenseFile licenseFile(final String serialNumber, final String fileName, final String content) {
		return licenseFile(serialNumber).withFileName(fileName).withContent(content);
	}

	public static void assertLicenseFile(final LicenseFile licenseFile, final String expectedSerialNumber,
			final String expectedFileName, final String expectedContent) {
		assertEquals(expectedSerialNumber, licenseFile.getSerialNumber());
		assertEquals(expectedFileName, licenseFile.getFileName());
		assertEquals(expectedContent, licenseFile.getContent());
	}
}
