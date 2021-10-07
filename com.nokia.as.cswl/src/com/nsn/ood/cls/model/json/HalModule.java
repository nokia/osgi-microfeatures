/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.nsn.ood.cls.model.gen.hal.Embedded;
import com.nsn.ood.cls.model.gen.hal.Links;


/**
 * @author marynows
 * 
 */
public class HalModule extends SimpleModule {
	private static final long serialVersionUID = 4351940773312355494L;

	public HalModule() {
		super("HalModule");
		addDeserializer(Links.class, new LinksDeserializer());
		addDeserializer(Embedded.class, getEmbeddedDeserializer());
	}

	protected EmbeddedDeserializer getEmbeddedDeserializer() {
		return new EmbeddedDeserializer();
	}
}
