/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.Query;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.Update;
import com.nsn.ood.cls.core.db.creator.LicensedFeatureCreator;
import com.nsn.ood.cls.core.db.feature.QueryLicensedFeaturesForCapacityCheck;
import com.nsn.ood.cls.core.db.feature.UpdateLicensedFeaturesForCapacityCheck;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.model.internal.LicensedFeature;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = FeatureCapacityCheckOperation.class)
@Loggable
public class FeatureCapacityCheckOperation {
	private static final Logger LOG = LoggerFactory.getLogger(FeatureCapacityCheckOperation.class);

	@ServiceDependency(filter = "(name=update)")
	private StatementExecutor<Update> updateExecutor;
	@ServiceDependency(filter = "(name=query)")
	private StatementExecutor<Query> queryExecutor;
	@ServiceDependency
	private LicensedFeatureCreator licensedFeatureCreator;
	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter;

	public List<LicensedFeature> retrieve(final long threshold, final DateTime checkTime) {
		final QueryLicensedFeaturesForCapacityCheck query = new QueryLicensedFeaturesForCapacityCheck(threshold,
				checkTime.withTimeAtStartOfDay(), this.licensedFeatureCreator, timestamp2DateTimeConverter);
		try {
			this.queryExecutor.execute(query);
		} catch (final SQLException e) {
			LOG.error(LogMessage.QUERY_FAIL, e);
		}
		return query.getList();
	}

	public void update(final List<LicensedFeature> features, final DateTime checkTime) throws UpdateException {
		final UpdateLicensedFeaturesForCapacityCheck update = new UpdateLicensedFeaturesForCapacityCheck(features,
				checkTime, timestamp2DateTimeConverter);
		try {
			this.updateExecutor.execute(update);
		} catch (final SQLException e) {
			LOG.error(LogMessage.UPDATE_FAIL, e);
			throw new UpdateException(e, update.getIndex());
		}
	}
}
