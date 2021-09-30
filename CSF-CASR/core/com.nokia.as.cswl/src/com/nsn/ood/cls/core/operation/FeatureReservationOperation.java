/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.net.URI;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
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
import com.nsn.ood.cls.core.db.creator.LicenseCreator;
import com.nsn.ood.cls.core.db.creator.ReservationCreator;
import com.nsn.ood.cls.core.db.license.QueryLicensesForFeature;
import com.nsn.ood.cls.core.db.reservation.BulkUpdateReservations;
import com.nsn.ood.cls.core.db.reservation.QueryReservations;
import com.nsn.ood.cls.core.db.reservation.QueryReservationsForFeatureCodeForClients;
import com.nsn.ood.cls.core.db.reservation.UpdateFeatureUsage;
import com.nsn.ood.cls.core.db.reservation.UpdateReservations;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.core.model.FeatureLicenseUsageDelta;
import com.nsn.ood.cls.core.operation.util.FeatureUsageJoiner;
import com.nsn.ood.cls.core.operation.util.FeatureUtils;
import com.nsn.ood.cls.core.operation.util.LicenseRepository;
import com.nsn.ood.cls.core.operation.util.ReservationErrorType;
import com.nsn.ood.cls.core.operation.util.ReservationsUtils;
import com.nsn.ood.cls.core.operation.util.ReservationsUtils.CalculationException;
import com.nsn.ood.cls.core.operation.util.UsageCalculator;
import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.core.service.error.ErrorExceptionFactory;
import com.nsn.ood.cls.core.service.error.UnknownErrorException;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.errors.FeatureError;
import com.nsn.ood.cls.model.gen.features.Allocation;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSException;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Loggable
@Component(provides = FeatureReservationOperation.class)
public class FeatureReservationOperation {
	private static final Logger LOG = LoggerFactory.getLogger(FeatureReservationOperation.class);

	@ServiceDependency(filter = "(name=query)")
	private StatementExecutor<Query> queryExecutor;
	@ServiceDependency(filter = "(name=update)")
	private StatementExecutor<Update> updateExecutor;
	@ServiceDependency
	private ReservationsUtils reservationsUtils;
	@ServiceDependency
	private FeatureUtils featureUtils;
	@ServiceDependency
	private LicenseCreator licenseCreator;
	@ServiceDependency(filter = "(&(from=feature)(to=string))")
	private Converter<Feature, String> feature2StringConverter;
	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DatetimeConverter;
	@ServiceDependency(filter = "(&(from=featureType)(to=integer))")
	private Converter<Feature.Type, Integer> featureType2IntegerConverter;
	@ServiceDependency(filter = "(&(from=licenseMode)(to=integer))")
	private Converter<License.Mode, Integer> licenseMode2IntegerConverter;
	@ServiceDependency(filter = "(&(from=licenseType)(to=integer))")
	private Converter<License.Type, Integer> licenseType2IntegerConverter;
	@ServiceDependency(filter = "(&(from=errorException)(to=error))")
	private Converter<ErrorException, Error> errorException2ErrorConverter;
	@ServiceDependency
	private ReservationCreator reservationCreator;
	@ServiceDependency(filter = "(name=reservation)")
	private ConditionsMapper reservationConditionsMapper;
	@ServiceDependency
	private UsageCalculator usageCalculator;
	@ServiceDependency
	private ErrorExceptionFactory errorExceptionFactory;
	@ServiceDependency
	private LicenseRepository licenseRepository;
	@ServiceDependency
	private BulkUpdateReservations bulkUpdateReservations;

	public ReservationResult reserveCapacity(final Client client, final Feature feature) throws ReservationException {
		LOG.debug("Reserve capacity for feature: {}", feature2StringConverter.convertTo(feature));

		final List<Reservation> reservations = getReservations(client, feature);
		LOG.debug("Current reservations: {}", reservations.size());

		if (isCalculationsNeeded(feature, reservations)) {
			final List<License> licenses = getLicenses(client, feature);
			final List<Reservation> clonedReservations = this.usageCalculator.cloneReservations(reservations);
			LOG.debug("Matching licenses: {}", licenses.size());
			final List<Reservation> newReservations = calculateReservations(client, feature, licenses, reservations);
			LOG.debug("New reservations: {}", newReservations.size());

			updateReservations(client, feature, newReservations);

			final FeatureLicenseUsageDelta usageDelta = this.usageCalculator.calculateUsage(feature, clonedReservations,
					newReservations, licenses);
			updateUsage(usageDelta, feature);

			return new ReservationResultImpl(true, //
					this.featureUtils.createFeatureWithAllocations(feature, newReservations));
		} else {
			LOG.debug("No changes in reservations needed");
			return new ReservationResultImpl(false, //
					this.featureUtils.createFeatureWithAllocations(feature, reservations));
		}
	}

