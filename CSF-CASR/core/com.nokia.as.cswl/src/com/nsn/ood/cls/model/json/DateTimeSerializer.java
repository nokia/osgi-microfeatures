/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.json;

import java.io.IOException;

import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.nsn.ood.cls.model.CLSConst;


/**
 * @author marynows
 * 
 */
public class DateTimeSerializer extends JsonSerializer<DateTime> {

	@Override
	public void serialize(final DateTime value, final JsonGenerator jgen, final SerializerProvider provider)
			throws IOException {
		jgen.writeString(value.toString(CLSConst.DATE_TIME_FORMAT));
	}
}
