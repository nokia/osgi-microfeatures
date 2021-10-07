/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.DescriptionBuilder;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * License <-> String
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "license")
@Property(name = "to", value = "string")
public class License2StringConverter implements Converter<License, String> {

	@Override
	public String convertTo(final License license) {
		if (license == null) {
			throw new CLSIllegalArgumentException("License must not be null");
		}

		return new DescriptionBuilder()//
				.append("fileName", license.getFileName())//
				.append("serialNumber", license.getSerialNumber())//
				.append("mode", license.getMode())//
				.append("type", license.getType())//
				.append("endDate", license.getEndDate())//
				.append("targetType", license.getTargetType())//
				.append("usedCapacity", license.getUsedCapacity())//
				.append("totalCapacity", license.getTotalCapacity()).build();
	}

	@Override
	public License convertFrom(final String string) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