	public Pair<List<Client>, List<Error>> reserveCapacity(final long featureCode, final List<Client> clients)
			throws UnknownErrorException {

		final List<Client> reservationResultList = new ArrayList<>();
		final List<Error> errorList = new ArrayList<>();

		LOG.debug("Reserve capacity for feature: {}", featureCode);

		final List<Reservation> allReservations = retrieveReservations(featureCode, clients);

		final Map<String, List<Reservation>> mapReservationsPerClient = mapReservationsPerClient(allReservations,
				clients);

		this.licenseRepository.init(featureCode);
		this.bulkUpdateReservations.init(featureCode);

		final FeatureUsageJoiner featureUsageJoinser = new FeatureUsageJoiner();

		for (final Client client : clients) {
			final List<Reservation> reservations = mapReservationsPerClient.get(client.getClientId());
			LOG.debug("Current reservations for client {}: {}", client.getClientId(), reservations.size());
			final Feature feature = getFeatureByCode(featureCode, client);
			if (isCalculationsNeeded(feature, reservations)) {
				final List<License> licenses = this.licenseRepository.retrieveLicenses(client, feature);
				final List<Reservation> clonedReservations = this.usageCalculator.cloneReservations(reservations);
				LOG.debug("Matching licenses: {}", licenses.size());

				try {
					final List<Reservation> newReservations = this.reservationsUtils.calculate(client, feature,
							licenses, reservations);
					LOG.debug("New reservations: {}", newReservations.size());

					this.bulkUpdateReservations.addReservationsToUpdate(client, newReservations);

					final FeatureLicenseUsageDelta usageDelta = this.usageCalculator.calculateUsage(feature,
							clonedReservations, newReservations, licenses);
					featureUsageJoinser.add(usageDelta);

					reservationResultList.add(createReservationClient(client.getClientId(),
							this.featureUtils.createFeatureWithAllocations(feature, newReservations)));
				} catch (final CalculationException e) {
					final ErrorException errorException = this.errorExceptionFactory
							.feature(convertCode(e.getErrorType()), e, e.getError());
					final Error error = errorException2ErrorConverter.convertTo(errorException);

					final Client errorClient = new Client();
					errorClient.setClientId(client.getClientId());
					errorClient.setErrorFeatures(Collections.singletonList(e.getError()));
					error.getEmbedded().setAdditionalProperty("clients", errorClient);
					errorList.add(error);
				}
			} else {
				LOG.debug("No changes in reservations needed");
				reservationResultList.add(createReservationClient(client.getClientId(),
						this.featureUtils.createFeatureWithAllocations(feature, reservations)));
			}
		}
		this.bulkUpdateReservations.performReservationsUpdate();
		final FeatureLicenseUsageDelta featureLicenseUsageDelta = featureUsageJoinser.getJoinedFeatureUsage();
		if (featureLicenseUsageDelta != null) {
			updateFeatureUsage(featureLicenseUsageDelta);
		}
		return Pair.of(reservationResultList, errorList);
	}

	private List<Reservation> retrieveReservations(final long featureCode, final List<Client> clients)
			throws UnknownErrorException {
		List<Reservation> allReservations = null;
		try {
			allReservations = getReservations(clients, featureCode);
		} catch (final SQLException e) {
			throw new UnknownErrorException("Reservation retrieve error", e);
		}
		return allReservations;
	}

	private ErrorCode convertCode(final ReservationErrorType type) {
		if (type == ReservationErrorType.ON_OFF) {
			return ErrorCode.ON_OFF_LICENSE_MISSING;
		} else if (type == ReservationErrorType.RELEASE) {
			return ErrorCode.CANNOT_RELEASE_CAPACITY;
		}
		return ErrorCode.NOT_ENOUGH_CAPACITY;
	}

	private Client createReservationClient(final String clientId, final Feature feature) {
		final Client result = new Client();
		result.setClientId(clientId);
		result.setFeatures(Collections.singletonList(correctResourceLinks(feature)));
		return result;
	}

	private Feature correctResourceLinks(final Feature feature) {
		for (final Allocation allocation : feature.getAllocations()) {
			allocation.setPoolLicense(URI.create("/licenses/" + allocation.getPoolLicense().toString()));
		}
		return feature;
	}

	private Feature getFeatureByCode(final long featureCode, final Client client) {
		for (final Feature feature : client.getFeatures()) {
			// TODO: PERF: change to map, crate client subclass with feature mapping
			if (featureCode == feature.getFeatureCode()) {
				return feature;
			}
		}
		return null;
	}

	private Map<String, List<Reservation>> mapReservationsPerClient(final List<Reservation> reservations,
			final List<Client> clients) {
		final Map<String, List<Reservation>> mapClientToReservations = new HashMap<>();

		for (final Client client : clients) {
			mapClientToReservations.put(client.getClientId(), new ArrayList<Reservation>());
		}

		for (final Reservation reservation : reservations) {
			mapClientToReservations.get(reservation.getClientId()).add(reservation);
		}
		return mapClientToReservations;
	}

	private void updateUsage(final FeatureLicenseUsageDelta usageDelta, final Feature feature)
			throws ReservationException {
		try {
			this.updateExecutor.execute(new UpdateFeatureUsage(usageDelta));
		} catch (final SQLException e) {
			LOG.error(LogMessage.UPDATE_FAIL, e);
			throw new ReservationException(feature, e);
		}
	}

