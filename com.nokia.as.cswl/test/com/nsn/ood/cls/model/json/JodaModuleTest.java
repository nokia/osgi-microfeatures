/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.json;

import org.joda.time.DateTime;
import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class JodaModuleTest extends AbstractModuleTest {
	private final JodaModule module = new JodaModule();

	@Test
	public void testJodaModule() throws Exception {
		assertNameAndVersion(this.module, "JodaModule");

		captureConfig(this.module, true, true);
		assertSerializer(DateTime.class, DateTimeSerializer.class);
		assertDeserializer(DateTime.class, DateTimeDeserializer.class);
	}
}
