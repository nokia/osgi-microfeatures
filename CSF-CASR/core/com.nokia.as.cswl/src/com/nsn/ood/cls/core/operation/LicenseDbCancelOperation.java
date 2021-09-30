/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.sql.SQLException;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.Update;
import com.nsn.ood.cls.core.db.feature.DeleteFeature;
import com.nsn.ood.cls.core.db.feature.UpdateFeatureCapacity;
import com.nsn.ood.cls.core.db.license.DeleteLicense;
import com.nsn.ood.cls.core.operation.exception.CancelException;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = LicenseDbCancelOperation.class)
@Loggable
public class LicenseDbCancelOperation {
	private static final Logger LOG = LoggerFactory.getLogger(LicenseDbCancelOperation.class);

	@ServiceDependency(filter = "(name=update)")
	private StatementExecutor<Update> updateExecutor;

	public void cancel(final License license) throws CancelException {
		deleteLicense(license);
		updateFeatureCapacity(license);
		deleteFeature(license);
	}

	private void deleteLicense(final License license) throws CancelException {
		execute(new DeleteLicense(license.getSerialNumber()));
	}

	private void updateFeatureCapacity(final License license) throws CancelException {
		execute(new UpdateFeatureCapacity(license.getFeatures().get(0).getFeatureCode()));
	}

	private void deleteFeature(final License license) throws CancelException {
		execute(new DeleteFeature(license.getFeatures().get(0).getFeatureCode()));
	}

	private void execute(final Update update) throws CancelException {
		try {
			this.updateExecutor.execute(update);
		} catch (final SQLException e) {
			LOG.error(LogMessage.UPDATE_FAIL, e);
			throw new CancelException(e);
		}
	}
}
