/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * License mode <-> DB license mode value
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "licenseMode")
@Property(name = "to", value = "integer")
public class LicenseMode2IntegerConverter implements Converter<License.Mode, Integer> {
	private static final int ON_OFF = 1;
	private static final int CAPACITY = 2;

	@Override
	public Integer convertTo(final License.Mode mode) {
		if (mode == License.Mode.ON_OFF) {
			return ON_OFF;
		} else if (mode == License.Mode.CAPACITY) {
			return CAPACITY;
		}
		throw new CLSIllegalArgumentException("Invalid license mode: \"" + mode + "\"");
	}

	@Override
	public License.Mode convertFrom(final Integer mode) {
		if (mode == ON_OFF) {
			return License.Mode.ON_OFF;
		} else if (mode == CAPACITY) {
			return License.Mode.CAPACITY;
		}
		throw new CLSIllegalArgumentException("Invalid DB license mode: \"" + mode + "\"");
	}
}
