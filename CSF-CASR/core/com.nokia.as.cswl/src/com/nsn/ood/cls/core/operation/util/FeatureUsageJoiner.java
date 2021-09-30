/*
 * Copyright (c) 2016 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.nsn.ood.cls.core.model.FeatureLicenseUsageDelta;
import com.nsn.ood.cls.core.model.FeatureUpdate;
import com.nsn.ood.cls.core.model.LicenseUpdate;


/**
 * @author wro50095
 *
 */
public class FeatureUsageJoiner {
	private FeatureLicenseUsageDelta currentState;

	public void add(final FeatureLicenseUsageDelta featureUsageDelta) {
		if (this.currentState == null) {
			this.currentState = featureUsageDelta;
		} else {
			this.currentState = add(this.currentState, featureUsageDelta);
		}

	}

	private FeatureLicenseUsageDelta add(final FeatureLicenseUsageDelta delta1, final FeatureLicenseUsageDelta delta2) {
		final FeatureLicenseUsageDelta result = createFeatureUsageDelta();

		updateFeaturDelta(delta1, delta2, result.getFeature());
		updateLicenseDelta(delta1, delta2, result.getLicense());

		return result;
	}

	private void updateLicenseDelta(final FeatureLicenseUsageDelta delta1, final FeatureLicenseUsageDelta delta2,
			final List<LicenseUpdate> license) {
		final List<LicenseUpdate> allLicenseUpdate = new ArrayList<>();
		allLicenseUpdate.addAll(delta1.getLicense());
		allLicenseUpdate.addAll(delta2.getLicense());

		license.addAll(sumLicenseUpdate(allLicenseUpdate));

	}

	private List<LicenseUpdate> sumLicenseUpdate(final List<LicenseUpdate> allLicenseUpdate) {
		final Map<String, Long> mapSerialToDelta = new HashMap<>();
		for (final LicenseUpdate licenseUpdate : allLicenseUpdate) {
			final String key = licenseUpdate.getSerialNumber();
			Long value = mapSerialToDelta.get(key);
			if (value == null) {
				value = 0L;
			}
			value = value + licenseUpdate.getUsageDelta();
			mapSerialToDelta.put(key, value);
		}
		return generateLicenseUpdate(mapSerialToDelta);
	}

	private List<LicenseUpdate> generateLicenseUpdate(final Map<String, Long> mapSerialToDelta) {
		final List<LicenseUpdate> result = new ArrayList<>();
		for (final Entry<String, Long> entry : mapSerialToDelta.entrySet()) {
			final LicenseUpdate lu = new LicenseUpdate();
			lu.setSerialNumber(entry.getKey());
			lu.setUsageDelta(entry.getValue());
			result.add(lu);
		}
		return result;
	}

	private void updateFeaturDelta(final FeatureLicenseUsageDelta delta1, final FeatureLicenseUsageDelta delta2,
			final FeatureUpdate featureUpdate) {
		final long usageDelta = delta1.getFeature().getUsageDelta() + delta2.getFeature().getUsageDelta();

		featureUpdate.setFeatureCode(delta1.getFeature().getFeatureCode());
		featureUpdate.setUsageDelta(usageDelta);
	}

	private FeatureLicenseUsageDelta createFeatureUsageDelta() {
		final FeatureLicenseUsageDelta result = new FeatureLicenseUsageDelta();
		result.setFeature(new FeatureUpdate());
		result.setLicense(new ArrayList<LicenseUpdate>());
		return result;
	}

	public FeatureLicenseUsageDelta getJoinedFeatureUsage() {
		return this.currentState;

	}

}
