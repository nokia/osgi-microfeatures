/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.condition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Test;

import com.nsn.ood.cls.core.condition.Field.Order;
import com.nsn.ood.cls.core.condition.Filter.Type;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
public class ConditionsBuilderTest {

	@Test
	public void testCreate() throws Exception {
		final Conditions conditions = ConditionsBuilder.create().build();

		assertNotNull(conditions);
		assertTrue(conditions.filters().isEmpty());
		assertTrue(conditions.sorting().fields().isEmpty());
		assertEquals(0, conditions.pagination().offset());
		assertEquals(Integer.MAX_VALUE, conditions.pagination().limit());
		assertFalse(conditions.skipMetaData());
	}

	@Test
	public void testCreateAndSkipMetaData() throws Exception {
		final Conditions conditions = ConditionsBuilder.createAndSkipMetaData().build();

		assertNotNull(conditions);
		assertTrue(conditions.filters().isEmpty());
		assertTrue(conditions.sorting().fields().isEmpty());
		assertEquals(0, conditions.pagination().offset());
		assertEquals(Integer.MAX_VALUE, conditions.pagination().limit());
		assertTrue(conditions.skipMetaData());
	}

	@Test
	public void testUse() throws Exception {
		final Conditions conditions = new Conditions(true);
		assertEquals(conditions, ConditionsBuilder.use(conditions).build());
	}

	@Test
	public void testEqualFilter() throws Exception {
		{
			final Conditions conditions = ConditionsBuilder.create().equalFilter("name", "value").build();
			assertEquals(1, conditions.filters().size());
			assertFilter(conditions.filters().get(0), "name", "value", Type.EQUAL);
		}
		{
			final Conditions conditions = ConditionsBuilder.create().equalFilter("name", "").build();
			assertEquals(1, conditions.filters().size());
			assertFilter(conditions.filters().get(0), "name", "", Type.EQUAL);
		}
	}

	@Test
	public void testWildcardFilter() throws Exception {
		{
			final Conditions conditions = ConditionsBuilder.create().wildcardFilter("name", "value").build();
			assertEquals(1, conditions.filters().size());
			assertFilter(conditions.filters().get(0), "name", "value", Type.WILDCARD);
		}
		{
			final Conditions conditions = ConditionsBuilder.create().wildcardFilter("name", "").build();
			assertEquals(1, conditions.filters().size());
			assertFilter(conditions.filters().get(0), "name", "", Type.WILDCARD);
		}
	}

	@Test
	public void testBetweenFilter() throws Exception {
		{
			final Conditions conditions = ConditionsBuilder.create().betweenFilter("name", "from", "to").build();
			assertEquals(1, conditions.filters().size());
			assertFilter(conditions.filters().get(0), "name", "from,to", Type.BETWEEN);
		}
		{
			final Conditions conditions = ConditionsBuilder.create().betweenFilter("name", "", "").build();
			assertEquals(1, conditions.filters().size());
			assertFilter(conditions.filters().get(0), "name", ",", Type.BETWEEN);
		}
	}

	@Test
	public void testMultipleFilters() throws Exception {
		final Conditions conditions = ConditionsBuilder.create().equalFilter("name1", "value1")
				.betweenFilter("name2", "from2", "to2").wildcardFilter("name3", "value3").build();

		assertEquals(3, conditions.filters().size());
		assertFilter(conditions.filters().get(0), "name1", "value1", Type.EQUAL);
		assertFilter(conditions.filters().get(1), "name2", "from2,to2", Type.BETWEEN);
		assertFilter(conditions.filters().get(2), "name3", "value3", Type.WILDCARD);
	}

	@Test
	public void testEqualFilterWithWrongParameters() throws Exception {
		final List<Pair<String, String>> parameters = Arrays.asList(//
				Pair.of((String) null, "value"),//
				Pair.of("", "value"),//
				Pair.of("name", (String) null));

		for (final Pair<String, String> p : parameters) {
			try {
				ConditionsBuilder.create().equalFilter(p.getLeft(), p.getRight());
				fail();
			} catch (final CLSIllegalArgumentException e) {
				assertFalse(e.getMessage().isEmpty());
			}
		}
	}

