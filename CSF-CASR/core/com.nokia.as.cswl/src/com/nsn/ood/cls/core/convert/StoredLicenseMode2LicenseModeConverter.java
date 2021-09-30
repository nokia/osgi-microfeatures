/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nokia.licensing.dtos.AddnColumns.LicenseMode;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * Stored license mode <-> License mode
 *
 * @author marynows
 *
 */
@Component
@Property(name = "from", value = "storedLicenseMode")
@Property(name = "to", value = "licenseMode")
public class StoredLicenseMode2LicenseModeConverter implements Converter<LicenseMode, License.Mode> {

	@Override
	public License.Mode convertTo(final LicenseMode mode) {
		if (mode == LicenseMode.CAPACITY) {
			return License.Mode.CAPACITY;
		} else if (mode == LicenseMode.ONOFF) {
			return License.Mode.ON_OFF;
		}
		return null;
	}

	@Override
	public LicenseMode convertFrom(final License.Mode mode) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
