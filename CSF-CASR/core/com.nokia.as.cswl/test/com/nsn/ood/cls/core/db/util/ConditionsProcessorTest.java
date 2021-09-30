/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.util;

import static com.nsn.ood.cls.core.condition.ConditionsTestUtil.conditions;
import static com.nsn.ood.cls.core.condition.ConditionsTestUtil.filter;
import static com.nsn.ood.cls.core.condition.ConditionsTestUtil.pagination;
import static com.nsn.ood.cls.core.condition.ConditionsTestUtil.sorting;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.Field;
import com.nsn.ood.cls.core.condition.Field.Order;
import com.nsn.ood.cls.core.condition.Filter;
import com.nsn.ood.cls.core.condition.Filter.Type;
import com.nsn.ood.cls.core.condition.Pagination;
import com.nsn.ood.cls.core.condition.Sorting;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
public class ConditionsProcessorTest {
	private static final Timestamp TIMESTAMP = new Timestamp(new DateTime(2015, 4, 24, 17, 22,
			DateTimeZone.forOffsetHours(1)).getMillis());

	@Test
	public void testWhere() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final FiltersParser filtersParserMock = createMock(FiltersParser.class);
		final ConditionsMapper mapperMock = createMock(ConditionsMapper.class);

		final Conditions conditions = conditions(false);
		conditions.addFilter(filter(Type.EQUAL, "name1", "value1"));
		conditions.addFilter(filter(Type.WILDCARD, "name2", "value2"));

		expect(filtersParserMock.sql()).andReturn("sql");
		expect(filtersParserMock.values()).andReturn(Arrays.<Object> asList());

		replayAll();
		final ConditionsProcessor processor = new ConditionsProcessor(conditions, mapperMock) {
			@Override
			protected FiltersParser createFiltersParser(final List<Filter> filters, final ConditionsMapper mapper)
					throws ConditionProcessingException {
				super.createFiltersParser(Collections.<Filter> emptyList(), null);
				assertEquals(conditions.filters(), filters);
				assertEquals(mapperMock, mapper);
				return filtersParserMock;
			}
		};
		assertEquals("sql", processor.sql());
		processor.prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testWhereWithException() throws Exception {
		final ConditionProcessingException conditionProcessingExceptionMock = createMock(ConditionProcessingException.class);
		final Conditions conditions = conditions(false);
		conditions.addFilter(filter(Type.EQUAL, "name", "value"));

		replayAll();
		try {
			new ConditionsProcessor(conditions, new ConditionsMapper()) {
				@Override
				protected FiltersParser createFiltersParser(final List<Filter> filters, final ConditionsMapper mapper)
						throws ConditionProcessingException {
					throw conditionProcessingExceptionMock;
				}
			};
			fail();
		} catch (final ConditionProcessingException e) {
			assertEquals(conditionProcessingExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		statementMock.setInt(1, 2);
		statementMock.setLong(2, 34L);
		statementMock.setTimestamp(3, TIMESTAMP);
		statementMock.setString(4, "test");

		replayAll();
		final ConditionsProcessor processor = new ConditionsProcessor(conditions(false), new ConditionsMapper());
		setInternalState(processor, Arrays.<Object> asList(2, 34L, TIMESTAMP, "test"));
		assertEquals("", processor.sql());
		processor.prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testOrderBy() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final SortingParser sortingParserMock = createMock(SortingParser.class);
		final ConditionsMapper mapperMock = createMock(ConditionsMapper.class);

		final Sorting sorting = sorting();
		sorting.addField("name", Order.ASC);
		final Conditions conditions = conditions(false);
		conditions.setSorting(sorting);

		expect(sortingParserMock.sql()).andReturn("sql");

		replayAll();
		final ConditionsProcessor processor = new ConditionsProcessor(conditions, mapperMock) {
			@Override
			protected SortingParser createSortingParser(final List<Field> fields, final ConditionsMapper mapper) {
				super.createSortingParser(Collections.<Field> emptyList(), null);
				assertEquals(sorting.fields(), fields);
				assertEquals(mapperMock, mapper);
				return sortingParserMock;
			}
		};
		assertEquals("sql", processor.sql());
		processor.prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testLimitAndOffset() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final PaginationParser paginationParserMock = createMock(PaginationParser.class);

		final Conditions conditions = conditions(false);
		conditions.setPaginationLimit(10);
		conditions.setPaginationOffset(20);

		expect(paginationParserMock.sql()).andReturn("sql");

		replayAll();
		final ConditionsProcessor processor = new ConditionsProcessor(conditions, new ConditionsMapper()) {
			@Override
			protected PaginationParser createPaginationParser(final Pagination pagination) {
				super.createPaginationParser(pagination(0, 0));
				assertEquals(conditions.pagination(), pagination);
				return paginationParserMock;
			}
		};
		assertEquals("sql", processor.sql());
		processor.prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testEmptyConditions() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		replayAll();
		final ConditionsProcessor processor = new ConditionsProcessor(conditions(false), new ConditionsMapper());
		assertEquals("", processor.sql());
		processor.prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testNoConditions() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		replayAll();
		final ConditionsProcessor processor = new ConditionsProcessor(null, new ConditionsMapper());
		assertEquals("", processor.sql());
		processor.prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testNoMapper() throws Exception {
		try {
			new ConditionsProcessor(null, null);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}
}
