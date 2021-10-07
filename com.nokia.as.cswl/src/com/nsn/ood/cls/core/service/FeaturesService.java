/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.model.ClientTag;
import com.nsn.ood.cls.core.model.ClientWithTag;
import com.nsn.ood.cls.core.model.FeaturesWithTag;
import com.nsn.ood.cls.core.operation.ClientCreateOperation;
import com.nsn.ood.cls.core.operation.ClientRetrieveOperation;
import com.nsn.ood.cls.core.operation.ClientUpdateOperation;
import com.nsn.ood.cls.core.operation.FeatureLockOperation;
import com.nsn.ood.cls.core.operation.FeatureLockOperation.LockException;
import com.nsn.ood.cls.core.operation.FeatureReleaseOperation;
import com.nsn.ood.cls.core.operation.FeatureReleaseOperation.ReleaseException;
import com.nsn.ood.cls.core.operation.FeatureReservationOperation;
import com.nsn.ood.cls.core.operation.FeatureReservationOperation.ReservationException;
import com.nsn.ood.cls.core.operation.FeatureReservationOperation.ReservationResult;
import com.nsn.ood.cls.core.operation.FeatureRetrieveOperation;
import com.nsn.ood.cls.core.operation.UpdateCapacityOperation;
import com.nsn.ood.cls.core.operation.exception.CreateException;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.core.operation.util.ReservationErrorType;
import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.core.service.error.ErrorExceptionFactory;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.core.service.error.UnknownErrorException;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component(provides = FeaturesService.class)
@Loggable
public class FeaturesService {
	@ServiceDependency
	private ClientCreateOperation clientCreateOperation;
	@ServiceDependency
	private FeatureReservationOperation featureReservationOperation;
	@ServiceDependency
	private ClientUpdateOperation clientUpdateOperation;
	@ServiceDependency
	private ClientRetrieveOperation clientRetrieveOperation;
	@ServiceDependency
	private FeatureReleaseOperation featureReleaseOperation;
	@ServiceDependency
	private FeatureLockOperation featureLockOperation;
	@ServiceDependency
	private FeatureRetrieveOperation featureRetrieveOperation;
	@ServiceDependency
	private UpdateCapacityOperation updateCapacityOperation;
	@ServiceDependency
	private ErrorExceptionFactory errorExceptionFactory;
	@ServiceDependency
	private ServiceExceptionFactory serviceExceptionFactory;

	public FeaturesWithTag reserveCapacity(final String clientId, final List<Feature> features)
			throws ServiceException {
		lockFeatures(getFeatureCodes(features));

		final ClientWithTag clientWithTag = getOrCreateClientWithTag(clientId);

		final Pair<List<Feature>, List<Long>> featuresResult = reserveCapacity(clientWithTag.getObject(), features);

		final ClientTag clientTag = updateExpirationTime(clientWithTag);

		return new FeaturesWithTag().withClientTag(clientTag).withObject(featuresResult.getLeft());
	}

	private List<Long> getFeatureCodes(final List<Feature> features) {
		final List<Long> featureCodes = new ArrayList<>();
		for (final Feature feature : features) {
			featureCodes.add(feature.getFeatureCode());
		}
		return featureCodes;
	}

	private ClientWithTag getOrCreateClientWithTag(final String clientId) throws ServiceException {
		try {
			return this.clientCreateOperation.createIfNotExist(clientId);
		} catch (final CreateException | UnknownErrorException e) {
			throw this.serviceExceptionFactory.client(ErrorCode.CANNOT_RESERVE_CLIENT_ID, e,
					new Client().withClientId(clientId));
		}
	}

	private Pair<List<Feature>, List<Long>> reserveCapacity(final Client client, final List<Feature> features)
			throws ServiceException {
		final List<Feature> featuresWithAllocations = new ArrayList<>();
		final List<Long> featuresToUpdate = new ArrayList<>();
		final List<ErrorException> errorExceptions = new ArrayList<>();
		for (final Feature feature : features) {
			try {
				final ReservationResult result = this.featureReservationOperation.reserveCapacity(client, feature);
				featuresWithAllocations.add(result.getFeature());
				if (result.isUpdated()) {
					featuresToUpdate.add(result.getFeature().getFeatureCode());
				}
			} catch (final ReservationException e) {
				errorExceptions.add(this.errorExceptionFactory.feature(convertCode(e.getErrorType()), e, e.getError()));
			}
		}
		if (!errorExceptions.isEmpty()) {
			throw this.serviceExceptionFactory.exceptions(errorExceptions);
		}
		return Pair.of(featuresWithAllocations, featuresToUpdate);
	}

