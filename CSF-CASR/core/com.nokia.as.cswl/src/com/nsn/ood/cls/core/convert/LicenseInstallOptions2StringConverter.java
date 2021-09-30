/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.cljl.LicenseInstallOptions;
import com.nsn.ood.cls.util.DescriptionBuilder;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * License install options <-> String
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "licenseInstallOptions")
@Property(name = "to", value = "string")
public class LicenseInstallOptions2StringConverter implements Converter<LicenseInstallOptions, String> {

	@Override
	public String convertTo(final LicenseInstallOptions options) {
		if (options == null) {
			throw new CLSIllegalArgumentException("License install options must not be null");
		}

		return new DescriptionBuilder()//
				.append("targetId", options.getTargetId())//
				.append("user", options.getUsername())//
				.append("force", options.isForce()).build();
	}

	@Override
	public LicenseInstallOptions convertFrom(final String string) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
