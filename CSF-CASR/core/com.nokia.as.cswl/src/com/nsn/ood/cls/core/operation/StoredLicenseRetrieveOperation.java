/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.sql.SQLException;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.Query;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.creator.LicenseCreator;
import com.nsn.ood.cls.core.db.license.QueryStoredLicenses;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.core.service.error.UnknownRuntimeErrorException;
import com.nsn.ood.cls.model.internal.StoredLicense;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = StoredLicenseRetrieveOperation.class)
@Loggable
public class StoredLicenseRetrieveOperation extends AbstractRetrieveOperation<StoredLicense, QueryStoredLicenses> {
	@ServiceDependency
	private LicenseCreator creator;
	@ServiceDependency(filter = "(name=storedLicense)")
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
	protected QueryStoredLicenses createQuery(final Conditions conditions) throws ConditionProcessingException {
		return new QueryStoredLicenses(conditions, this.mapper, this.creator);
	}

	@Override
	protected ConditionsMapper getMapper() {
		return this.mapper;
	}
}
