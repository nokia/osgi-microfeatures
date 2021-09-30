/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import java.util.Date;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.joda.time.DateTime;

import com.nsn.ood.cls.util.convert.Converter;


/**
 * Java Date <-> Joda DateTime
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "date")
@Property(name = "to", value = "dateTime")
public class Date2DateTimeConverter implements Converter<Date, DateTime> {

	@Override
	public DateTime convertTo(final Date date) {
		if (date != null) {
			return new DateTime(date);
		}
		return null;
	}

	@Override
	public Date convertFrom(final DateTime dateTime) {
		if (dateTime != null) {
			return dateTime.toDate();
		}
		return null;
	}
}