	private void updateFeatureUsage(final FeatureLicenseUsageDelta usageDelta) throws UnknownErrorException {
		try {
			this.updateExecutor.execute(new UpdateFeatureUsage(usageDelta));
		} catch (final SQLException e) {
			throw new UnknownErrorException("Error occured during update feature usage", e);
		}
	}

	private List<Reservation> getReservations(final List<Client> clients, final long feature) throws SQLException {
		final QueryReservationsForFeatureCodeForClients featureCodeForClients = new QueryReservationsForFeatureCodeForClients(
				feature, clients, this.reservationCreator);
		this.queryExecutor.execute(featureCodeForClients);
		return featureCodeForClients.getList();
	}

	private List<Reservation> getReservations(final Client client, final Feature feature) throws ReservationException {
		final QueryReservations query = tryCreateQueryReservations(client, feature);
		executeQuery(query, feature);
		return query.getList();
	}

	private QueryReservations tryCreateQueryReservations(final Client client, final Feature feature)
			throws ReservationException {
		try {
			final Conditions conditions = ConditionsBuilder.createAndSkipMetaData()//
					.equalFilter("clientId", client.getClientId())//
					.equalFilter("featureCode", String.valueOf(feature.getFeatureCode())).build();
			return createQueryReservations(conditions);
		} catch (final ConditionProcessingException e) {
			throw new ReservationException(feature, e);
		}
	}

	protected QueryReservations createQueryReservations(final Conditions conditions)
			throws ConditionProcessingException {
		return new QueryReservations(conditions, this.reservationConditionsMapper, this.reservationCreator);
	}

	private boolean isCalculationsNeeded(final Feature feature, final List<Reservation> reservations) {
		return this.reservationsUtils.isCalculationsNeeded(feature, reservations);
	}

	private List<License> getLicenses(final Client client, final Feature feature) throws ReservationException {
		final QueryLicensesForFeature query = createLicensesQuery(client, feature);
		executeQuery(query, feature);
		return query.getList();
	}

	protected QueryLicensesForFeature createLicensesQuery(final Client client, final Feature feature) {
		return new QueryLicensesForFeature(client, feature, this.licenseCreator, timestamp2DatetimeConverter, featureType2IntegerConverter);
	}

	private List<Reservation> calculateReservations(final Client client, final Feature feature,
			final List<License> licenses, final List<Reservation> reservations) throws ReservationException {
		try {
			return this.reservationsUtils.calculate(client, feature, licenses, reservations);
		} catch (final CalculationException e) {
			throw new ReservationException(e);
		}
	}

	private void executeQuery(final Query query, final Feature feature) throws ReservationException {
		try {
			this.queryExecutor.execute(query);
		} catch (final SQLException e) {
			LOG.error(LogMessage.QUERY_FAIL, e);
			throw new ReservationException(feature, e);
		}
	}

	private void updateReservations(final Client client, final Feature feature, final List<Reservation> reservations)
			throws ReservationException {
		try {
			this.updateExecutor.execute(createReservationsUpdate(client, feature, reservations));
		} catch (final SQLException e) {
			LOG.error(LogMessage.UPDATE_FAIL, e);
			throw new ReservationException(feature, e);
		}
	}

	protected UpdateReservations createReservationsUpdate(final Client client, final Feature feature,
			final List<Reservation> reservations) {
		return new UpdateReservations(client, feature.getFeatureCode(), reservations, timestamp2DatetimeConverter, licenseMode2IntegerConverter, licenseType2IntegerConverter);
	}

	public interface ReservationResult {
		boolean isUpdated();

		Feature getFeature();
	}

	private static final class ReservationResultImpl implements ReservationResult {
		private final boolean updated;
		private final Feature feature;

		private ReservationResultImpl(final boolean updated, final Feature feature) {
			this.updated = updated;
			this.feature = feature;
		}

		@Override
		public boolean isUpdated() {
			return this.updated;
		}

		@Override
		public Feature getFeature() {
			return this.feature;
		}
	}

	public static final class ReservationException extends CLSException {
		private static final long serialVersionUID = 3496531407080846580L;

		private final FeatureError error;
		private final ReservationErrorType type;

		private ReservationException(final Feature feature, final Throwable cause) {
			super(cause.getMessage(), cause);
			this.error = new FeatureError().withFeatureCode(feature.getFeatureCode())
					.withRequestedCapacity(feature.getCapacity());
			this.type = (feature.getType() == Feature.Type.CAPACITY ? ReservationErrorType.CAPACITY
					: ReservationErrorType.ON_OFF);
		}

		private ReservationException(final CalculationException e) {
			super(e.getMessage(), e);
			this.error = e.getError();
			this.type = e.getErrorType();
		}

		public FeatureError getError() {
			return this.error;
		}

		public ReservationErrorType getErrorType() {
			return this.type;
		}
	}
}
