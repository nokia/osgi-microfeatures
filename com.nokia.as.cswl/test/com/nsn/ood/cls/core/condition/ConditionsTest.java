/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.condition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.condition.Filter.Type;


/**
 * @author marynows
 * 
 */
public class ConditionsTest {
	private Conditions conditions;

	@Before
	public void setUp() throws Exception {
		this.conditions = new Conditions(false);
	}

	@Test
	public void testCreateInstance() throws Exception {
		assertNotNull(this.conditions.pagination());
		assertFalse(this.conditions.pagination().isLimited());

		assertNotNull(this.conditions.sorting());
		assertTrue(this.conditions.sorting().fields().isEmpty());
		assertFalse(this.conditions.sorting().hasFields());

		assertTrue(this.conditions.filters().isEmpty());
		assertFalse(this.conditions.hasFilters());

		assertFalse(this.conditions.skipMetaData());

		assertTrue(new Conditions(true).skipMetaData());
	}

	@Test
	public void testAddFilter() throws Exception {
		final Filter filter1 = new Filter(null, "name1", null);
		final Filter filter2 = new Filter(null, "name2", null);
		final Filter filter3 = new Filter(null, "name1", null);

		testAddFilter(null, false);
		testAddFilter(filter1, true, filter1);
		testAddFilter(null, true, filter1);
		testAddFilter(filter2, true, filter1, filter2);
		testAddFilter(filter3, true, filter1, filter2);
	}

	private void testAddFilter(final Filter filter, final boolean expectedHasFilters, final Filter... expectedFilters) {
		this.conditions.addFilter(filter);
		assertEquals(expectedHasFilters, this.conditions.hasFilters());
		assertEquals(Arrays.asList(expectedFilters), this.conditions.filters());
	}

	@Test
	public void testSetSorting() throws Exception {
		final Sorting defaultSorting = this.conditions.sorting();
		final Sorting sorting1 = new Sorting();
		final Sorting sorting2 = new Sorting();

		testSetSorting(null, defaultSorting);
		testSetSorting(sorting1, sorting1);
		testSetSorting(null, sorting1);
		testSetSorting(sorting2, sorting2);
	}

	private void testSetSorting(final Sorting sorting, final Sorting expectedSorting) {
		this.conditions.setSorting(sorting);
		assertSame(expectedSorting, this.conditions.sorting());
	}

	@Test
	public void testSetPagination() throws Exception {
		assertPagination(this.conditions.pagination(), Pagination.DEFAULT_OFFSET, Pagination.DEFAULT_LIMIT, false);

		this.conditions.setPaginationOffset(1);
		assertPagination(this.conditions.pagination(), 1, Pagination.DEFAULT_LIMIT, true);

		this.conditions.setPaginationLimit(13);
		assertPagination(this.conditions.pagination(), 1, 13, true);

		this.conditions.setPaginationOffset(20);
		this.conditions.setPaginationLimit(10);
		assertPagination(this.conditions.pagination(), 20, 10, true);

		this.conditions.setPaginationOffset(Pagination.DEFAULT_OFFSET);
		this.conditions.setPaginationLimit(Pagination.DEFAULT_LIMIT);
		assertPagination(this.conditions.pagination(), Pagination.DEFAULT_OFFSET, Pagination.DEFAULT_LIMIT, false);

		this.conditions.setPaginationLimit(1);
		assertPagination(this.conditions.pagination(), Pagination.DEFAULT_OFFSET, 1, true);
	}

	private void assertPagination(final Pagination pagination, final int expectedOffset, final int expectedLimit,
			final boolean expectedIsLimited) {
		assertEquals(expectedOffset, pagination.offset());
		assertEquals(expectedLimit, pagination.limit());
		assertEquals(expectedIsLimited, pagination.isLimited());
	}

	@Test
	public void testClone() throws Exception {
		final Conditions c1 = new Conditions(true);
		c1.addFilter(new Filter(Type.EQUAL, "name", "value"));
		c1.setSorting(new Sorting());
		c1.setPaginationLimit(10);
		c1.setPaginationOffset(20);

		final Conditions c2 = c1.clone();

		assertNotSame(c1, c2);
		assertEquals(c1, c2);
		assertEquals(c1.hashCode(), c2.hashCode());

		final Iterator<Filter> it1 = c1.filters().iterator();
		final Iterator<Filter> it2 = c2.filters().iterator();
		for (; it1.hasNext();) {
			final Filter filter1 = it1.next();
			final Filter filter2 = it2.next();

			assertNotSame(filter1, filter2);
			assertEquals(filter1, filter2);
		}

		assertNotSame(c1.sorting(), c2.sorting());
		assertEquals(c1.sorting(), c2.sorting());

		assertNotSame(c1.pagination(), c2.pagination());
		assertEquals(c1.pagination(), c2.pagination());
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(this.conditions.toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		assertTrue(this.conditions.equals(this.conditions));
		assertFalse(this.conditions.equals(null));
		assertFalse(this.conditions.equals("test"));
	}
}
