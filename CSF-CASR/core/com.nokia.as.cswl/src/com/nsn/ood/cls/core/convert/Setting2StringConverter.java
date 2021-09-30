/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.util.DescriptionBuilder;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * Setting <-> String
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "setting")
@Property(name = "to", value = "string")
public class Setting2StringConverter implements Converter<Setting, String> {

	@Override
	public String convertTo(final Setting setting) {
		if (setting == null) {
			throw new CLSIllegalArgumentException("Setting must not be null");
		}

		return new DescriptionBuilder()//
				.append("key", setting.getKey())//
				.append("value", setting.getValue())//
				.build();
	}

	@Override
	public Setting convertFrom(final String string) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
