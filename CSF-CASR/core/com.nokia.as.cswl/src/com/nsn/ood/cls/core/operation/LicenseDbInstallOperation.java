/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.Query;
import com.nsn.ood.cls.core.db.SimpleQuery;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.Update;
import com.nsn.ood.cls.core.db.feature.InsertFeature;
import com.nsn.ood.cls.core.db.feature.QueryFeatureExist;
import com.nsn.ood.cls.core.db.feature.UpdateFeatureCapacity;
import com.nsn.ood.cls.core.db.license.InsertLicense;
import com.nsn.ood.cls.core.db.license.QueryLicenseExist;
import com.nsn.ood.cls.core.model.LicenseState;
import com.nsn.ood.cls.core.operation.exception.InstallException;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = LicenseDbInstallOperation.class)
@Loggable
public class LicenseDbInstallOperation {
	private static final Logger LOG = LoggerFactory.getLogger(LicenseDbInstallOperation.class);

	@ServiceDependency(filter = "(name=query)")
	private StatementExecutor<Query> queryExecutor;
	@ServiceDependency(filter = "(name=update)")
	private StatementExecutor<Update> updateExecutor;
	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DatetimeConverter;
	@ServiceDependency(filter = "(&(from=licenseState)(to=string))")
	private Converter<LicenseState, String> licenseState2StringConverter;
	@ServiceDependency(filter = "(&(from=licenseMode)(to=integer))")
	private Converter<License.Mode, Integer> licenseMode2IntegerConverter;
	@ServiceDependency(filter = "(&(from=licenseType)(to=integer))")
	private Converter<License.Type, Integer> licenseType2IntegerConverter;

	public void install(final License license) throws InstallException {
		final boolean featureExist = isFeatureExist(license);
		final boolean licenseExist = isLicenseExist(license);
		if (!featureExist) {
			addNewFeature(license);
		}
		if (!licenseExist) {
			addNewLicense(license);
		}
		if (!featureExist || !licenseExist) {
			updateFeatureCapacity(license);
		}
	}

	private boolean isFeatureExist(final License license) throws InstallException {
		return query(new QueryFeatureExist(license.getFeatures().get(0).getFeatureCode()));
	}

	private boolean isLicenseExist(final License license) throws InstallException {
		return query(new QueryLicenseExist(license.getSerialNumber()));
	}

	private boolean query(final SimpleQuery<Boolean> query) throws InstallException {
		try {
			this.queryExecutor.execute(query);
		} catch (final SQLException e) {
			LOG.error(LogMessage.QUERY_FAIL, e);
			throw new InstallException(e);
		}
		return query.getValue();
	}

	private void addNewFeature(final License license) throws InstallException {
		update(new InsertFeature(license));
	}

	private void addNewLicense(final License license) throws InstallException {
		update(new InsertLicense(license, timestamp2DatetimeConverter, licenseMode2IntegerConverter, licenseType2IntegerConverter, licenseState2StringConverter));
	}

	private void updateFeatureCapacity(final License license) throws InstallException {
		update(new UpdateFeatureCapacity(license.getFeatures().get(0).getFeatureCode()));
	}

	private void update(final Update update) throws InstallException {
		try {
			this.updateExecutor.execute(update);
		} catch (final SQLException e) {
			LOG.error(LogMessage.UPDATE_FAIL, e);
			throw new InstallException(e);
		}
	}
}
