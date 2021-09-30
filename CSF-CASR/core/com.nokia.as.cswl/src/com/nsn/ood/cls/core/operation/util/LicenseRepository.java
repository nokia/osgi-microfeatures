/*
 * Copyright (c) 2016 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.util;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;

import com.nsn.ood.cls.core.db.Query;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.creator.LicenseCreatorFast;
import com.nsn.ood.cls.core.db.license.QueryLicensesForFeatureFast;
import com.nsn.ood.cls.core.service.error.UnknownErrorException;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.features.Feature.Type;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author wro50095
 *
 */
@Component(provides = LicenseRepository.class)
public class LicenseRepository {

	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DatetimeConverter;

	@ServiceDependency
	private LicenseCreatorFast licenseCreator;

	@ServiceDependency(filter = "(name=query)")
	private StatementExecutor<Query> queryExecutor;

	private Map<Type, List<License>> mapModeToLicense;
	private Map<Pair<Type, String>, List<License>> mapModeTargetTypeToLicense;

	public void init(final long featureCode) throws UnknownErrorException {
		final List<License> allLicenses = retrieveLicensesForFeatureCode(featureCode);
		createLicenseMaps(allLicenses);

	}

	public List<License> retrieveLicenses(final Client client, final Feature feature) {
		List<License> result = null;
		final Type mode = feature.getType();
		if (hasTargetType(client)) {
			result = this.mapModeTargetTypeToLicense.get(Pair.of(mode, client.getTargetType()));
		} else {
			result = this.mapModeToLicense.get(mode);
		}
		return ObjectUtils.firstNonNull(result, new ArrayList<License>());
	}

	private boolean hasTargetType(final Client client) {
		return client.getTargetType() != null;
	}

	private void createLicenseMaps(final List<License> allLicenses) {
		initMaps();
		for (final License license : allLicenses) {
			putToModeMap(license);
			if (hasTargetType(license)) {
				putToModeTargetTypeMap(license);
			}
		}
	}

	private void initMaps() {
		this.mapModeTargetTypeToLicense = new HashMap<>();
		this.mapModeToLicense = new HashMap<>();
	}

	private void putToModeMap(final License license) {
		final Type key = createType(license);
		putObjectToMapList(license, key, this.mapModeToLicense);
	}

	private Type createType(final License license) {
		return Type.fromValue(license.getMode().toString());
	}

	private void putToModeTargetTypeMap(final License license) {
		final Pair<Type, String> key = Pair.of(createType(license), license.getTargetType());
		putObjectToMapList(license, key, this.mapModeTargetTypeToLicense);
	}

	private <K, V> void putObjectToMapList(final V license, final K key, final Map<K, List<V>> map) {
		List<V> list = map.get(key);
		if (list == null) {
			list = new ArrayList<>();
			map.put(key, list);
		}
		list.add(license);
	}

	private boolean hasTargetType(final License license) {
		return license.getTargetType() != null;
	}

	private List<License> retrieveLicensesForFeatureCode(final long featureCode) throws UnknownErrorException {
		try {
			final QueryLicensesForFeatureFast query = new QueryLicensesForFeatureFast(featureCode, this.licenseCreator,
					timestamp2DatetimeConverter);
			this.queryExecutor.execute(query);
			return query.getList();
		} catch (final SQLException e) {
			throw new UnknownErrorException("Error occured during license retrieving", e);
		}
	}

}
