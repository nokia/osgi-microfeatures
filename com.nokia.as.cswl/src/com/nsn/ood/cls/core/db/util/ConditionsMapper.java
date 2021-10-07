/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.util;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

import com.nsn.ood.cls.core.db.util.ConditionsMapper.ValueConverter;
import com.nsn.ood.cls.model.CLSConst;
import com.nsn.ood.cls.util.Strings;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 *
 */
public class ConditionsMapper {
	private static final StringConverter STRING_CONVERTER = new StringConverter();
	private static final IntegerConverter INTEGER_CONVERTER = new IntegerConverter();
	private static final LongConverter LONG_CONVERTER = new LongConverter();
	private static final TimestampConverter TIMESTAMP_CONVERTER = new TimestampConverter();

	private final Map<String, Column> mapping = new HashMap<>();

	public interface ValueConverter<T> {
		T prepareConvert(String value);

		String handleConvert(T value);
	}

	public enum RangePolicy {
		IGNORE_NULLS, NULLS_FIRST, NULLS_LAST
	}

	public static final class Column {
		private final String field;
		private final String name;
		private final Class<?> type;
		private final ValueConverter<?> converter;
		private final RangePolicy rangePolicy;

		private Column(final String field, final String name, final Class<?> type, final ValueConverter<?> converter,
				final RangePolicy rangePolicy) {
			this.field = field;
			this.name = name;
			this.type = type;
			this.converter = converter;
			this.rangePolicy = rangePolicy;
		}

		public String field() {
			return this.field;
		}

		public String name() {
			return this.name;
		}

		public Class<?> type() {
			return this.type;
		}

		public RangePolicy rangePolicy() {
			return this.rangePolicy;
		}

		public Object prepareParse(final String value) {
			return this.converter.prepareConvert(value);
		}

		@SuppressWarnings("unchecked")
		public String handleParse(final Object value) {
			return ((ValueConverter<Object>) this.converter).handleConvert(value);
		}
	}

	public ConditionsMapper map(final String fieldName, final String columnName, final Class<?> type,
			final ValueConverter<?> converter) {
		return map(fieldName, columnName, type, RangePolicy.IGNORE_NULLS, converter);
	}

	public ConditionsMapper map(final String fieldName, final String columnName, final Class<?> type,
			final RangePolicy rangePolicy, final ValueConverter<?> converter) {
		if (Strings.isNullOrEmpty(fieldName) || Strings.isNullOrEmpty(columnName) || (type == null)
				|| (rangePolicy == null) || (converter == null)) {
			throw new CLSIllegalArgumentException("Parameters must not be null or empty");
		}

		this.mapping.put(fieldName, new Column(fieldName, columnName, type, converter, rangePolicy));
		return this;
	}

	public ConditionsMapper map(final String fieldName, final String columnName, final Class<?> type) {
		return map(fieldName, columnName, type, RangePolicy.IGNORE_NULLS);
	}

	public ConditionsMapper map(final String fieldName, final String columnName, final Class<?> type,
			final RangePolicy rangePolicy) {
		if (Strings.isNullOrEmpty(fieldName) || Strings.isNullOrEmpty(columnName) || (type == null)
				|| (rangePolicy == null)) {
			throw new CLSIllegalArgumentException("Parameters must not be null or empty");
		}

		ValueConverter<?> converter;
		if (Timestamp.class.isAssignableFrom(type)) {
			converter = TIMESTAMP_CONVERTER;
		} else if (Long.class.isAssignableFrom(type)) {
			converter = LONG_CONVERTER;
		} else if (Integer.class.isAssignableFrom(type)) {
			converter = INTEGER_CONVERTER;
		} else {
			converter = STRING_CONVERTER;
		}
		this.mapping.put(fieldName, new Column(fieldName, columnName, type, converter, rangePolicy));
		return this;
	}

	public boolean hasColumn(final String fieldName) {
		return this.mapping.containsKey(fieldName);
	}

	public Column getColumn(final String fieldName) {
		return this.mapping.get(fieldName);
	}
}

class IntegerConverter implements ValueConverter<Integer> {
	@Override
	public Integer prepareConvert(final String value) {
		return Integer.valueOf(value);
	}

	@Override
	public String handleConvert(final Integer value) {
		return value.toString();
	}
}

class LongConverter implements ValueConverter<Long> {
	@Override
	public Long prepareConvert(final String value) {
		return Long.valueOf(value);
	}

	@Override
	public String handleConvert(final Long value) {
		return value.toString();
	}
}

class TimestampConverter implements ValueConverter<Timestamp> {
	@Override
	public Timestamp prepareConvert(final String value) {
		return new Timestamp(DateTime.parse(value).getMillis());
	}

	@Override
	public String handleConvert(final Timestamp value) {
		return new DateTime(value).toString(CLSConst.DATE_TIME_FORMAT);
	}
}

class StringConverter implements ValueConverter<String> {
	@Override
	public String prepareConvert(final String value) {
		return value;
	}

	@Override
	public String handleConvert(final String value) {
		return value;
	}
}
