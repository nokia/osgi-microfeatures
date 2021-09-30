/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
import static com.nsn.ood.cls.model.test.ViolationErrorTestUtil.violationError;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.easymock.Capture;
import org.junit.Test;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.db.ConditionsQuery;
import com.nsn.ood.cls.core.db.DistinctQuery;
import com.nsn.ood.cls.core.db.DistinctQuery.QueryPrepare;
import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.Query;
import com.nsn.ood.cls.core.db.QueryExecutor;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.core.service.error.UnknownRuntimeErrorException;
import com.nsn.ood.cls.model.gen.metadata.MetaData;
import com.nsn.ood.cls.model.metadata.MetaDataList;


/**
 * @author marynows
 *
 */
public class AbstractRetrieveOperationTest {
	private static final List<String> LIST = Arrays.asList("test1", "test2");
	private static final MetaData META_DATA = metaData(20L, 10L);

	@Test
	public void testGetList() throws Exception {
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final Conditions conditionsMock = createMock(Conditions.class);
		final ConditionsQuery<String> conditionsQueryMock = createConditionsQueryMock();

		queryExecutorMock.execute(conditionsQueryMock);
		expect(conditionsQueryMock.getList()).andReturn(LIST);
		expect(conditionsQueryMock.getMetaData()).andReturn(META_DATA);

		replayAll();
		final AbstractRetrieveOperation<String, ConditionsQuery<String>> operation = new TestListAbstractRetrieveOperation() {
			@Override
			protected ConditionsQuery<String> createQuery(final Conditions conditions)
					throws ConditionProcessingException {
				assertEquals(conditionsMock, conditions);
				return conditionsQueryMock;
			}
			
			protected void executeQuery(final Query query) {
				try {
					queryExecutorMock.execute(query);
				} catch (final SQLException e) {
					// throw new RetrieveException(LogMessage.QUERY_FAIL, e);
					throw new UnknownRuntimeErrorException(LogMessage.QUERY_FAIL, e);
				}
			}
		};
		setInternalState(operation, queryExecutorMock);
		assertEquals(new MetaDataList<>(LIST, META_DATA), operation.getList(conditionsMock));
		verifyAll();
	}

	@Test
	public void testGetListWithSQLException() throws Exception {
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final Conditions conditionsMock = createMock(Conditions.class);
		final ConditionsQuery<String> conditionsQueryMock = createConditionsQueryMock();

		queryExecutorMock.execute(conditionsQueryMock);
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		final AbstractRetrieveOperation<String, ConditionsQuery<String>> operation = new TestListAbstractRetrieveOperation() {
			@Override
			protected ConditionsQuery<String> createQuery(final Conditions conditions)
					throws ConditionProcessingException {
				assertEquals(conditionsMock, conditions);
				return conditionsQueryMock;
			}
			
			protected void executeQuery(final Query query) {
				try {
					queryExecutorMock.execute(query);
				} catch (final SQLException e) {
					// throw new RetrieveException(LogMessage.QUERY_FAIL, e);
					throw new UnknownRuntimeErrorException(LogMessage.QUERY_FAIL, e);
				}
			}
		};
		setInternalState(operation, queryExecutorMock);
		try {
			operation.getList(conditionsMock);
			fail();
		} catch (final UnknownRuntimeErrorException e) {
			assertEquals("Cannot execute DB query", e.getMessage());
			assertTrue(e.getCause() instanceof SQLException);
		} finally {
			verifyAll();
		}
	}

	@Test
	public void testGetListWithConditionProcessingException() throws Exception {
		final Conditions conditionsMock = createMock(Conditions.class);
		final ConditionProcessingException conditionProcessingExceptionMock = createMock(
				ConditionProcessingException.class);

		expect(conditionProcessingExceptionMock.getMessage()).andReturn("message");
		expect(conditionProcessingExceptionMock.getError()).andReturn(violationError("m"));

		replayAll();
		try {
			final AbstractRetrieveOperation<String, ConditionsQuery<String>> operation = new TestListAbstractRetrieveOperation() {
				@Override
				protected ConditionsQuery<String> createQuery(final Conditions conditions)
						throws ConditionProcessingException {
					assertEquals(conditionsMock, conditions);
					throw conditionProcessingExceptionMock;
				}
				
				protected void executeQuery(final Query query) { }
			};
			operation.getList(conditionsMock);
			fail();
		} catch (final RetrieveException e) {
			assertEquals(conditionProcessingExceptionMock, e.getCause());
			assertEquals("message", e.getMessage());
			assertEquals(violationError("m"), e.getError());
		}
		verifyAll();
	}

