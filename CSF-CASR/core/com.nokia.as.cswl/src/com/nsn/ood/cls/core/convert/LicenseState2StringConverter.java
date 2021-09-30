/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.core.model.LicenseState;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * License state <-> DB string
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "licenseState")
@Property(name = "to", value = "string")
public class LicenseState2StringConverter implements Converter<LicenseState, String> {

	@Override
	public String convertTo(final LicenseState state) {
		if (state != null) {
			return state.toString();
		}
		return null;
	}

	@Override
	public LicenseState convertFrom(final String string) {
		try {
			return LicenseState.fromValue(string);
		} catch (final IllegalArgumentException e) {
			return null;
		}
	}
}
