/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.model.internal.Activity.OperationType;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * Activity operation type <-> DB string
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "activityOperation")
@Property(name = "to", value = "string")
public class ActivityOperationType2StringConverter implements Converter<OperationType, String> {

	@Override
	public String convertTo(final OperationType operationType) {
		if (operationType != null) {
			return operationType.toString();
		}
		return null;
	}

	@Override
	public OperationType convertFrom(final String string) {
		try {
			return OperationType.fromValue(string);
		} catch (final IllegalArgumentException e) {
			return null;
		}
	}
}
