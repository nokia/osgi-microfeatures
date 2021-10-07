/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.Query;
import com.nsn.ood.cls.core.db.SimpleDistinctQuery;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.license.QueryLicensesFeatureCodesForStateUpdate;
import com.nsn.ood.cls.core.db.reservation.LockReservations;
import com.nsn.ood.cls.core.db.reservation.QueryReservationsFeatureCodesForClient;
import com.nsn.ood.cls.core.db.reservation.QueryReservationsFeatureCodesForExpiredClients;
import com.nsn.ood.cls.core.db.reservation.QueryReservationsFeatureCodesForExpiredLicenses;
import com.nsn.ood.cls.model.CLSConst;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSException;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component(provides = FeatureLockOperation.class)
@Loggable
public class FeatureLockOperation {
	private static final Logger LOG = LoggerFactory.getLogger(FeatureLockOperation.class);

	@ServiceDependency(filter = "(name=query)")
	private StatementExecutor<Query> queryExecutor;
	@ServiceDependency(filter = "(&(from=client)(to=string))")
	private Converter<Client, String> client2StringConverter;
	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	@ServiceDependency(filter = "(&(from=licenseType)(to=integer))")
	private Converter<License.Type, Integer> licenseType2IntegerConverter;

	public void lock(final Long featureCode) throws LockException {
		lock(Arrays.asList(featureCode));
	}

	public List<Long> lock(final List<Long> featureCodes) throws LockException {
		LOG.debug("Locking features: {}", featureCodes.toString());
		if (!featureCodes.isEmpty()) {
			try {
				this.queryExecutor.execute(new LockReservations(featureCodes));
			} catch (final SQLException e) {
				LOG.error(LogMessage.QUERY_FAIL, e);
				throw new LockException(e);
			}
		}
		return featureCodes;
	}

	public List<Long> lockForClient(final Client client) throws LockException {
		LOG.debug("Getting features for client: {}", client2StringConverter.convertTo(client));
		return lock(getFeatureCodes(new QueryReservationsFeatureCodesForClient(client)));
	}

	public List<Long> lockForExpiredClients(final DateTime expires) throws LockException {
		LOG.debug("Getting features for expiring clients to date: {}", expires.toString(CLSConst.DATE_TIME_FORMAT));
		return lock(getFeatureCodes(new QueryReservationsFeatureCodesForExpiredClients(expires, timestamp2DateTimeConverter, licenseType2IntegerConverter)));
	}

	public List<Long> lockForExpiredLicenses(final DateTime endDate) throws LockException {
		LOG.debug("Getting features for expiring licenses to date: {}", endDate.toString(CLSConst.DATE_TIME_FORMAT));
		return lock(getFeatureCodes(new QueryReservationsFeatureCodesForExpiredLicenses(endDate, timestamp2DateTimeConverter)));
	}

	public List<Long> lockForLicensesState(final DateTime date) throws LockException {
		LOG.debug("Getting features for updating licenses state for date: {}",
				date.toString(CLSConst.DATE_TIME_FORMAT));
		return lock(getFeatureCodes(new QueryLicensesFeatureCodesForStateUpdate(date, timestamp2DateTimeConverter)));
	}

	private List<Long> getFeatureCodes(final SimpleDistinctQuery<Long> query) throws LockException {
		try {
			this.queryExecutor.execute(query);
			return query.getValues();
		} catch (final SQLException e) {
			LOG.error(LogMessage.QUERY_FAIL, e);
			throw new LockException(e);
		}
	}

	public static final class LockException extends CLSException {
		private static final long serialVersionUID = -7557970928465867154L;

		private LockException(final Throwable cause) {
			super(cause.getMessage(), cause);
		}
	}
}
