/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.db.util.ConditionsMapper.RangePolicy;
import com.nsn.ood.cls.core.db.util.ConditionsMapper.ValueConverter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
public class ConditionsMapperTest {
	private DateTimeZone defaultZone;

	@Before
	public void setUp() throws Exception {
		this.defaultZone = DateTimeZone.getDefault();
		DateTimeZone.setDefault(DateTimeZone.forOffsetHours(1));
	}

	@After
	public void tearDown() throws Exception {
		DateTimeZone.setDefault(this.defaultZone);
	}

	@Test
	public void testWrongValues() throws Exception {
		testWrongValues("", "columnName", String.class);
		testWrongValues(null, "columnName", String.class);
		testWrongValues("filedName", "", String.class);
		testWrongValues("filedName", null, String.class);
		testWrongValues("filedName", "columnName", null);

		testWrongValues("", "columnName", String.class, RangePolicy.NULLS_FIRST);
		testWrongValues(null, "columnName", String.class, RangePolicy.NULLS_FIRST);
		testWrongValues("filedName", "", String.class, RangePolicy.NULLS_FIRST);
		testWrongValues("filedName", null, String.class, RangePolicy.NULLS_FIRST);
		testWrongValues("filedName", "columnName", null, RangePolicy.NULLS_FIRST);
		testWrongValues("filedName", "columnName", String.class, (RangePolicy) null);

		testWrongValues("", "columnName", String.class, new StringConverter());
		testWrongValues(null, "columnName", String.class, new StringConverter());
		testWrongValues("filedName", "", String.class, new StringConverter());
		testWrongValues("filedName", null, String.class, new StringConverter());
		testWrongValues("filedName", "columnName", null, new StringConverter());
		testWrongValues("filedName", "columnName", String.class, (ValueConverter<?>) null);

		testWrongValues("", "columnName", String.class, RangePolicy.NULLS_FIRST, new StringConverter());
		testWrongValues(null, "columnName", String.class, RangePolicy.NULLS_FIRST, new StringConverter());
		testWrongValues("filedName", "", String.class, RangePolicy.NULLS_FIRST, new StringConverter());
		testWrongValues("filedName", null, String.class, RangePolicy.NULLS_FIRST, new StringConverter());
		testWrongValues("filedName", "columnName", null, RangePolicy.NULLS_FIRST, new StringConverter());
		testWrongValues("filedName", "columnName", String.class, null, new StringConverter());
		testWrongValues("filedName", "columnName", String.class, RangePolicy.NULLS_FIRST, null);
	}

	private void testWrongValues(final String fieldName, final String columnName, final Class<?> type) {
		try {
			new ConditionsMapper().map(fieldName, columnName, type);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}

	private void testWrongValues(final String fieldName, final String columnName, final Class<?> type,
			final RangePolicy rangePolicy) {
		try {
			new ConditionsMapper().map(fieldName, columnName, type, rangePolicy);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}

	private void testWrongValues(final String fieldName, final String columnName, final Class<?> type,
			final ValueConverter<?> converter) {
		try {
			new ConditionsMapper().map(fieldName, columnName, type, converter);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}

	private void testWrongValues(final String fieldName, final String columnName, final Class<?> type,
			final RangePolicy rangePolicy, final ValueConverter<?> converter) {
		try {
			new ConditionsMapper().map(fieldName, columnName, type, rangePolicy, converter);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}

	@Test
	public void testMappingWithConverter() throws Exception {
		final ConditionsMapper mapper = new ConditionsMapper().map("field", "column", Float.class,
				new ValueConverter<Float>() {
					@Override
					public Float prepareConvert(final String value) throws IllegalArgumentException {
						assertEquals("2.5", value);
						return Float.valueOf(value);
					}

					@Override
					public String handleConvert(final Float value) throws IllegalArgumentException {
						assertEquals(Float.valueOf(3.5f), value);
						return String.valueOf(value);
					}
				});

		assertMapping(mapper, "field", "column", Float.class, RangePolicy.IGNORE_NULLS, "2.5", 2.5f, 3.5f, "3.5");

		assertFalse(mapper.hasColumn("test"));
		assertNull(mapper.getColumn("test"));
	}

	@Test
	public void testMapping() throws Exception {
		final ConditionsMapper mapper = new ConditionsMapper()//
				.map("field1", "column1", Integer.class)//
				.map("field2", "column2", Long.class)//
				.map("field3", "column3", Timestamp.class)//
				.map("field4", "column4", String.class, RangePolicy.NULLS_FIRST)//
				.map("field5", "column5", Double.class, RangePolicy.NULLS_LAST);

		assertMapping(mapper, "field1", "column1", Integer.class, RangePolicy.IGNORE_NULLS, "2", 2, 22, "22");
		assertMapping(mapper, "field2", "column2", Long.class, RangePolicy.IGNORE_NULLS, "3", 3L, 33L, "33");
		assertMapping(mapper, "field3", "column3", Timestamp.class, RangePolicy.IGNORE_NULLS,//
				"2015-04-27T17:34:00+01:00", new Timestamp(DateTime.parse("2015-04-27T17:34:00+01:00").getMillis()),//
				new Timestamp(DateTime.parse("2015-07-06T15:06:00+01:00").getMillis()), "2015-07-06T15:06:00+01:00");
		assertMapping(mapper, "field4", "column4", String.class, RangePolicy.NULLS_FIRST, "aaa", "aaa", "bbb", "bbb");
		assertMapping(mapper, "field5", "column5", Double.class, RangePolicy.NULLS_LAST, "aaa", "aaa", "bbb", "bbb");

		assertFalse(mapper.hasColumn("test"));
		assertNull(mapper.getColumn("test"));
	}

	private void assertMapping(final ConditionsMapper mapper, final String fieldName, final String expectedColumnName,
			final Class<?> expectedColumnType, final RangePolicy expectedRangePolicy, final String prepareValue,
			final Object expectedPrepareValue, final Object handleValue, final String expectedHandleValue) {
		assertTrue(mapper.hasColumn(fieldName));
		assertEquals(fieldName, mapper.getColumn(fieldName).field());
		assertEquals(expectedColumnName, mapper.getColumn(fieldName).name());
		assertEquals(expectedColumnType, mapper.getColumn(fieldName).type());
		assertEquals(expectedRangePolicy, mapper.getColumn(fieldName).rangePolicy());
		assertEquals(expectedPrepareValue, mapper.getColumn(fieldName).prepareParse(prepareValue));
		assertEquals(expectedHandleValue, mapper.getColumn(fieldName).handleParse(handleValue));
	}
}
