/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.mapper;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Start;

import com.nsn.ood.cls.core.db.util.ConditionsMapper;


/**
 * @author marynows
 * 
 */
@Component(provides = ConditionsMapper.class)
@Property(name = "name", value = "licensedFeature")
public class LicensedFeatureConditionsMapper extends ConditionsMapper {

	@Start
	public void start() {
		map("featureCode", "featurecode", Long.class);
		map("featureName", "featurename", String.class);
		map("capacityUnit", "capacityunit", String.class);
		map("targetType", "targettype", String.class);
		map("totalCapacity", "total", Long.class);
		map("usedCapacity", "used", Long.class);
		map("remainingCapacity", "remaining", Long.class);
	}
}
