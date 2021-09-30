/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import java.util.Objects;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.model.internal.ActivityDetail;
import com.nsn.ood.cls.model.internal.ActivityDetail.Status;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * Setting <-> Activity details
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "setting")
@Property(name = "to", value = "activityDetail")
public class Setting2ActivityDetailConverter implements Converter<Setting, ActivityDetail> {

	@Override
	public ActivityDetail convertTo(final Setting setting) {
		if (setting == null) {
			throw new CLSIllegalArgumentException("Setting must not be null");
		}

		return new ActivityDetail()//
				.withStatus(Status.SUCCESS)//
				.withSettingKey(Objects.toString(setting.getKey(), null))//
				.withSettingValue(setting.getValueAsString());
	}

	@Override
	public Setting convertFrom(final ActivityDetail activityDetail) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
