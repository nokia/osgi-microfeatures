/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.db.ConditionsQuery;
import com.nsn.ood.cls.core.db.DistinctQuery;
import com.nsn.ood.cls.core.db.DistinctQuery.QueryPrepare;
import com.nsn.ood.cls.core.db.Query;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.core.db.util.ConditionsMapper.Column;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.model.metadata.MetaDataList;


/**
 * @author marynows
 *
 */
public abstract class AbstractRetrieveOperation<T, Q extends ConditionsQuery<T>> {

	protected abstract Q createQuery(final Conditions conditions) throws ConditionProcessingException;

	protected abstract ConditionsMapper getMapper();

	public MetaDataList<T> getList(final Conditions conditions) throws RetrieveException {
		final Q query = tryCreateQuery(conditions);
		executeQuery(query);
		return new MetaDataList<>(query.getList(), query.getMetaData());
	}

	public List<String> getFilterValues(final String filterName, final Conditions conditions) throws RetrieveException {
		final Column column = getColumn(filterName);

		final DistinctQuery query = createDistinctQuery(tryCreateQuery(conditions), column);
		executeQuery(query);
		return processFilterValues(query.getValues(), column);
	}

	private Column getColumn(final String filterName) throws RetrieveException {
		if (!getMapper().hasColumn(filterName)) {
			throw new RetrieveException("Invalid filter name", filterName);
		}
		return getMapper().getColumn(filterName);
	}

	private DistinctQuery createDistinctQuery(final Q query, final Column column) {
		return createDistinctQuery(query.sql(), new QueryPrepareImpl(query), column.name(), column.type());
	}

	protected DistinctQuery createDistinctQuery(final String sql, final QueryPrepare queryPrepare,
			final String columnName, final Class<?> columnType) {
		return new DistinctQuery(sql, queryPrepare, columnName, columnType);
	}

	private List<String> processFilterValues(final List<Object> values, final Column column) {
		final List<String> result = new ArrayList<>();
		for (final Object value : values) {
			result.add(column.handleParse(value));
		}
		return result;
	}

	private Q tryCreateQuery(final Conditions conditions) throws RetrieveException {
		try {
			return createQuery(conditions);
		} catch (final ConditionProcessingException e) {
			throw new RetrieveException(e);
		}
	}

	protected abstract void executeQuery(final Query query);
//	private void executeQuery(final Query query) {
//		try {
//			this.queryExecutor.execute(query);
//		} catch (final SQLException e) {
//			// throw new RetrieveException(LogMessage.QUERY_FAIL, e);
//			throw new UnknownRuntimeErrorException(LogMessage.QUERY_FAIL, e);
//		}
//	}

	private static final class QueryPrepareImpl implements QueryPrepare {
		private final Query query;

		public QueryPrepareImpl(final Query query) {
			this.query = query;
		}

		@Override
		public void prepare(final PreparedStatement statement) throws SQLException {
			this.query.prepare(statement);
		}
	}
}
