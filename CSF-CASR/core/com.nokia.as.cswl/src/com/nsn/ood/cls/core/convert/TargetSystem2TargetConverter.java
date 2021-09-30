/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nokia.licensing.dtos.TargetSystem;
import com.nsn.ood.cls.model.gen.licenses.Target;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * Stored license target system <-> License target
 *
 * @author marynows
 *
 */
	@Component
	@Property(name = "from", value = "targetSystem")
	@Property(name = "to", value = "target")
public class TargetSystem2TargetConverter implements Converter<TargetSystem, Target> {

	@Override
	public Target convertTo(final TargetSystem targetSystem) {
		if (targetSystem == null) {
			throw new CLSIllegalArgumentException("Target system must not be null");
		}

		return new Target().withTargetId(targetSystem.getTargetId());
	}

	@Override
	public TargetSystem convertFrom(final Target target) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
