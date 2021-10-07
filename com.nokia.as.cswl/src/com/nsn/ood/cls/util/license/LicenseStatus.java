/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util.license;

/**
 * @author marynows
 * 
 */
public class LicenseStatus {
	private boolean supported = false;
	private String licenseName;

	protected LicenseStatus() {
	}

	public boolean isSupported() {
		return this.supported;
	}

	void setSupported(final boolean supported) {
		this.supported = supported;
	}

	public String getLicenseName() {
		return this.licenseName;
	}

	void setLicenseName(final String licenseName) {
		this.licenseName = licenseName;
	}
}
