/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.model.internal.LicensedFeature;
import com.nsn.ood.cls.util.DescriptionBuilder;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * Licensed feature <-> String
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "licensedFeature")
@Property(name = "to", value = "string")
public class LicensedFeature2StringConverter implements Converter<LicensedFeature, String> {

	@Override
	public String convertTo(final LicensedFeature feature) {
		if (feature == null) {
			throw new CLSIllegalArgumentException("Licensed feature must not be null");
		}

		return new DescriptionBuilder()//
				.append("featureCode", feature.getFeatureCode())//
				.append("featureName", feature.getFeatureName())//
				.append("totalCapacity", feature.getTotalCapacity())//
				.append("usedCapacity", feature.getUsedCapacity()).build();
	}

	@Override
	public LicensedFeature convertFrom(final String string) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
