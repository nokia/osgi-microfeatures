/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.util;

import static com.nsn.ood.cls.core.condition.ConditionsTestUtil.betweenFilter;
import static com.nsn.ood.cls.core.condition.ConditionsTestUtil.filter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.nsn.ood.cls.core.condition.Filter;
import com.nsn.ood.cls.core.condition.Filter.Type;
import com.nsn.ood.cls.core.db.util.ConditionsMapper.RangePolicy;


/**
 * @author marynows
 * 
 */
public class FiltersParserTest {

	@Test
	public void testNoFitlers() throws Exception {
		final FiltersParser parser = new FiltersParser(Collections.<Filter> emptyList(), new ConditionsMapper());

		assertEquals("", parser.sql());
		assertTrue(parser.values().isEmpty());
	}

	@Test
	public void testFilters() throws Exception {
		final FiltersParser parser = new FiltersParser(//
				Arrays.<Filter> asList(//
						filter(Type.EQUAL, "f1", "string"),//
						filter(Type.EQUAL, "f2", ""),//
						filter(Type.EQUAL, "f3", "no mapping"),//
						betweenFilter("f11", "s1", "s2"),//
						betweenFilter("f12", "2", ""),//
						betweenFilter("f13", "", "345"),//
						betweenFilter("f14", "", ""),//
						filter(Type.WILDCARD, "f21", "*str*ing*"),//
						filter(Type.WILDCARD, "f22", "*"),//
						filter(Type.WILDCARD, "f23", "*no mapping*"),//
						filter(Type.WILDCARD, "f24", ""),//
						betweenFilter("f31", "nf1", "nf2"),//
						betweenFilter("f32", "nf3", ""),//
						betweenFilter("f33", "", "nf4"),//
						betweenFilter("f34", "", ""),//
						betweenFilter("f41", "nl1", "nl2"),//
						betweenFilter("f42", "nl3", ""),//
						betweenFilter("f43", "", "nl4"),//
						betweenFilter("f44", "", "")),//
				new ConditionsMapper()//
						.map("f1", "col1", String.class)//
						.map("f2", "col2", String.class)//
						.map("f11", "col11", String.class)//
						.map("f12", "col12", Integer.class)//
						.map("f13", "col13", Long.class)//
						.map("f14", "col14", Timestamp.class)//
						.map("f21", "col21", String.class)//
						.map("f22", "col22", String.class)//
						.map("f24", "col24", String.class)//
						.map("f31", "col31", String.class, RangePolicy.NULLS_FIRST)//
						.map("f32", "col32", String.class, RangePolicy.NULLS_FIRST)//
						.map("f33", "col33", String.class, RangePolicy.NULLS_FIRST)//
						.map("f34", "col34", String.class, RangePolicy.NULLS_FIRST)//
						.map("f41", "col41", String.class, RangePolicy.NULLS_LAST)//
						.map("f42", "col42", String.class, RangePolicy.NULLS_LAST)//
						.map("f43", "col43", String.class, RangePolicy.NULLS_LAST)//
						.map("f44", "col44", String.class, RangePolicy.NULLS_LAST)//
						.map("test", "testCol", String.class));

		assertEquals(" where col1 = ? and col2 is null and col11 >= ? and col11 < ? and col12 >= ? and col13 < ?"
				+ " and lower(col21) like lower(?) and lower(col22) like lower(?) and col24 is null"
				+ " and col31 >= ? and col31 < ? and col32 >= ? and (col33 < ? or col33 is null) and col34 is null"
				+ " and col41 >= ? and col41 < ? and (col42 >= ? or col42 is null) and col43 < ? and col44 is null",
				parser.sql());
		assertEquals(Arrays.<Object> asList("string", "s1", "s2", 2, 345L, "%str%ing%", "%", "nf1", "nf2", "nf3",
				"nf4", "nl1", "nl2", "nl3", "nl4"), parser.values());
	}

	@Test
	public void testFilterWithWrongValue() throws Exception {
		final Filter filter = filter(Type.EQUAL, "f1", "wrong");
		try {
			new FiltersParser(Arrays.asList(filter), new ConditionsMapper().map("f1", "col1", Integer.class));
			fail();
		} catch (final ConditionProcessingException e) {
			assertTrue(e.getCause() instanceof IllegalArgumentException);
			assertEquals("Invalid filter value", e.getMessage());
			assertEquals(filter.name(), e.getError().getPath());
			assertEquals(filter.value(), e.getError().getValue());
			assertFalse(e.getError().getMessage().isEmpty());
		}
	}
}
