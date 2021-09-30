/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.powermock.reflect.Whitebox.getInternalState;

import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.core.db.util.ConditionsMapper.Column;
import com.nsn.ood.cls.core.db.util.ConditionsMapper.RangePolicy;
import com.nsn.ood.cls.core.db.util.ConditionsMapper.ValueConverter;


/**
 * @author marynows
 * 
 */
public class ConditionsMapperTestUtil {

	public static void assertMapperColumn(final ConditionsMapper mapper, final String field, final String expectedName) {
		assertMapperColumn(mapper, field, expectedName, RangePolicy.IGNORE_NULLS);
	}

	public static void assertMapperColumn(final ConditionsMapper mapper, final String field, final String expectedName,
			final RangePolicy expectedRangePolicy) {
		assertTrue(mapper.hasColumn(field));

		final Column column = mapper.getColumn(field);
		assertEquals(field, column.field());
		assertEquals(expectedName, column.name());
		assertEquals(expectedRangePolicy, column.rangePolicy());
	}

	public static void assertMapperColumnConverter(final ConditionsMapper mapper, final String field,
			final ValueConverter<?> expectedConverter) {
		final Column column = mapper.getColumn(field);
		assertSame(expectedConverter, getInternalState(column, ValueConverter.class));
	}

	public static void assertMapperColumnValue(final ConditionsMapper mapper, final String field, final String value,
			final Object expectedValue) {
		assertEquals(expectedValue, mapper.getColumn(field).prepareParse(value));
		assertEquals(value, mapper.getColumn(field).handleParse(expectedValue));
	}
}
