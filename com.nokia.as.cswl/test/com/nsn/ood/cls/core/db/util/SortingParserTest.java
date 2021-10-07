/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.util;

import static com.nsn.ood.cls.core.condition.ConditionsTestUtil.field;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.nsn.ood.cls.core.condition.Field;
import com.nsn.ood.cls.core.condition.Field.Order;


/**
 * @author marynows
 * 
 */
public class SortingParserTest {

	@Test
	public void testEmptySorting() throws Exception {
		final SortingParser parser = new SortingParser(Collections.<Field> emptyList(), new ConditionsMapper());

		assertEquals("", parser.sql());
	}

	@Test
	public void testSorting() throws Exception {
		final SortingParser parser = new SortingParser(//
				Arrays.<Field> asList(//
						field("f1", Order.ASC),//
						field("f2", Order.DESC),//
						field("f3", Order.ASC)),//
				new ConditionsMapper()//
						.map("f1", "col1", String.class)//
						.map("f2", "col2", Integer.class)//
						.map("test", "testCol", Long.class));

		assertEquals(" order by col1, col2 desc", parser.sql());
	}
}
