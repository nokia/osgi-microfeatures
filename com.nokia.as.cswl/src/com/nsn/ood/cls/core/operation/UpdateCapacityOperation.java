/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.sql.SQLException;
import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.Update;
import com.nsn.ood.cls.core.db.feature.UpdateFeatureCapacity;
import com.nsn.ood.cls.core.db.license.UpdateLicensesCapacity;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component(provides = UpdateCapacityOperation.class)
@Loggable
public class UpdateCapacityOperation {
	private static final Logger LOG = LoggerFactory.getLogger(UpdateCapacityOperation.class);

	@ServiceDependency(filter = "(name=update)")
	private StatementExecutor<Update> updateExecutor;

	public void updateCapacity(final List<Long> featureCodes) throws UpdateException {
		try {
			this.updateExecutor.execute(new UpdateFeatureCapacity(featureCodes));
			this.updateExecutor.execute(new UpdateLicensesCapacity(featureCodes));
		} catch (final SQLException e) {
			LOG.error(LogMessage.UPDATE_FAIL, e);
			throw new UpdateException(e);
		}
	}
}
