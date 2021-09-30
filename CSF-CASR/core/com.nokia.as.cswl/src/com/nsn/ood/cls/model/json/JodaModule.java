/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.json;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.module.SimpleModule;


/**
 * @author marynows
 * 
 */
public class JodaModule extends SimpleModule {
	private static final long serialVersionUID = 5358701873589178917L;

	public JodaModule() {
		super("JodaModule");
		addSerializer(DateTime.class, new DateTimeSerializer());
		addDeserializer(DateTime.class, new DateTimeDeserializer());
	}
}
