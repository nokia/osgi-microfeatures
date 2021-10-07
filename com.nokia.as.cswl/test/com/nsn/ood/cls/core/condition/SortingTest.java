/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.condition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.condition.Field.Order;


/**
 * @author marynows
 * 
 */
public class SortingTest {
	private Sorting sorting;

	@Before
	public void setUp() throws Exception {
		this.sorting = new Sorting();
	}

	@Test
	public void testCreateInstance() throws Exception {
		assertTrue(this.sorting.fields().isEmpty());
		assertFalse(this.sorting.hasFields());
	}

	@Test
	public void testAddField() throws Exception {
		this.sorting.addField(null, null);
		assertFalse(this.sorting.hasFields());
		assertNames();
		assertOrders();

		this.sorting.addField("", null);
		assertFalse(this.sorting.hasFields());
		assertNames();
		assertOrders();

		this.sorting.addField("name", null);
		assertFalse(this.sorting.hasFields());
		assertNames();
		assertOrders();

		this.sorting.addField(null, Order.ASC);
		assertFalse(this.sorting.hasFields());
		assertNames();
		assertOrders();

		this.sorting.addField("name1", Order.ASC);
		assertTrue(this.sorting.hasFields());
		assertNames("name1");
		assertOrders(Order.ASC);

		this.sorting.addField("name2", Order.DESC);
		assertTrue(this.sorting.hasFields());
		assertNames("name1", "name2");
		assertOrders(Order.ASC, Order.DESC);

		this.sorting.addField("name1", Order.DESC);
		assertTrue(this.sorting.hasFields());
		assertNames("name1", "name2");
		assertOrders(Order.ASC, Order.DESC);
	}

	private void assertNames(final String... expectedNames) {
		final List<String> names = new ArrayList<>();
		for (final Field field : this.sorting.fields()) {
			names.add(field.name());
		}
		assertEquals(Arrays.asList(expectedNames), names);
	}

	private void assertOrders(final Order... expectedOrders) {
		final List<Order> orders = new ArrayList<>();
		for (final Field field : this.sorting.fields()) {
			orders.add(field.order());
		}
		assertEquals(Arrays.asList(expectedOrders), orders);
	}

	@Test
	public void testClone() throws Exception {
		final Sorting sorting1 = new Sorting();
		sorting1.addField("field1", Order.ASC);
		sorting1.addField("field2", Order.DESC);

		final Sorting sorting2 = sorting1.clone();

		assertNotSame(sorting1, sorting2);
		assertEquals(sorting1, sorting2);
		assertEquals(sorting1.hashCode(), sorting2.hashCode());

		final Iterator<Field> it1 = sorting1.fields().iterator();
		final Iterator<Field> it2 = sorting2.fields().iterator();
		for (; it1.hasNext();) {
			final Field field1 = it1.next();
			final Field field2 = it2.next();

			assertNotSame(field1, field2);
			assertEquals(field1, field2);
		}
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(this.sorting.toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		assertTrue(this.sorting.equals(this.sorting));
		assertFalse(this.sorting.equals(null));
		assertFalse(this.sorting.equals("test"));
	}
}
