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
@Property(name = "name", value = "activityDetail")
public class ActivityDetailConditionsMapper extends ConditionsMapper {

	@Start
	public void start() {
		map("activityId", "activityid", Long.class);
		map("fileName", "filename", String.class);
		map("errorCode", "errorcode", String.class);
		map("status", "status", String.class);
		map("featureName", "featurename", String.class);
		map("featureCode", "featurecode", Long.class);
		map("settingKey", "settingkey", String.class);
		map("settingValue", "settingvalue", String.class);
	}
}
