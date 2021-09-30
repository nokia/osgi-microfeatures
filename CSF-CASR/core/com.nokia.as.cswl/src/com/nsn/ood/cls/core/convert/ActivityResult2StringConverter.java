/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.model.internal.Activity.Result;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * Activity result <-> DB string
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "activityResult")
@Property(name = "to", value = "string")
public class ActivityResult2StringConverter implements Converter<Result, String> {

	@Override
	public String convertTo(final Result result) {
		if (result != null) {
			return result.toString();
		}
		return null;
	}

	@Override
	public Result convertFrom(final String string) {
		try {
			return Result.fromValue(string);
		} catch (final IllegalArgumentException e) {
			return null;
		}
	}
}
