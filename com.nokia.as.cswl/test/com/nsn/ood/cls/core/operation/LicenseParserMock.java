/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.io.InputStream;

import com.nsn.ood.cls.util.exception.CLSRuntimeException;
import com.nsn.ood.cls.util.license.LicenseParser;
import com.nsn.ood.cls.util.license.LicenseStatus;


/**
 * @author marynows
 * 
 */
public class LicenseParserMock extends LicenseParser {
	private final Boolean isSupported;

	public LicenseParserMock(final Boolean isSupported) {
		this.isSupported = isSupported;
	}

	@Override
	public LicenseStatus parse(final InputStream licenseContent) {
		if (this.isSupported == null) {
			throw new CLSRuntimeException();
		}

		return new LicenseStatus() {
			@Override
			public boolean isSupported() {
				return LicenseParserMock.this.isSupported;
			}
		};
	}
}