	private ErrorCode convertCode(final ReservationErrorType type) {
		if (type == ReservationErrorType.ON_OFF) {
			return ErrorCode.ON_OFF_LICENSE_MISSING;
		} else if (type == ReservationErrorType.RELEASE) {
			return ErrorCode.CANNOT_RELEASE_CAPACITY;
		}
		return ErrorCode.NOT_ENOUGH_CAPACITY;
	}

	private ClientTag updateExpirationTime(final ClientWithTag clientWithTag) throws ServiceException {
		try {
			return this.clientUpdateOperation.updateExpirationTime(clientWithTag);
		} catch (final UpdateException e) {
			throw this.serviceExceptionFactory.client(ErrorCode.CANNOT_UPDATE_KEEP_ALIVE, e, clientWithTag.getObject());
		}
	}

	public void releaseCapacity(final String clientId, final List<Long> featureCodes, final boolean force)
			throws ServiceException {
		final ClientWithTag clientWithTag = getClientWithTag(clientId);

		if (featureCodes.isEmpty()) {
			releaseAll(force, clientWithTag);
		} else {
			releaseFeatures(featureCodes, force, clientWithTag);
		}
	}

	private void releaseAll(final boolean force, final ClientWithTag clientWithTag) throws ServiceException {
		final List<Long> featureCodes = lockFeaturesForClient(clientWithTag.getObject());
		if (!featureCodes.isEmpty()) {
			releaseCapacity(clientWithTag.getObject(), force);
			updateCapacity(featureCodes);
		}
	}

	private void releaseFeatures(final List<Long> featureCodes, final boolean force, final ClientWithTag clientWithTag)
			throws ServiceException {
		lockFeatures(featureCodes);
		for (final Long featureCode : featureCodes) {
			releaseCapacity(clientWithTag.getObject(), featureCode, force);
		}
		updateCapacity(featureCodes);
	}

	private void releaseCapacity(final Client client, final boolean force) throws ServiceException {
		try {
			this.featureReleaseOperation.releaseAll(client, force);
		} catch (final ReleaseException e) {
			throw this.serviceExceptionFactory.feature(ErrorCode.CANNOT_RELEASE_CAPACITY, e, e.getError());
		}
	}

	private void releaseCapacity(final Client client, final long featureCode, final boolean force)
			throws ServiceException {
		try {
			this.featureReleaseOperation.release(client, featureCode, force);
		} catch (final ReleaseException e) {
			throw this.serviceExceptionFactory.feature(ErrorCode.CANNOT_RELEASE_CAPACITY, e, e.getError());
		}
	}

	private void lockFeatures(final List<Long> featureCodes) throws ServiceException {
		try {
			this.featureLockOperation.lock(featureCodes);
		} catch (final LockException e) {
			throw this.serviceExceptionFactory.error(ErrorCode.CONCURRENT_ACTIONS_FAIL, e);
		}
	}

	private List<Long> lockFeaturesForClient(final Client client) throws ServiceException {
		try {
			return this.featureLockOperation.lockForClient(client);
		} catch (final LockException e) {
			throw this.serviceExceptionFactory.error(ErrorCode.CONCURRENT_ACTIONS_FAIL, e);
		}
	}

	private void updateCapacity(final List<Long> featureCodes) throws ServiceException {
		if (!featureCodes.isEmpty()) {
			try {
				this.updateCapacityOperation.updateCapacity(featureCodes);
			} catch (final UpdateException e) {
				throw this.serviceExceptionFactory.error(ErrorCode.CAPACITY_UPDATE_FAIL, e);
			}
		}
	}

	public MetaDataList<Feature> getFeatures(final String clientId, final Conditions conditions)
			throws ServiceException {
		final ClientWithTag clientWithTag = getClientWithTag(clientId);
		final Conditions conditionsWithClientId = ConditionsBuilder.use(conditions)
				.equalFilter("clientId", clientWithTag.getObject().getClientId()).build();

		try {
			return this.featureRetrieveOperation.getList(conditionsWithClientId);
		} catch (final RetrieveException e) {
			throw this.serviceExceptionFactory.violation(e, e.getError());
		}
	}

	private ClientWithTag getClientWithTag(final String clientId) throws ServiceException {
		try {
			final ClientWithTag clientWithTag = this.clientRetrieveOperation.getClient(clientId);
			if (clientWithTag == null) {
				throw this.serviceExceptionFactory.clientNotFound(new Client().withClientId(clientId));
			}
			return clientWithTag;
		} catch (final UnknownErrorException e) {
			throw this.serviceExceptionFactory.internalError(e);
		}
	}
}