	@Test
	public void testGetFilterValues() throws Exception {
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final Conditions conditionsMock = createMock(Conditions.class);
		final ConditionsQuery<String> conditionsQueryMock = createConditionsQueryMock();
		final DistinctQuery distinctQueryMock = createMock(DistinctQuery.class);
		final ConditionsMapper conditionsMapper = new ConditionsMapper().map("filterName", "columnName", String.class);
		final Capture<QueryPrepare> capturedQueryPrepare = new Capture<>();
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		expect(conditionsQueryMock.sql()).andReturn("sql");
		queryExecutorMock.execute(distinctQueryMock);
		expect(distinctQueryMock.getValues()).andReturn(Arrays.<Object> asList("one", "two"));

		conditionsQueryMock.prepare(statementMock);

		replayAll();
		final AbstractRetrieveOperation<String, ConditionsQuery<String>> operation = new TestListAbstractRetrieveOperation() {
			@Override
			protected ConditionsQuery<String> createQuery(final Conditions conditions)
					throws ConditionProcessingException {
				assertEquals(conditionsMock, conditions);
				return conditionsQueryMock;
			}

			@Override
			protected ConditionsMapper getMapper() {
				return conditionsMapper;
			}

			@Override
			protected DistinctQuery createDistinctQuery(final String sql, final QueryPrepare queryPrepare,
					final String columnName, final Class<?> columnType) {
				super.createDistinctQuery(sql, queryPrepare, columnName, columnType);
				assertEquals("sql", sql);
				capturedQueryPrepare.setValue(queryPrepare);
				assertEquals("columnName", columnName);
				assertEquals(String.class, columnType);
				return distinctQueryMock;
			}
			
			protected void executeQuery(final Query query) {
				try {
					queryExecutorMock.execute(query);
				} catch (final SQLException e) {
					// throw new RetrieveException(LogMessage.QUERY_FAIL, e);
					throw new UnknownRuntimeErrorException(LogMessage.QUERY_FAIL, e);
				}
			}
		};
		setInternalState(operation, queryExecutorMock);
		assertEquals(Arrays.asList("one", "two"), operation.getFilterValues("filterName", conditionsMock));
		capturedQueryPrepare.getValue().prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testGetFilterValuesWithNoSuchFilter() throws Exception {
		final ConditionsMapper conditionsMapper = new ConditionsMapper();

		replayAll();
		try {
			final AbstractRetrieveOperation<String, ConditionsQuery<String>> operation = new TestListAbstractRetrieveOperation() {
				@Override
				protected ConditionsQuery<String> createQuery(final Conditions conditions)
						throws ConditionProcessingException {
					return null;
				}

				@Override
				protected ConditionsMapper getMapper() {
					return conditionsMapper;
				}
				
				protected void executeQuery(final Query query) { }
			};
			operation.getFilterValues("filterName", null);
			fail();
		} catch (final RetrieveException e) {
			assertNull(e.getCause());
			assertEquals("Invalid filter name", e.getMessage());
			assertEquals(violationError("Invalid filter name", null, "filterName"), e.getError());
		}
		verifyAll();
	}

	@SuppressWarnings("unchecked")
	private ConditionsQuery<String> createConditionsQueryMock() {
		return createMock(ConditionsQuery.class);
	}

	private static abstract class TestListAbstractRetrieveOperation
			extends AbstractRetrieveOperation<String, ConditionsQuery<String>> {

		@Override
		protected ConditionsMapper getMapper() {
			return null;
		}
	}
}
