/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.model.gen.features.Allocation;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.util.CollectionUtils;
import com.nsn.ood.cls.util.DescriptionBuilder;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * Feature <-> String
 *
 * @author marynows
 *
 */
@Component
@Property(name = "from", value = "feature")
@Property(name = "to", value = "string")
public class Feature2StringConverter implements Converter<Feature, String> {

	@Override
	public String convertTo(final Feature feature) {
		if (feature == null) {
			throw new CLSIllegalArgumentException("Feature must not be null");
		}

		final DescriptionBuilder db = new DescriptionBuilder()//
				.append("featureCode", feature.getFeatureCode())//
				.append("type", feature.getType())//
				.append("capacity", feature.getCapacity());
		if (CollectionUtils.isNotEmpty(feature.getAllocations())) {
			db.append("allocations", createAllocations(feature.getAllocations()));
		}
		return db.build();
	}

	private List<String> createAllocations(final List<Allocation> allocations) {
		return allocations.stream()
				.map(a -> new DescriptionBuilder()//
						.append("capacity", a.getCapacity())//
						.append("license", a.getPoolLicense())//
						.build())
				.collect(Collectors.toList());
	}

	@Override
	public Feature convertFrom(final String string) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
