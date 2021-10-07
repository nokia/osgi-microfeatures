/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nokia.licensing.dtos.AddnColumns.LicenseType;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * Stored license type <-> License type
 *
 * @author marynows
 *
 */
@Component
@Property(name = "from", value = "storedLicenseType")
@Property(name = "to", value = "licenseType")
public class StoredLicenseType2LicenseTypeConverter implements Converter<LicenseType, License.Type> {

	@Override
	public License.Type convertTo(final LicenseType type) {
		if (type == LicenseType.FLOATING_POOL) {
			return License.Type.FLOATING_POOL;
		} else if (type == LicenseType.POOL) {
			return License.Type.POOL;
		}
		return null;
	}

	@Override
	public LicenseType convertFrom(final License.Type type) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
