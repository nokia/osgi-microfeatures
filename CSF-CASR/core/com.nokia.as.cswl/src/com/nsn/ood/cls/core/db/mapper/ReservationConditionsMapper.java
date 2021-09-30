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
@Property(name = "name", value = "reservation")
public class ReservationConditionsMapper extends ConditionsMapper {
	
	@ServiceDependency(filter = "(name=licenseMode)")
	private ValueConverter<Integer> modeConverter;
	
	@ServiceDependency(filter = "(name=licenseType)")
	private ValueConverter<Integer> typeConverter;

	@Start
	public void start() {
		map("featureCode", "featurecode", Long.class);
		map("serialNumber", "serialnumber", String.class);
		map("clientId", "clientid", String.class);
		map("capacity", "capacity", Long.class);
		map("reservationTime", "reservationtime", Timestamp.class);
		map("mode", "mode", Integer.class, modeConverter);
		map("type", "type", Integer.class, typeConverter);
		map("endDate", "enddate", Timestamp.class);
		map("fileName", "filename", String.class);
	}
}
