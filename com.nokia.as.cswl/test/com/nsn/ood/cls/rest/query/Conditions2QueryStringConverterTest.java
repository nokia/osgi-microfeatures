/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.query;

import static com.nsn.ood.cls.core.condition.ConditionsTestUtil.betweenFilter;
import static com.nsn.ood.cls.core.condition.ConditionsTestUtil.conditions;
import static com.nsn.ood.cls.core.condition.ConditionsTestUtil.field;
import static com.nsn.ood.cls.core.condition.ConditionsTestUtil.filter;
import static com.nsn.ood.cls.core.condition.ConditionsTestUtil.sorting;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.Field;
import com.nsn.ood.cls.core.condition.Field.Order;
import com.nsn.ood.cls.core.condition.Filter;
import com.nsn.ood.cls.core.condition.Filter.Type;
import com.nsn.ood.cls.core.condition.Pagination;
import com.nsn.ood.cls.core.condition.Sorting;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 *
 */
public class Conditions2QueryStringConverterTest {
	private Conditions2QueryStringConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new Conditions2QueryStringConverter();
	}

	@Test
	public void testConvertToWithFilters() throws Exception {
		testConvertToWithFilters("?name=value", //
				filter(Type.EQUAL, "name", "value"));
		testConvertToWithFilters("?name=*value*", //
				filter(Type.WILDCARD, "name", "*value*"));
		testConvertToWithFilters("?f1=v1&f2=*v2*", //
				filter(Type.EQUAL, "f1", "v1"), //
				filter(Type.WILDCARD, "f2", "*v2*"));
		testConvertToWithFilters("?name=from,to", //
				betweenFilter("name", "from", "to"));
		testConvertToWithFilters("?f1=v1,&f2=,v2&f3=v3,v4", //
				betweenFilter("f1", "v1", ""), //
				betweenFilter("f2", "", "v2"), //
				betweenFilter("f3", "v3", "v4"));
		testConvertToWithFilters("?f1=v1&f2=v2,v3&f3=", //
				filter(Type.EQUAL, "f1", "v1"), //
				betweenFilter("f2", "v2", "v3"), //
				filter(Type.EQUAL, "f3", ""));
		testConvertToWithFilters("?na%20me=v@*%3D%25/%5C%26%2B?.,;:%23*()", //
				filter(Type.EQUAL, "na me", "v@*=%/\\&+?.,;:#*()"));
	}

	private void testConvertToWithFilters(final String expectedQuery, final Filter... filters) {
		final Conditions conditions = conditions(false);
		for (final Filter filter : filters) {
			conditions.addFilter(filter);
		}
		assertEquals(expectedQuery, this.converter.convertTo(conditions));
	}

	@Test
	public void testConvertToWithSorting() throws Exception {
		testConvertToWithSorting("?sort=field", field("field", Order.ASC));
		testConvertToWithSorting("?sort=-field", field("field", Order.DESC));
		testConvertToWithSorting("?sort=f1,-f2,-f3,f4", //
				field("f1", Order.ASC), //
				field("f2", Order.DESC), //
				field("f3", Order.DESC), //
				field("f4", Order.ASC));
	}

	private void testConvertToWithSorting(final String expectedQuery, final Field... fields) {
		final Sorting sorting = sorting();
		for (final Field field : fields) {
			sorting.addField(field.name(), field.order());
		}
		final Conditions conditions = conditions(false);
		conditions.setSorting(sorting);
		assertEquals(expectedQuery, this.converter.convertTo(conditions));
	}

	@Test
	public void testConvertToWithPagination() throws Exception {
		testConvertToWithPagination("?limit=10", Pagination.DEFAULT_OFFSET, 10);
		testConvertToWithPagination("?offset=20", 20, Pagination.DEFAULT_LIMIT);
		testConvertToWithPagination("?offset=20&limit=10", 20, 10);
	}

	private void testConvertToWithPagination(final String expectedQuery, final int offset, final int limit) {
		final Conditions conditions = conditions(false);
		conditions.setPaginationOffset(offset);
		conditions.setPaginationLimit(limit);
		assertEquals(expectedQuery, this.converter.convertTo(conditions));
	}

	@Test
	public void testConvertToWithAll() throws Exception {
		final Sorting sorting = sorting();
		sorting.addField("field1", Order.DESC);
		sorting.addField("field2", Order.ASC);
		final Conditions conditions = conditions(false);
		conditions.addFilter(filter(Type.EQUAL, "field1", "value1"));
		conditions.addFilter(betweenFilter("field2", "value2", "value3"));
		conditions.addFilter(filter(Type.WILDCARD, "field3", "*value4*"));
		conditions.setSorting(sorting);
		conditions.setPaginationOffset(20);
		conditions.setPaginationLimit(10);

		assertEquals("?field1=value1&field2=value2,value3&field3=*value4*&sort=-field1,field2&offset=20&limit=10",
				this.converter.convertTo(conditions));
	}

	@Test
	public void testConvertToEmpty() throws Exception {
		assertEquals("", this.converter.convertTo(conditions(false)));
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
