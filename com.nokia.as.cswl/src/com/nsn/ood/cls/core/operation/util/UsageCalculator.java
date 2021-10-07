/*
 * Copyright (c) 2016 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.felix.dm.annotation.api.Component;

import com.nsn.ood.cls.core.model.FeatureLicenseUsageDelta;
import com.nsn.ood.cls.core.model.FeatureUpdate;
import com.nsn.ood.cls.core.model.LicenseUpdate;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author wro50095
 *
 */
@Component(provides = UsageCalculator.class)
public class UsageCalculator {
	private final Map<String, Long> mapLicenseToDifference = new HashMap<>();

	public FeatureLicenseUsageDelta calculateUsage(final Feature feature, final List<Reservation> reservations,
			final List<Reservation> newReservations, final List<License> licenses) {

		final Map<String, Reservation> mapSerialToReservations = new HashMap<>();
		final Map<String, Reservation> mapSerialToNewReservations = new HashMap<>();

		for (final Reservation reservation : reservations) {
			mapSerialToReservations.put(reservation.getSerialNumber(), reservation);
		}

		for (final Reservation reservation : newReservations) {
			mapSerialToNewReservations.put(reservation.getSerialNumber(), reservation);
		}

		return calculateUsage(feature, mapSerialToReservations, mapSerialToNewReservations, licenses);

	}

	private FeatureLicenseUsageDelta calculateUsage(final Feature feature,
			final Map<String, Reservation> mapSerialToReservations,
			final Map<String, Reservation> mapSerialToNewReservations, final List<License> licenses) {

		long featureUsageDelta = 0;

		for (final License license : licenses) {
			final Reservation oldReservation = mapSerialToReservations.get(license.getSerialNumber());
			final Reservation newReservation = mapSerialToNewReservations.get(license.getSerialNumber());

			Long oldUsage;
			if (oldReservation != null) {
				oldUsage = oldReservation.getCapacity();
			} else {
				oldUsage = 0L;
			}
			Long newUsage;
			if (newReservation != null) {
				newUsage = newReservation.getCapacity();
			} else {
				newUsage = 0L;
			}
			final long localDelta = newUsage - oldUsage;
			featureUsageDelta += localDelta;
			license.setUsedCapacity(license.getUsedCapacity() + localDelta);
			updateLicenseDifference(license.getSerialNumber(), localDelta);

		}

		final FeatureLicenseUsageDelta result = new FeatureLicenseUsageDelta();

		result.setLicense(createLicenseUpdate());
		result.setFeature(createFeatureUpdate(feature, featureUsageDelta));

		cleanup();
		return result;

	}

	private void cleanup() {
		this.mapLicenseToDifference.clear();
	}

	private FeatureUpdate createFeatureUpdate(final Feature feature, final long featureUsageDelta) {
		final FeatureUpdate result = new FeatureUpdate();
		result.setUsageDelta(featureUsageDelta);
		result.setFeatureCode(feature.getFeatureCode());
		return result;
	}

	private List<LicenseUpdate> createLicenseUpdate() {
		final List<LicenseUpdate> result = new ArrayList<>();
		for (final Entry<String, Long> entrySet : this.mapLicenseToDifference.entrySet()) {
			final LicenseUpdate lu = new LicenseUpdate();
			lu.setSerialNumber(entrySet.getKey());

			lu.updateUsageDelta(entrySet.getValue());
			result.add(lu);
		}
		return result;
	}

	private void updateLicenseDifference(final String serialNumber, final long difference) {
		Long diffForLic = this.mapLicenseToDifference.get(serialNumber);
		if (diffForLic == null) {
			diffForLic = 0L;
		}
		diffForLic += difference;
		this.mapLicenseToDifference.put(serialNumber, diffForLic);
	}

	public List<Reservation> cloneReservations(final List<Reservation> reservations) {
		final List<Reservation> result = new ArrayList<>(reservations.size());
		for (final Reservation reservation : reservations) {
			result.add(new Reservation(reservation));
		}
		return result;
	}

	public static final class UsageCalculatorInput {
		private final Feature feature;
		private final List<Reservation> reservations;
		private final List<Reservation> newReservations;
		private final List<License> licenses;

		public UsageCalculatorInput(final Feature feature, final List<Reservation> reservations,
				final List<Reservation> newReservations, final List<License> licenses) {
			this.feature = feature;
			this.reservations = reservations;
			this.newReservations = newReservations;
			this.licenses = licenses;
		}

	}

	public FeatureLicenseUsageDelta calculateUsage(final List<UsageCalculatorInput> usageCalculatorInputs) {
		final Set<Feature> features = new HashSet<>();
		final List<Reservation> reservations = new ArrayList<>();
		final List<Reservation> newReservations = new ArrayList<>();
		final List<License> licenses = new ArrayList<>();

		for (final UsageCalculatorInput usageCalculatorInput : usageCalculatorInputs) {
			features.add(usageCalculatorInput.feature);
			reservations.addAll(usageCalculatorInput.reservations);
			newReservations.addAll(usageCalculatorInput.newReservations);
			licenses.addAll(usageCalculatorInput.licenses);
		}
		if (features.size() != 1) {
			throw new CLSRuntimeException("More than one feature passed to UsageCalculator!");
		}

		return calculateUsage(features.iterator().next(), groupReservations(reservations),
				groupReservations(newReservations), licenses);
	}

	private List<Reservation> groupReservations(final List<Reservation> reservations) {
		final Map<String, List<Reservation>> mapSerialToReservation = mapSerialToReservations(reservations);

		return combineReservations(mapSerialToReservation);
	}

	private List<Reservation> combineReservations(final Map<String, List<Reservation>> mapSerialToReservation) {
		final List<Reservation> result = new ArrayList<>();
		for (final List<Reservation> reservation : mapSerialToReservation.values()) {
			final Reservation first = reservation.get(0);
			if (reservation.size() > 1) {
				for (final Reservation reservation2 : reservation.subList(1, reservation.size())) {
					first.setCapacity(first.getCapacity() + reservation2.getCapacity());
				}
			}
			result.add(first);
		}
		return result;
	}

	private Map<String, List<Reservation>> mapSerialToReservations(final List<Reservation> reservations) {
		final Map<String, List<Reservation>> mapSerialToReservation = new HashMap<>();
		for (final Reservation reservation : reservations) {
			final String key = reservation.getSerialNumber();
			List<Reservation> reservationsList = mapSerialToReservation.get(key);
			if (reservationsList == null) {
				reservationsList = new ArrayList<>();
				mapSerialToReservation.put(key, reservationsList);
			}
			reservationsList.add(reservation);
		}
		return mapSerialToReservation;
	}

}
