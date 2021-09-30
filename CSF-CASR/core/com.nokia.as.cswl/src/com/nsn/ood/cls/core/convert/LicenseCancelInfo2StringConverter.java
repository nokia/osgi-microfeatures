/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nsn.ood.cls.util.DescriptionBuilder;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * License cancel info <-> String
 *
 * @author marynows
 *
 */
@Component
@Property(name = "from", value = "licenseCancelInfo")
@Property(name = "to", value = "string")
public class LicenseCancelInfo2StringConverter implements Converter<LicenseCancelInfo, String> {

	@Override
	public String convertTo(final LicenseCancelInfo info) {
		if (info == null) {
			throw new CLSIllegalArgumentException("License cancel info must not be null");
		}

		return new DescriptionBuilder()//
				.append("fileName", info.getLicenseFileName())//
				.append("serialNumber", info.getSerialNbr())//
				.append("user", info.getUserName()).build();
	}

	@Override
	public LicenseCancelInfo convertFrom(final String string) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
