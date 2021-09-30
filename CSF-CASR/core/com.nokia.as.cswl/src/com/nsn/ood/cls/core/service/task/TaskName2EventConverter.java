/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.task;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.osgi.service.event.Event;

import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "taskName")
@Property(name = "to", value = "event")
public class TaskName2EventConverter implements Converter<String, Event> {

	@Override
	public Event convertTo(final String taskName) {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		if ("releaseCapacityForExpiredClients".equals(taskName)) {
			return new Event("com/nsn/ood/cls/core/event/releaseCapacityForExpiredClients", properties);
		}
		if ("releaseCapacityForExpiredLicenses".equals(taskName)) {
			return new Event("com/nsn/ood/cls/core/event/releaseCapacityForExpiredLicenses", properties);
		}
		if ("updateLicensesState".equals(taskName)) {
			return new Event("com/nsn/ood/cls/core/event/updateLicensesState", properties);
		}
		if ("sendExpiringLicensesEmail".equals(taskName)) {
			return new Event("com/nsn/ood/cls/core/event/sendExpiringLicensesEmail", properties);
		}
		if ("sendCapacityThresholdEmail".equals(taskName)) {
			return new Event("com/nsn/ood/cls/core/event/sendCapacityThresholdEmail", properties);
		}
		throw new CLSIllegalArgumentException("Unknown task");
	}

	@Override
	public String convertFrom(final Event event) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
