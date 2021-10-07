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
 * License type <-> DB license type value
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "licenseType")
@Property(name = "to", value = "integer")
public class LicenseType2IntegerConverter implements Converter<License.Type, Integer> {
	private static final int POOL = 2;
	private static final int FLOATING_POOL = 4;

	@Override
	public Integer convertTo(final License.Type type) {
		if (type == License.Type.POOL) {
			return POOL;
		} else if (type == License.Type.FLOATING_POOL) {
			return FLOATING_POOL;
		}
		throw new CLSIllegalArgumentException("Invalid license type: \"" + type + "\"");
	}

	@Override
	public License.Type convertFrom(final Integer type) {
		if (type == POOL) {
			return License.Type.POOL;
		} else if (type == FLOATING_POOL) {
			return License.Type.FLOATING_POOL;
		}
		throw new CLSIllegalArgumentException("Invalid DB license type: \"" + type + "\"");
	}
}
