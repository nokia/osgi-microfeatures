/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nokia.licensing.dtos.FeatureInfo;
import com.nsn.ood.cls.model.gen.licenses.Feature;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * Stored license feature info <-> License feature
 *
 * @author marynows
 *
 */
@Component
@Property(name = "from", value = "featureInfo")
@Property(name = "to", value = "feature")
public class FeatureInfo2FeatureConverter implements Converter<FeatureInfo, Feature> {

	@Override
	public Feature convertTo(final FeatureInfo featureInfo) {
		if (featureInfo == null) {
			throw new CLSIllegalArgumentException("Feature info must not be null");
		}

		return new Feature()//
				.withFeatureCode(featureInfo.getFeatureCode())//
				.withFeatureName(featureInfo.getFeatureName());
	}

	@Override
	public FeatureInfo convertFrom(final Feature feature) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
