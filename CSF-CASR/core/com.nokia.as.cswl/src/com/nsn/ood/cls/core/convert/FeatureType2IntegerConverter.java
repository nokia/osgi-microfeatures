/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * Feature type <-> DB license mode value
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "featureType")
@Property(name = "to", value = "integer")
public class FeatureType2IntegerConverter implements Converter<Feature.Type, Integer> {
	private static final int ON_OFF = 1;
	private static final int CAPACITY = 2;

	@Override
	public Integer convertTo(final Feature.Type type) {
		if (type == Feature.Type.ON_OFF) {
			return ON_OFF;
		} else if (type == Feature.Type.CAPACITY) {
			return CAPACITY;
		}
		throw new CLSIllegalArgumentException("Invalid feature type: \"" + type + "\"");
	}

	@Override
	public Feature.Type convertFrom(final Integer mode) {
		if (mode == ON_OFF) {
			return Feature.Type.ON_OFF;
		} else if (mode == CAPACITY) {
			return Feature.Type.CAPACITY;
		}
		throw new CLSIllegalArgumentException("Invalid DB license mode: \"" + mode + "\"");
	}
}
