/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import java.sql.Timestamp;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.joda.time.DateTime;

import com.nsn.ood.cls.util.convert.Converter;


/**
 * DB timestamp <-> Joda DateTime
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "timestamp")
@Property(name = "to", value = "dateTime")
public class Timestamp2DateTimeConverter implements Converter<Timestamp, DateTime> {

	@Override
	public DateTime convertTo(final Timestamp timestamp) {
		if (timestamp != null) {
			return new DateTime(timestamp);
		}
		return null;
	}

	@Override
	public Timestamp convertFrom(final DateTime dateTime) {
		if (dateTime != null) {
			return new Timestamp(dateTime.getMillis());
		}
		return null;
	}
}
