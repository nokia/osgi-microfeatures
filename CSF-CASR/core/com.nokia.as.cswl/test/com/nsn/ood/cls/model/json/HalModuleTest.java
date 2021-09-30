/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.json;

import org.junit.Test;

import com.nsn.ood.cls.model.gen.hal.Embedded;
import com.nsn.ood.cls.model.gen.hal.Links;


/**
 * @author marynows
 * 
 */
public class HalModuleTest extends AbstractModuleTest {
	private final HalModule module = new HalModule();

	@Test
	public void testHalModule() throws Exception {
		assertNameAndVersion(this.module, "HalModule");

		captureConfig(this.module, false, true);
		assertDeserializer(Links.class, LinksDeserializer.class);
		assertDeserializer(Embedded.class, EmbeddedDeserializer.class);
	}
}
