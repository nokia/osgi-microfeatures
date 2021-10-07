/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.model.gen.licenses.Feature;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.ActivityDetail;
import com.nsn.ood.cls.model.internal.ActivityDetail.Status;
import com.nsn.ood.cls.util.CollectionUtils;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * License <-> Activity details
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "license")
@Property(name = "to", value = "activityDetail")
public class License2ActivityDetailConverter implements Converter<License, ActivityDetail> {

	@Override
	public ActivityDetail convertTo(final License license) {
		if (license == null) {
			throw new CLSIllegalArgumentException("License must not be null");
		}

		final ActivityDetail activityDetail = new ActivityDetail()//
				.withStatus(Status.SUCCESS)//
				.withFileName(license.getFileName());
		if (CollectionUtils.isNotEmpty(license.getFeatures())) {
			final Feature feature = license.getFeatures().get(0);
			activityDetail//
					.withFeatureCode(feature.getFeatureCode())//
					.withFeatureName(feature.getFeatureName());
		}
		return activityDetail;
	}

	@Override
	public License convertFrom(final ActivityDetail activityDetail) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
