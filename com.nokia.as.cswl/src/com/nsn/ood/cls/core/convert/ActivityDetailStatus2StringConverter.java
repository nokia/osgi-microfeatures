/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.model.internal.ActivityDetail.Status;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * Activity detail status <-> DB string
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "activityDetailStatus")
@Property(name = "to", value = "string")
public class ActivityDetailStatus2StringConverter implements Converter<Status, String> {

	@Override
	public String convertTo(final Status status) {
		if (status != null) {
			return status.toString();
		}
		return null;
	}

	@Override
	public Status convertFrom(final String string) {
		try {
			return Status.fromValue(string);
		} catch (final IllegalArgumentException e) {
			return null;
		}
	}
}
