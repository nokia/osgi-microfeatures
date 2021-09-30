package com.nsn.ood.cls.core.operation;

import java.sql.SQLException;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.Query;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.creator.DBLicenseCreator;
import com.nsn.ood.cls.core.db.license.QueryDBLicenses;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.core.service.error.UnknownRuntimeErrorException;
import com.nsn.ood.cls.model.gen.licenses.DBLicense;

@Component(provides = DBLicenseRetrieveOperation.class)
public class DBLicenseRetrieveOperation extends AbstractRetrieveOperation<DBLicense, QueryDBLicenses> {
    @ServiceDependency
    private DBLicenseCreator creator;
    @ServiceDependency(filter = "(name=license)")
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
    protected QueryDBLicenses createQuery(final Conditions conditions) throws ConditionProcessingException {
        return new QueryDBLicenses(conditions, this.mapper, this.creator);
    }

    @Override
    protected ConditionsMapper getMapper() {
        return this.mapper;
    }
}
