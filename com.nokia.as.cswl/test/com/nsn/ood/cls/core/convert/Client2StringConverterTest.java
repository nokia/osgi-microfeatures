/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class Client2StringConverterTest {
	private Client2StringConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new Client2StringConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals("[]", this.converter.convertTo(client(null, null, null)));

		assertEquals("[clientId=id]", this.converter.convertTo(client("id", null, null)));
		assertEquals("[keepAliveTime=32]", this.converter.convertTo(client(null, 32L, null)));
		assertEquals("[targetType=type]", this.converter.convertTo(client(null, null, "type")));

		assertEquals("[clientId=id, keepAliveTime=32]", this.converter.convertTo(client("id", 32L, null)));
		assertEquals("[clientId=id, targetType=type]", this.converter.convertTo(client("id", null, "type")));
		assertEquals("[keepAliveTime=32, targetType=type]", this.converter.convertTo(client(null, 32L, "type")));

		assertEquals("[clientId=id, keepAliveTime=32, targetType=type]",
				this.converter.convertTo(client("id", 32L, "type")));
	}

	@Test
	public void testConvertToNull() throws Exception {
		try {
			this.converter.convertTo(null);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}

	@Test
	public void testConvertFrom() throws Exception {
		try {
			this.converter.convertFrom(null);
			fail();
		} catch (final CLSRuntimeException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}
}
