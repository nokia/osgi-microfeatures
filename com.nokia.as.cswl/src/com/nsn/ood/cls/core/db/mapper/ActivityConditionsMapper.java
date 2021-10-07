/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.mapper;

import java.sql.Timestamp;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Start;

import com.nsn.ood.cls.core.db.util.ConditionsMapper;


/**
 * @author marynows
 * 
 */
@Component(provides = ConditionsMapper.class)
@Property(name = "name", value = "activity")
public class ActivityConditionsMapper extends ConditionsMapper {

	@Start
	public void start() {
		map("id", "id", Long.class);
		map("clientId", "clientid", String.class);
		map("activityTime", "activitytime", Timestamp.class);
		map("operationType", "operationtype", String.class);
		map("result", "result", String.class);
	}
}
