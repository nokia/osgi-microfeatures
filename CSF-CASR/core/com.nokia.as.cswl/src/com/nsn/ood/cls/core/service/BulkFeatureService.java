/*
 * Copyright (c) 2016 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.core.model.ClientWithTag;
import com.nsn.ood.cls.core.operation.ClientCreateOperation;
import com.nsn.ood.cls.core.operation.ClientUpdateOperation;
import com.nsn.ood.cls.core.operation.FeatureReservationOperation;
import com.nsn.ood.cls.core.operation.exception.CreateException;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.core.service.error.UnknownErrorException;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.clients.Clients;
import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.errors.FeatureError;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.hal.Embedded;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author wro50095
 *
 */
@Component(provides = BulkFeatureService.class)
@Loggable
public class BulkFeatureService {
	/**  */
	private static final String CLIENTS = "clients";
	@ServiceDependency
	private ClientCreateOperation clientCreateOperation;
	@ServiceDependency
	private ServiceExceptionFactory serviceExceptionFactory;
	@ServiceDependency(filter = "(&(from=errorException)(to=error))")
	private Converter<ErrorException, Error> errorException2ErrorConverter;
	@ServiceDependency
	private FeatureReservationOperation featureReservationOperation;
	@ServiceDependency
	private ClientUpdateOperation clientUpdateOperation;

	public Pair<List<Client>, List<Error>> bulkFeatureReservations(final Clients clients) throws UnknownErrorException {

		checkForDuplicateReservations(clients);

		final List<Client> successfulReservations = new ArrayList<>();
		final List<Error> errorReservations = new ArrayList<>();

		final Pair<List<ClientWithTag>, List<Error>> initializedClientsAndErrors = retrieveClientsWithETag(clients);

		final List<ClientWithTag> clientsReadyForReservations = initializedClientsAndErrors.getLeft();
		final List<Error> clientsWithErrors = initializedClientsAndErrors.getRight();

		errorReservations.addAll(clientsWithErrors);

		fillFeaturesInRetrievedClients(clientsReadyForReservations, clients);

		final Map<Long, List<ClientWithTag>> mapFeatureCodeToClients = splitByFeatureCode(clientsReadyForReservations);

		for (final Entry<Long, List<ClientWithTag>> entry : mapFeatureCodeToClients.entrySet()) {

			final Pair<List<Client>, List<Error>> partResult = this.featureReservationOperation
					.reserveCapacity(entry.getKey(), getClientsList(entry.getValue()));
			successfulReservations.addAll(partResult.getLeft());
			errorReservations.addAll(partResult.getRight());
		}

		final List<Error> clientUpdateErrors = updateClientKeepAliveTime(clientsReadyForReservations,
				successfulReservations);
		errorReservations.addAll(clientUpdateErrors);
		removeKeepAliveFailedClientsFromSuccessfulReservations(clientUpdateErrors, successfulReservations);

		final List<Client> success = groupClients(successfulReservations);
		final List<Error> failures = groupByError(errorReservations);

		return Pair.of(success, failures);

	}

	private void checkForDuplicateReservations(final Clients clients) throws UnknownErrorException {
		final Set<Pair<String, Long>> clientFeatureSet = new HashSet<>();
		for (final Client client : clients.getClients()) {
			for (final Feature feature : client.getFeatures()) {
				if (!clientFeatureSet.add(Pair.of(client.getClientId(), feature.getFeatureCode()))) {
					throw new UnknownErrorException(String.format("Duplicated client (%s) - feature (%d) conbination",
							client.getClientId(), feature.getFeatureCode()));
				}
			}
		}
	}

	private Client createClient(final String key) {
		Client mergedClient;
		mergedClient = new Client();
		mergedClient.setClientId(key);
		mergedClient.setFeatures(new ArrayList<Feature>());
		return mergedClient;
	}

	@SuppressWarnings("unchecked")
	private List<Error> groupByError(final List<Error> errors) {
		final Map<Long, Error> mapErrorCodeToErrors = new HashMap<>();
		for (final Error error : errors) {
			final long key = error.getErrorCode();
			Error errorEntry = mapErrorCodeToErrors.get(key);
			if (errorEntry == null) {
				errorEntry = createNewError(error);
				mapErrorCodeToErrors.put(key, errorEntry);
			}
			((List<Client>) errorEntry.getEmbedded().getAdditionalProperties().get(CLIENTS))
					.add((Client) error.getEmbedded().getAdditionalProperties().get(CLIENTS));
		}
		final List<Error> result = new ArrayList<>();
		for (final Error error : mapErrorCodeToErrors.values()) {
			result.add(groupByClient(error));
		}
		return result;
	}

	private Error createNewError(final Error error) {
		final Embedded embedded = new Embedded();
		embedded.getAdditionalProperties().put(CLIENTS, new ArrayList<>());
		return (Error) new Error().withDeveloperMessage(error.getDeveloperMessage()).withErrorCode(error.getErrorCode())
				.withUserMessage(error.getUserMessage()).withEmbedded(embedded);
	}

	@SuppressWarnings({
			"rawtypes", "unchecked" })
	private Error groupByClient(final Error error) {
		final List<Client> clients = (List<Client>) error.getEmbedded().getAdditionalProperties().get(CLIENTS);

		final Map<String, Client> mapClientIdToClient = new HashMap<>();
		for (final Client client : clients) {
			final String key = client.getClientId();
			Client mapEntry = mapClientIdToClient.get(key);
			if (mapEntry == null) {
				mapEntry = createErrorClient(key);
				mapClientIdToClient.put(key, mapEntry);
			}
			mapEntry.getErrorFeatures().addAll(client.getErrorFeatures());
		}
		final Error newError = createNewError(error);
		newError.getEmbedded().getAdditionalProperties().put(CLIENTS, new ArrayList(mapClientIdToClient.values()));
		return newError;

	}

