/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.condition.Field.Order;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
public class SortingParserTest {
	private SortingParser parser;

	@Before
	public void setUp() throws Exception {
		this.parser = new SortingParser();
	}

	@Test
	public void testParseNull() throws Exception {
		try {
			this.parser.parse(null, null);
			fail();
		} catch (final CLSIllegalArgumentException e) {
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testParse() throws Exception {
		testParse("");
		testParse("   \t");
		testParse(" ,  ,, \t ,,");
		testParse("field", Pair.of("field", Order.ASC));
		testParse("-field", Pair.of("field", Order.DESC));
		testParse(" field1,-field2  ,, -field3 ,field4,-,  - \t",//
				Pair.of("field1", Order.ASC),//
				Pair.of("field2", Order.DESC),//
				Pair.of("field3", Order.DESC),//
				Pair.of("field4", Order.ASC));
	}

	@SuppressWarnings("unchecked")
	private void testParse(final String value, final Pair<String, Order>... expectedFields) {
		final ConditionsBuilder expectedBuilder = ConditionsBuilder.create();
		for (final Pair<String, Order> field : expectedFields) {
			expectedBuilder.sort(field.getLeft(), field.getRight());
		}

		final ConditionsBuilder builder = ConditionsBuilder.create();
		this.parser.parse(builder, value);
		assertEquals(expectedBuilder.build(), builder.build());
	}
}
