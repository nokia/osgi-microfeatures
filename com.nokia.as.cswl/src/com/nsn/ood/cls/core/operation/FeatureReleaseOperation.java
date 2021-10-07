/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.Query;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.Update;
import com.nsn.ood.cls.core.db.creator.ReservationCreator;
import com.nsn.ood.cls.core.db.reservation.DeleteReservations;
import com.nsn.ood.cls.core.db.reservation.DeleteReservationsByFeatureCode;
import com.nsn.ood.cls.core.db.reservation.DeleteReservationsByLicenseType;
import com.nsn.ood.cls.core.db.reservation.DeleteReservationsForExpiredClients;
import com.nsn.ood.cls.core.db.reservation.DeleteReservationsForExpiredLicenses;
import com.nsn.ood.cls.core.db.reservation.QueryReservations;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.errors.FeatureError;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSException;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = FeatureReleaseOperation.class)
@Loggable
public class FeatureReleaseOperation {
	private static final Logger LOG = LoggerFactory.getLogger(FeatureReleaseOperation.class);

	@ServiceDependency(filter = "(name=update)")
	private StatementExecutor<Update> updateExecutor;
	@ServiceDependency(filter = "(name=query)")
	private StatementExecutor<Query> queryExecutor;
	@ServiceDependency(filter = "(&(from=licenseType)(to=integer))")
	private Converter<License.Type, Integer> licenseType2IntegerConverter;
	@ServiceDependency(filter = "(&(from=reservation)(to=string))")
	private Converter<Reservation, String> reservation2StringConverter;
	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	@ServiceDependency
	private ReservationCreator reservationCreator;
	@ServiceDependency(filter = "(name=reservation)")
	private ConditionsMapper reservationConditionsMapper;

	public void releaseAll(final Client client, final boolean force) throws ReleaseException {
		try {
			LOG.debug("Releasing capacity for all features: force={}", force);
			this.updateExecutor.execute(createAllReservationsDelete(client, force));
		} catch (final SQLException e) {
			LOG.error(LogMessage.UPDATE_FAIL, e);
			throw new ReleaseException(e);
		}
	}

	private Update createAllReservationsDelete(final Client client, final boolean force) {
		return (force ? new DeleteReservations(client) //
				: new DeleteReservationsByLicenseType(client, License.Type.FLOATING_POOL, licenseType2IntegerConverter));
	}

	public void release(final Client client, final long featureCode, final boolean force) throws ReleaseException {
		final List<Reservation> reservations = getReservations(client, featureCode);
		List<String> converted = reservations.stream()
									.map(reservation2StringConverter::convertTo)
									.collect(Collectors.toList());
		if (reservations.isEmpty()) {
			LOG.debug("No current reservations");
		} else {
			LOG.debug("Current reservations: {}", converted);
			if (!force) {
				verifyReservations(reservations, featureCode);
			}
			deleteReservations(client, featureCode);
		}
	}

	private List<Reservation> getReservations(final Client client, final long featureCode) throws ReleaseException {
		try {
			final QueryReservations query = tryCreateQueryReservations(client, featureCode);
			this.queryExecutor.execute(query);
			return query.getList();
		} catch (final SQLException e) {
			LOG.error(LogMessage.QUERY_FAIL, e);
			throw new ReleaseException(featureCode, e);
		}
	}

	private QueryReservations tryCreateQueryReservations(final Client client, final long featureCode)
			throws ReleaseException {
		try {
			final Conditions conditions = ConditionsBuilder.createAndSkipMetaData()//
					.equalFilter("clientId", client.getClientId())//
					.equalFilter("featureCode", String.valueOf(featureCode)).build();
			return createQueryReservations(conditions);
		} catch (final ConditionProcessingException e) {
			throw new ReleaseException(featureCode, e);
		}
	}

	protected QueryReservations createQueryReservations(final Conditions conditions)
			throws ConditionProcessingException {
		return new QueryReservations(conditions, this.reservationConditionsMapper, this.reservationCreator);
	}

	private void verifyReservations(final List<Reservation> reservations, final long featureCode)
			throws ReleaseException {
		for (final Reservation reservation : reservations) {
			if (reservation.getType() == License.Type.POOL) {
				LOG.debug("Aborting operation: Pool license was found: {}",
						reservation2StringConverter.convertTo(reservation));
				throw new ReleaseException(featureCode, calculateCapacity(reservations));
			}
		}
	}

	private long calculateCapacity(final List<Reservation> reservations) {
		long capacity = 0;
		for (final Reservation reservation : reservations) {
			if (reservation.getType() == License.Type.POOL) {
				capacity += reservation.getCapacity();
			}
		}
		return capacity;
	}

	private void deleteReservations(final Client client, final long featureCode) throws ReleaseException {
		LOG.debug("Deleting reservations for feature: {}", featureCode);
		try {
			this.updateExecutor.execute(new DeleteReservationsByFeatureCode(client, featureCode));
		} catch (final SQLException e) {
			LOG.error(LogMessage.UPDATE_FAIL, e);
			throw new ReleaseException(featureCode, e);
		}
	}

	public void releaseForExpiredClients(final DateTime expires) throws ReleaseException {
		executeUpdate(new DeleteReservationsForExpiredClients(expires, timestamp2DateTimeConverter, licenseType2IntegerConverter));
	}

	public void releaseForExpiredLicenses(final DateTime endDate) throws ReleaseException {
		executeUpdate(new DeleteReservationsForExpiredLicenses(endDate, timestamp2DateTimeConverter));
	}

	private void executeUpdate(final Update update) throws ReleaseException {
		try {
			this.updateExecutor.execute(update);
		} catch (final SQLException e) {
			LOG.error(LogMessage.UPDATE_FAIL, e);
			throw new ReleaseException(e);
		}
	}

	public static class ReleaseException extends CLSException {
		private static final long serialVersionUID = -1686364455421026007L;

		private final FeatureError error;

		private ReleaseException(final Throwable cause) {
			super(cause.getMessage(), cause);
			this.error = null;
		}

		private ReleaseException(final long featureCode, final Throwable cause) {
			super(cause.getMessage(), cause);
			this.error = new FeatureError().withFeatureCode(featureCode).withCapacity(0L);
		}

		private ReleaseException(final long featureCode, final long capacity) {
			super();
			this.error = new FeatureError().withFeatureCode(featureCode).withCapacity(capacity);
		}

		public FeatureError getError() {
			return this.error;
		}
	}
}
