/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.condition.Filter;
import com.nsn.ood.cls.core.db.SizeQuery.QueryPrepare;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.core.db.util.ConditionsProcessor;
import com.nsn.ood.cls.model.gen.metadata.MetaData;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
public abstract class ConditionsQuery<T> implements Query {
	private final String sql;
	private final ConditionsProcessor conditionsProcessor;
	private final SizeQuery queryTotalSize;
	private final SizeQuery queryFilteredSize;

	public abstract List<T> getList();

	protected ConditionsQuery(final String sql, final Conditions conditions, final ConditionsMapper mapper,
			final String idFilterName) throws ConditionProcessingException {
		if (conditions == null) {
			throw new CLSIllegalArgumentException("Conditions must not be null");
		}

		this.sql = sql;
		this.conditionsProcessor = createConditionsProcessor(conditions, mapper);

		if (conditions.skipMetaData()) {
			this.queryTotalSize = null;
			this.queryFilteredSize = null;
		} else {
			final Conditions totalSizeConditions = createConditions();
			final Conditions filteredSizeConditions = createConditions();
			fillFilters(conditions.filters(), idFilterName, totalSizeConditions, filteredSizeConditions);

			this.queryTotalSize = createQuerySize(totalSizeConditions, mapper, null);
			this.queryFilteredSize = totalSizeConditions.equals(filteredSizeConditions) ? null : createQuerySize(
					filteredSizeConditions, mapper, this.queryTotalSize);
		}
	}

	private void fillFilters(final List<Filter> filters, final String idFilterName,
			final Conditions totalSizeConditions, final Conditions filteredSizeConditions) {
		for (final Filter filter : filters) {
			if (filter.name().equals(idFilterName)) {
				totalSizeConditions.addFilter(filter);
			}
			filteredSizeConditions.addFilter(filter);
		}
	}

	private SizeQuery createQuerySize(final Conditions conditions, final ConditionsMapper mapper, final SizeQuery next)
			throws ConditionProcessingException {
		final ConditionsProcessor cp = createConditionsProcessor(conditions, mapper);
		return new SizeQuery(this.sql + cp.sql(), new QueryPrepareImpl(cp), next);
	}

	private ConditionsProcessor createConditionsProcessor(final Conditions conditions, final ConditionsMapper mapper)
			throws ConditionProcessingException {
		return new ConditionsProcessor(conditions, mapper);
	}

	private Conditions createConditions() {
		return ConditionsBuilder.createAndSkipMetaData().build();
	}

	@Override
	public String sql() {
		return this.sql + this.conditionsProcessor.sql();
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		this.conditionsProcessor.prepare(statement);
	}

	@Override
	public Query next() {
		return this.queryFilteredSize == null ? this.queryTotalSize : this.queryFilteredSize;
	}

	public MetaData getMetaData() {
		if (this.queryTotalSize == null) {
			return null;
		}

		final long total = this.queryTotalSize.getSize();
		return new MetaData()//
				.withTotal(total)//
				.withFiltered(this.queryFilteredSize == null ? total : this.queryFilteredSize.getSize());
	}

	private static final class QueryPrepareImpl implements QueryPrepare {
		private final ConditionsProcessor conditionsProcessor;

		public QueryPrepareImpl(final ConditionsProcessor conditionsProcessor) {
			this.conditionsProcessor = conditionsProcessor;
		}

		@Override
		public void prepare(final PreparedStatement statement) throws SQLException {
			this.conditionsProcessor.prepare(statement);
		}
	}
}
