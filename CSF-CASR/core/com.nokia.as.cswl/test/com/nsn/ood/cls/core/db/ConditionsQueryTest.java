/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createMockAndExpectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.easymock.Capture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.condition.Field.Order;
import com.nsn.ood.cls.core.db.SizeQuery.QueryPrepare;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.core.db.util.ConditionsProcessor;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	ConditionsQuery.class })
public class ConditionsQueryTest {
	private static final String SQL = "sql";

	@Test
	public void testCreateWithNullConditions() throws Exception {
		replayAll();
		try {
			new ConditionsQuery<String>(null, null, null, null) {
				@Override
				public void handle(final ResultSet resultSet) throws SQLException {
				}

				@Override
				public List<String> getList() {
					return null;
				}
			};
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
		verifyAll();
	}

	@Test
	public void testCreateWithSkipMetaData() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final Capture<Conditions> capturedConditions = new Capture<>();

		final Conditions conditions = ConditionsBuilder.createAndSkipMetaData().build();

		final ConditionsProcessor cpMock = mockCreateConditionsProcessor(capturedConditions);
		expect(cpMock.sql()).andReturn("where");
		cpMock.prepare(statementMock);

		replayAll();
		final ConditionsQuery<String> query = new ConditionsQuery<String>(SQL, conditions, null, null) {
			@Override
			public void handle(final ResultSet resultSet) throws SQLException {
			}

			@Override
			public List<String> getList() {
				return null;
			}
		};
		assertEquals(SQL + "where", query.sql());
		assertNull(query.next());
		assertNull(query.getMetaData());
		query.prepare(statementMock);
		verifyAll();

		assertEquals(conditions, capturedConditions.getValue());
	}

	@Test
	public void testCreateWithTwoFilters() throws Exception {
		final Capture<Conditions> capturedConditions = new Capture<>();
		final Capture<Conditions> capturedTotalConditions = new Capture<>();
		final Capture<Conditions> capturedFilteredConditions = new Capture<>();

		final Conditions conditions = ConditionsBuilder.create().equalFilter("name", "value").equalFilter("id", "ID")
				.sort("name", Order.ASC).build();
		testConditionsQuery(conditions, null, capturedConditions, capturedTotalConditions, capturedFilteredConditions);

		assertEquals(conditions, capturedConditions.getValue());
		assertEquals(ConditionsBuilder.createAndSkipMetaData().build(), capturedTotalConditions.getValue());
		assertEquals(ConditionsBuilder.createAndSkipMetaData().equalFilter("name", "value").equalFilter("id", "ID")
				.build(), capturedFilteredConditions.getValue());
	}

	@Test
	public void testCreateWithTwoFiltersAndFilterID() throws Exception {
		final Capture<Conditions> capturedConditions = new Capture<>();
		final Capture<Conditions> capturedTotalConditions = new Capture<>();
		final Capture<Conditions> capturedFilteredConditions = new Capture<>();

		final Conditions conditions = ConditionsBuilder.create().equalFilter("name", "value").equalFilter("id", "ID")
				.sort("name", Order.ASC).build();
		testConditionsQuery(conditions, "id", capturedConditions, capturedTotalConditions, capturedFilteredConditions);

		assertEquals(conditions, capturedConditions.getValue());
		assertEquals(ConditionsBuilder.createAndSkipMetaData().equalFilter("id", "ID").build(),
				capturedTotalConditions.getValue());
		assertEquals(ConditionsBuilder.createAndSkipMetaData().equalFilter("name", "value").equalFilter("id", "ID")
				.build(), capturedFilteredConditions.getValue());
	}

	@Test
	public void testCreateWithOneFilterAndFilterId() throws Exception {
		final Capture<Conditions> capturedConditions = new Capture<>();
		final Capture<Conditions> capturedTotalConditions = new Capture<>();

		final Conditions conditions = ConditionsBuilder.create().equalFilter("id", "ID").sort("name", Order.ASC)
				.build();
		testConditionsQuery(conditions, "id", capturedConditions, capturedTotalConditions, null);

		assertEquals(conditions, capturedConditions.getValue());
		assertEquals(ConditionsBuilder.createAndSkipMetaData().equalFilter("id", "ID").build(),
				capturedTotalConditions.getValue());
	}

	private void testConditionsQuery(final Conditions conditions, final String idFilterName,
			final Capture<Conditions> capturedConditions, final Capture<Conditions> capturedTotalConditions,
			final Capture<Conditions> capturedFilteredConditions) throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final Capture<QueryPrepare> capturedTotalQueryPrepare = new Capture<>();
		final Capture<QueryPrepare> capturedFilteredQueryPrepare = new Capture<>();

		final ConditionsProcessor cpMock = mockCreateConditionsProcessor(capturedConditions);
		expect(cpMock.sql()).andReturn("where");
		cpMock.prepare(statementMock);

		final ConditionsProcessor totalCPMock = mockCreateConditionsProcessor(capturedTotalConditions);
		final SizeQuery totalQSMock = mockCreateQuerySize(totalCPMock, "total", capturedTotalQueryPrepare, null);
		expect(totalQSMock.getSize()).andReturn(20L);
		totalCPMock.prepare(statementMock);

		SizeQuery filteredQSMock = null;
		if (capturedFilteredConditions != null) {
			final ConditionsProcessor filteredCPMock = mockCreateConditionsProcessor(capturedFilteredConditions);
			filteredQSMock = mockCreateQuerySize(filteredCPMock, "filtered", capturedFilteredQueryPrepare, totalQSMock);
			expect(filteredQSMock.getSize()).andReturn(10L);
			filteredCPMock.prepare(statementMock);
		}

		replayAll();
		final ConditionsQuery<String> query = new ConditionsQuery<String>(SQL, conditions, null, idFilterName) {
			@Override
			public void handle(final ResultSet resultSet) throws SQLException {
			}

			@Override
			public List<String> getList() {
				return null;
			}
		};
		assertEquals(SQL + "where", query.sql());
		if (capturedFilteredConditions != null) {
			assertEquals(filteredQSMock, query.next());
			assertEquals(metaData(20L, 10L), query.getMetaData());
		} else {
			assertEquals(totalQSMock, query.next());
			assertEquals(metaData(20L, 20L), query.getMetaData());
		}
		query.prepare(statementMock);
		capturedTotalQueryPrepare.getValue().prepare(statementMock);
		if (capturedFilteredConditions != null) {
			capturedFilteredQueryPrepare.getValue().prepare(statementMock);
		}
		verifyAll();
	}

	private SizeQuery mockCreateQuerySize(final ConditionsProcessor cpMock, final String sql,
			final Capture<QueryPrepare> queryPrepare, final SizeQuery next) throws Exception {
		expect(cpMock.sql()).andReturn(sql);
		return createMockAndExpectNew(SizeQuery.class, new Class<?>[] {
				String.class, QueryPrepare.class, Query.class }, eq(SQL + sql), capture(queryPrepare),
				next == null ? isNull() : eq(next));
	}

	private ConditionsProcessor mockCreateConditionsProcessor(final Capture<Conditions> conditions) throws Exception {
		return createMockAndExpectNew(ConditionsProcessor.class, new Class<?>[] {
				Conditions.class, ConditionsMapper.class }, conditions == null ? isNull() : capture(conditions),
				isNull(ConditionsMapper.class));
	}
}
