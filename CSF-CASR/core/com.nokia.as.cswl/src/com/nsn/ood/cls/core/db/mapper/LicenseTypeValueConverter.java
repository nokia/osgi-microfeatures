/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.mapper;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.core.db.util.ConditionsMapper.ValueConverter;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
@Component
@Property(name = "name", value = "licenseType")
public class LicenseTypeValueConverter implements ValueConverter<Integer> {
	@ServiceDependency(filter = "(&(from=licenseType)(to=integer))")
	private Converter<License.Type, Integer> licenseType2IntegerConverter;

	@Override
	public Integer prepareConvert(final String value) {
		return licenseType2IntegerConverter.convertTo(License.Type.fromValue(value));
	}

	@Override
	public String handleConvert(final Integer value) {
		return licenseType2IntegerConverter.convertFrom(value).toString();
	}
}
