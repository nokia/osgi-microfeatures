/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.model.gen.features.Allocation;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * Allocation usage <-> DB license type value
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "allocationUsage")
@Property(name = "to", value = "integer")
public class AllocationUsage2IntegerConverter implements Converter<Allocation.Usage, Integer> {
	private static final int POOL = 2;
	private static final int FLOATING_POOL = 4;

	@Override
	public Integer convertTo(final Allocation.Usage usage) {
		throw new CLSRuntimeException("Unsupported operation");
	}

	@Override
	public Allocation.Usage convertFrom(final Integer type) {
		if (type == POOL) {
			return Allocation.Usage.POOL;
		} else if (type == FLOATING_POOL) {
			return Allocation.Usage.FLOATING_POOL;
		}
		throw new CLSIllegalArgumentException("Invalid DB license type: \"" + type + "\"");
	}
}
