/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.rest.util.QueryFilter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 *
 */
public class FilterParserTest {
	private FilterParser parser;

	@Before
	public void setUp() throws Exception {
		this.parser = new FilterParser();
		Whitebox.setInternalState(this.parser, new QueryFilter());
	}

	@Test
	public void testParseEmptyOrNull() throws Exception {
		assertParseException(null, "value");
		assertParseException("", "value");
		assertParseException("name", null);
	}

	private void assertParseException(final String name, final String value) {
		try {
			this.parser.parse(null, name, value);
			fail();
		} catch (final CLSIllegalArgumentException e) {
		}
	}

	@Test
	public void testParseEqualFilter() throws Exception {
		testParseEqualFilter("field1", "value1");
		testParseEqualFilter("field1", "val❄ue❄1");
		testParseEqualFilter(" field2 ", " value2 ");
		testParseEqualFilter("field3", "");
	}

	private void testParseEqualFilter(final String name, final String value) {
		final ConditionsBuilder builder = ConditionsBuilder.create();
		this.parser.parse(builder, name, value);
		assertEquals(ConditionsBuilder.create().equalFilter(name, replaceStar(value)).build(), builder.build());
	}

	@Test
	public void testParseWildcardFilter() throws Exception {
		testParseWildcardFilter("field1", "*value1");
		testParseWildcardFilter(" field2 ", " value2 *");
		testParseWildcardFilter("field3", "val*ue3");
		testParseWildcardFilter("field4", "*value4*");
		testParseWildcardFilter("field4", "*value4*");
		testParseWildcardFilter("field4", "*val❄ue❄4*");
	}

	private void testParseWildcardFilter(final String name, final String value) {
		final ConditionsBuilder builder = ConditionsBuilder.create();
		this.parser.parse(builder, name, value);
		assertEquals(ConditionsBuilder.create().wildcardFilter(name, replaceStar(value)).build(), builder.build());
	}

	@Test
	public void testParseBetweenFilter() throws Exception {
		testParseBetweenFilter("field1", "value31,value32", "value31", "value32");
		testParseBetweenFilter("field2", " value41,value42 ", " value41", "value42 ");
		testParseBetweenFilter("field3", "value51  ,  value52", "value51  ", "  value52");
		testParseBetweenFilter("field4", "value61,value62,value63", "value61", "value62,value63");
		testParseBetweenFilter("field4", "valu❄e61,value62,val❄ue63", "valu,e61", "value62,val,ue63");
		testParseBetweenFilter("field5", ",", "", "");
	}

	private void testParseBetweenFilter(final String name, final String value, final String from, final String to) {
		final ConditionsBuilder builder = ConditionsBuilder.create();
		this.parser.parse(builder, name, value);
		assertEquals(ConditionsBuilder.create().betweenFilter(name, replaceStar(from), replaceStar(to)).build(),
				builder.build());
	}

	private String replaceStar(final String value) {
		if (value.contains("❄")) {
			return value.replaceAll("❄", ",");
		} else {
			return value;
		}
	}
}
