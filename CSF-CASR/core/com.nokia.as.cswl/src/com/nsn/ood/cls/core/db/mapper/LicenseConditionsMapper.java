/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.mapper;

import java.sql.Timestamp;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

import com.nsn.ood.cls.core.db.util.ConditionsMapper;


/**
 * @author marynows
 * 
 */
@Component(provides = ConditionsMapper.class)
@Property(name = "name", value = "license")
public class LicenseConditionsMapper extends ConditionsMapper {
	
	@ServiceDependency(filter = "(name=licenseMode)")
	private ValueConverter<Integer> modeConverter;
	
	@ServiceDependency(filter = "(name=licenseType)")
	private ValueConverter<Integer> typeConverter;
	
	@Start
	public void start() {
		map("serialNumber", "serialnumber", String.class);
		map("fileName", "filename", String.class);
		map("code", "code", String.class);
		map("name", "name", String.class);
		map("mode", "mode", Integer.class, modeConverter);
		map("totalCapacity", "total", Long.class);
		map("usedCapacity", "used", Long.class);
		map("capacityUnit", "capacityunit", String.class);
		map("type", "type", Integer.class, typeConverter);
		map("startDate", "startdate", Timestamp.class);
		map("endDate", "enddate", Timestamp.class, RangePolicy.NULLS_LAST);
		map("targetType", "targettype", String.class);
		map("targetId", "targetid", String.class); // TODO: convert to like '%...%'
		map("featureCode", "featurecode", Long.class);
		map("featureName", "featurename", String.class);
	}
	
}