	private Client createErrorClient(final String key) {
		Client mergedClient;
		mergedClient = new Client();
		mergedClient.setClientId(key);
		mergedClient.setErrorFeatures(new ArrayList<FeatureError>());
		return mergedClient;
	}

	private List<Client> groupClients(final List<Client> clientsToMerge) {
		final Map<String, Client> mapClientIdToClient = new HashMap<>();
		for (final Client client : clientsToMerge) {
			final String key = client.getClientId();
			Client mergedClient = mapClientIdToClient.get(key);
			if (mergedClient == null) {
				mergedClient = createClient(key);
				mapClientIdToClient.put(key, mergedClient);
			}
			mergedClient.getFeatures().addAll(client.getFeatures());
		}
		return new ArrayList<Client>(mapClientIdToClient.values());
	}

	private List<Error> updateClientKeepAliveTime(final List<ClientWithTag> allClients,
			final List<Client> successfulReservations) {
		final List<Error> clientUpdateErrors = new ArrayList<>();
		for (final ClientWithTag clientWithEtag : allClients) {
			if (clientHasAnySuccessfulReservation(clientWithEtag, successfulReservations)) {
				try {
					this.clientUpdateOperation.updateExpirationTime(clientWithEtag);
				} catch (final UpdateException e) {
					final ServiceException serviceException = this.serviceExceptionFactory
							.client(ErrorCode.CANNOT_UPDATE_KEEP_ALIVE, e, clientWithEtag.getObject());
					final ErrorException errorException = serviceException.getExceptions().iterator().next();
					clientUpdateErrors.add(errorException2ErrorConverter.convertTo(errorException));
				}
			}
		}
		return clientUpdateErrors;
	}

	private boolean clientHasAnySuccessfulReservation(final ClientWithTag clientWithEtag,
			final List<Client> successfulReservations) {
		for (final Client client : successfulReservations) {
			if (client.getClientId().equals(clientWithEtag.getObject().getClientId())) {
				return true;
			}
		}
		return false;
	}

	private void removeKeepAliveFailedClientsFromSuccessfulReservations(final List<Error> clientUpdateErrors,
			final List<Client> successfulReservations) {
		for (final Error error : clientUpdateErrors) {
			@SuppressWarnings("unchecked")
			final List<Client> clients = (List<Client>) error.getEmbedded().getAdditionalProperties().get(CLIENTS);
			for (final Client client : clients) {
				removeClientFromList(client, successfulReservations);
			}

		}
	}

	private void removeClientFromList(final Client clientToRemove, final List<Client> successfulReservations) {
		final List<Client> toRemoveList = new ArrayList<>();
		for (final Client client : successfulReservations) {
			if (client.getClientId().equals(clientToRemove.getClientId())) {
				toRemoveList.add(client);
			}
		}
		successfulReservations.removeAll(toRemoveList);
	}

	private List<Client> getClientsList(final List<ClientWithTag> value) {
		final List<Client> result = new ArrayList<>();
		for (final ClientWithTag clientWithTag : value) {
			result.add(clientWithTag.getObject());
		}
		return result;
	}

	private Map<Long, List<ClientWithTag>> splitByFeatureCode(final List<ClientWithTag> clients) {
		final Map<Long, List<ClientWithTag>> mapFeatureCodeToClientReservations = new HashMap<>();
		for (final ClientWithTag client : clients) {
			for (final Feature feature : client.getObject().getFeatures()) {
				final Long featureCode = feature.getFeatureCode();
				List<ClientWithTag> list = mapFeatureCodeToClientReservations.get(featureCode);
				if (list == null) {
					list = new ArrayList<>();
					mapFeatureCodeToClientReservations.put(featureCode, list);
				}
				list.add(client);
			}
		}
		return mapFeatureCodeToClientReservations;
	}

	private void fillFeaturesInRetrievedClients(final List<ClientWithTag> clientsWithETag, final Clients clients) {
		final Map<String, Client> mapClientIdToClients = mapClientIdToClients(clients);
		for (final ClientWithTag clientWithTag : clientsWithETag) {
			final Client clientWithFeatures = mapClientIdToClients.get(clientWithTag.getObject().getClientId());
			clientWithTag.getObject().setFeatures(clientWithFeatures.getFeatures());
		}

	}

	private Pair<List<ClientWithTag>, List<Error>> retrieveClientsWithETag(final Clients clients) {
		final List<ClientWithTag> clientsWithEtag = new ArrayList<>();
		final List<Error> errors = new ArrayList<>();

		for (final Client client : clients.getClients()) {
			try {
				clientsWithEtag.add(getOrCreateClientWithTag(client.getClientId()));
			} catch (final ServiceException e) {
				final Error error = errorException2ErrorConverter.convertTo(e.getExceptions().iterator().next());
				errors.add(error);
			}
		}
		return Pair.of(clientsWithEtag, errors);
	}

	private ClientWithTag getOrCreateClientWithTag(final String clientId) throws ServiceException {
		try {
			return this.clientCreateOperation.createIfNotExist(clientId);
		} catch (final CreateException | UnknownErrorException e) {
			throw this.serviceExceptionFactory.client(ErrorCode.CANNOT_RESERVE_CLIENT_ID, e,
					new Client().withClientId(clientId));
		}
	}

	private Map<String, Client> mapClientIdToClients(final Clients clients) {
		final Map<String, Client> map = new HashMap<>();
		for (final Client client : clients.getClients()) {
			map.put(client.getClientId(), client);
		}
		return map;
	}

}
