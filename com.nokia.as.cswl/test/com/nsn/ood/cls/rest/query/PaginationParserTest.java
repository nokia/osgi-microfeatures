/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
public class PaginationParserTest {
	private PaginationParser parser;

	@Before
	public void setUp() throws Exception {
		this.parser = new PaginationParser();
	}

	@Test
	public void testParseNull() throws Exception {
		try {
			this.parser.parseOffset(null, null);
			fail();
		} catch (final CLSIllegalArgumentException e) {
		}
		try {
			this.parser.parseLimit(null, null);
			fail();
		} catch (final CLSIllegalArgumentException e) {
		}
	}

	@Test
	public void testParseOffset() throws Exception {
		testParseOffset("", 0);
		testParseOffset("asd", 0);
		testParseOffset("1a", 0);
		testParseOffset("1.23", 0);
		testParseOffset("-1", 0);
		testParseOffset("0", 0);
		testParseOffset("1", 1);
		testParseOffset("2147483646", Integer.MAX_VALUE - 1);
		testParseOffset("2147483647", Integer.MAX_VALUE);
		testParseOffset("2147483648", 0);
		testParseOffset(" 1 ", 1);
		testParseOffset(" 1 2", 0);
	}

	private void testParseOffset(final String value, final int expectedOffset) {
		final ConditionsBuilder builder = ConditionsBuilder.create();
		this.parser.parseOffset(builder, value);
		assertEquals(ConditionsBuilder.create().offset(expectedOffset).build(), builder.build());
	}

	@Test
	public void testParseLimit() throws Exception {
		testParseLimit("", Integer.MAX_VALUE);
		testParseLimit("asd", Integer.MAX_VALUE);
		testParseLimit("1a", Integer.MAX_VALUE);
		testParseLimit("1.23", Integer.MAX_VALUE);
		testParseLimit("-1", Integer.MAX_VALUE);
		testParseLimit("0", Integer.MAX_VALUE);
		testParseLimit("1", 1);
		testParseLimit("2147483646", Integer.MAX_VALUE - 1);
		testParseLimit("2147483647", Integer.MAX_VALUE);
		testParseLimit("2147483648", Integer.MAX_VALUE);
		testParseLimit(" 1 ", 1);
		testParseLimit(" 1 2", Integer.MAX_VALUE);
	}

	private void testParseLimit(final String value, final int expectedLimit) {
		final ConditionsBuilder builder = ConditionsBuilder.create();
		this.parser.parseLimit(builder, value);
		assertEquals(ConditionsBuilder.create().limit(expectedLimit).build(), builder.build());
	}
}