	@Test
	public void testWildcardFilterWithWrongParameters() throws Exception {
		final List<Pair<String, String>> parameters = Arrays.asList(//
				Pair.of((String) null, "value"),//
				Pair.of("", "value"),//
				Pair.of("name", (String) null));

		for (final Pair<String, String> p : parameters) {
			try {
				ConditionsBuilder.create().wildcardFilter(p.getLeft(), p.getRight());
				fail();
			} catch (final CLSIllegalArgumentException e) {
				assertFalse(e.getMessage().isEmpty());
			}
		}
	}

	@Test
	public void testBetweenFilterWithWrongParameters() throws Exception {
		final List<Triple<String, String, String>> parameters = Arrays.asList(//
				Triple.of((String) null, "from", "to"),//
				Triple.of("", "from", "to"),//
				Triple.of("name", (String) null, "to"),//
				Triple.of("name", "from", (String) null));

		for (final Triple<String, String, String> p : parameters) {
			try {
				ConditionsBuilder.create().betweenFilter(p.getLeft(), p.getMiddle(), p.getRight());
				fail();
			} catch (final CLSIllegalArgumentException e) {
				assertFalse(e.getMessage().isEmpty());
			}
		}
	}

	@Test
	public void testSorting() throws Exception {
		final Conditions conditions = ConditionsBuilder.create().sort("name", Order.ASC).build();

		assertEquals(1, conditions.sorting().fields().size());
		assertField(conditions.sorting().fields().get(0), "name", Order.ASC);
	}

	@Test
	public void testMultipleSorting() throws Exception {
		final Conditions conditions = ConditionsBuilder.create().sort("name1", Order.DESC).sort("name2", Order.ASC)
				.sort("name3", Order.DESC).build();

		assertEquals(3, conditions.sorting().fields().size());
		assertField(conditions.sorting().fields().get(0), "name1", Order.DESC);
		assertField(conditions.sorting().fields().get(1), "name2", Order.ASC);
		assertField(conditions.sorting().fields().get(2), "name3", Order.DESC);
	}

	@Test
	public void testSortingWithWrongParameters() throws Exception {
		final List<Pair<String, Order>> parameters = Arrays.asList(//
				Pair.of((String) null, Order.ASC),//
				Pair.of("", Order.DESC),//
				Pair.of("name", (Order) null));

		for (final Pair<String, Order> p : parameters) {
			try {
				ConditionsBuilder.create().sort(p.getLeft(), p.getRight());
				fail();
			} catch (final CLSIllegalArgumentException e) {
				assertFalse(e.getMessage().isEmpty());
			}
		}
	}

	@Test
	public void testPagination() throws Exception {
		assertPagination(ConditionsBuilder.create().offset(0).build().pagination(), 0, Integer.MAX_VALUE);
		assertPagination(ConditionsBuilder.create().offset(30).build().pagination(), 30, Integer.MAX_VALUE);

		assertPagination(ConditionsBuilder.create().limit(1).build().pagination(), 0, 1);
		assertPagination(ConditionsBuilder.create().limit(20).build().pagination(), 0, 20);

		assertPagination(ConditionsBuilder.create().offset(150).limit(50).build().pagination(), 150, 50);
	}

	@Test
	public void testPaginationWithWrongParameters() throws Exception {
		try {
			ConditionsBuilder.create().offset(-1);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
		try {
			ConditionsBuilder.create().limit(0);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}

	protected void assertPagination(final Pagination pagination, final int expectedOffset, final int expectedLimit) {
		assertEquals(expectedOffset, pagination.offset());
		assertEquals(expectedLimit, pagination.limit());
	}

	private void assertFilter(final Filter filter, final String expectedName, final String expectedValue,
			final Type expectedType) {
		assertEquals(expectedName, filter.name());
		assertEquals(expectedValue, filter.value());
		assertEquals(expectedType, filter.type());
	}

	private void assertField(final Field field, final String expectedName, final Order expectedOrder) {
		assertEquals(expectedName, field.name());
		assertEquals(expectedOrder, field.order());
	}
}
