/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.sql.SQLException;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.Query;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.creator.FeatureCreator;
import com.nsn.ood.cls.core.db.feature.QueryFeatures;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.core.service.error.UnknownRuntimeErrorException;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Loggable
@Component(provides = FeatureRetrieveOperation.class)
public class FeatureRetrieveOperation extends AbstractRetrieveOperation<Feature, QueryFeatures> {
	@ServiceDependency
	private FeatureCreator creator;
	@ServiceDependency(filter = "(name=feature)")
	private ConditionsMapper mapper;
	
	@ServiceDependency(filter = "(name=query)") 
	private StatementExecutor<Query> queryExecutor;
	
	protected void executeQuery(final Query query) {
		try {
			this.queryExecutor.execute(query);
		} catch (final SQLException e) {
			// throw new RetrieveException(LogMessage.QUERY_FAIL, e);
			throw new UnknownRuntimeErrorException(LogMessage.QUERY_FAIL, e);
		}
	}

	@Override
	protected QueryFeatures createQuery(final Conditions conditions) throws ConditionProcessingException {
		return new QueryFeatures(conditions, this.mapper, this.creator);
	}

	@Override
	protected ConditionsMapper getMapper() {
		return this.mapper;
	}
}
