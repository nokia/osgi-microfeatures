/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * License <-> License cancel info
 *
 * @author marynows
 *
 */
@Component
@Property(name = "from", value = "license")
@Property(name = "to", value = "licenseCancelInfo")
public class License2LicenseCancelInfoConverter implements Converter<License, LicenseCancelInfo> {

	@Override
	public LicenseCancelInfo convertTo(final License license) {
		if (license == null) {
			throw new CLSIllegalArgumentException("License must not be null");
		}

		final LicenseCancelInfo licenseCancelInfo = new LicenseCancelInfo();
		licenseCancelInfo.setSerialNbr(license.getSerialNumber());
		licenseCancelInfo.setLicenseFileName(license.getFileName());
		return licenseCancelInfo;
	}

	@Override
	public License convertFrom(final LicenseCancelInfo value) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
