/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSException;


/**
 * @author marynows
 *
 */
class ReservationsCalculator {
	private static final Logger LOG = LoggerFactory.getLogger(ReservationsCalculator.class);

	private final Converter<Reservation, String> reservation2StringConverter;
	private final Converter<License, String> license2StringConverter;
	private final List<License> licenses;
	private final List<Reservation> reservations;
	private final DateTime reservationTime;

	private final Map<String, License> licensesPerSerialNumber = new HashMap<>();
	private final Map<String, Reservation> newReservations = new LinkedHashMap<>();

	private long requestedCapacity;
	private long totalCapacity;

	ReservationsCalculator(final List<License> licenses, final List<Reservation> reservations,
			final long requestedCapacity, final DateTime reservationTime, 
			final Converter<Reservation, String> reservation2StringConverter,
			final Converter<License, String> license2StringConverter) {
		this.reservation2StringConverter = reservation2StringConverter;
		this.license2StringConverter = license2StringConverter;
		this.licenses = licenses;
		this.reservations = reservations;
		this.requestedCapacity = requestedCapacity;
		this.reservationTime = reservationTime;

		for (final License license : this.licenses) {
			this.licensesPerSerialNumber.put(license.getSerialNumber(), license);
		}
	}

	List<Reservation> calculate() throws CapacityException {
		handleCurrentReservations();
		checkCapacityReleaseStatus();
		if (!allCapacityReserved()) {
			handleNewReservations();
			checkCapacityReservationStatus();
		}
		return new ArrayList<>(this.newReservations.values());
	}

	private void handleCurrentReservations() {
		LOG.debug("Processing current reservations...");
		for (final Reservation reservation : this.reservations) {
			final License license = getLicenseForReservation(reservation);
			if (isPoolLicense(license)) {
				reserveExistingPoolCapacity(reservation);
			} else {
				releaseExistingFloatingPoolCapacity(reservation, license);
			}
		}
	}

	private License getLicenseForReservation(final Reservation reservation) {
		return this.licensesPerSerialNumber.get(reservation.getSerialNumber());
	}

	private boolean isPoolLicense(final License license) {
		return (license == null) || (license.getType() == License.Type.POOL);
	}

	private void reserveExistingPoolCapacity(final Reservation reservation) {
		LOG.debug("Holding pool license reservation: {}", description(reservation));
		this.newReservations.put(reservation.getSerialNumber(), reservation);
		this.requestedCapacity -= reservation.getCapacity();
		this.totalCapacity += reservation.getCapacity();
	}

	private void releaseExistingFloatingPoolCapacity(final Reservation reservation, final License license) {
		LOG.debug("Releasing floating pool license reservation: {}", description(reservation));
		final long newCapacity = Math.max(license.getUsedCapacity() - reservation.getCapacity(), 0);
		license.setUsedCapacity(newCapacity);
	}

	private void checkCapacityReleaseStatus() throws CapacityException {
		if (this.requestedCapacity < 0) {
			LOG.debug("Aborting operation: Cannot release enough capacity");
			throw new CapacityException(this.totalCapacity, true);
		}
	}

	private void handleNewReservations() throws CapacityException {
		LOG.debug("Processing new reservations...");
		for (final License license : this.licenses) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Processing license: {}", description(license));
			}
			final long remainingLicenseCapacity = getRemainingLicenseCapacity(license);
			if (remainingLicenseCapacity > 0) {
				reserveCapacity(license, remainingLicenseCapacity);
			}
			if (allCapacityReserved()) {
				break;
			}
		}
	}

	private long getRemainingLicenseCapacity(final License license) {
		return license.getTotalCapacity() - license.getUsedCapacity();
	}

	private void reserveCapacity(final License license, final long remainingLicenseCapacity) {
		final long capacityToReserve = Math.min(remainingLicenseCapacity, this.requestedCapacity);
		if (!reservationExists(license)) {
			reserveNewCapacity(license, capacityToReserve);
		} else if (capacityToReserve > 0) {
			extendExistingPoolReservation(license, capacityToReserve);
		}
		this.requestedCapacity -= capacityToReserve;
		this.totalCapacity += remainingLicenseCapacity;
	}

	private boolean reservationExists(final License license) {
		return this.newReservations.containsKey(license.getSerialNumber());
	}

	private void reserveNewCapacity(final License license, final long capacityToReserve) {
		final Reservation newReservation = new Reservation()//
				.withSerialNumber(license.getSerialNumber())//
				.withCapacity(capacityToReserve)//
				.withReservationTime(this.reservationTime)//
				.withMode(license.getMode())//
				.withType(license.getType())//
				.withEndDate(license.getEndDate())//
				.withFileName(license.getFileName());
		this.newReservations.put(newReservation.getSerialNumber(), newReservation);
		LOG.debug("Creating new license reservation: {}", description(newReservation));
	}

	private void extendExistingPoolReservation(final License license, final long capacityToReserve) {
		final Reservation reservation = this.newReservations.get(license.getSerialNumber());
		reservation.setCapacity(reservation.getCapacity() + capacityToReserve);
		reservation.setReservationTime(this.reservationTime);
		LOG.debug("Extending pool license reservation: {}", description(reservation));
	}

	private void checkCapacityReservationStatus() throws CapacityException {
		if (!allCapacityReserved()) {
			LOG.debug("Aborting operation: Not enough capacity");
			throw new CapacityException(this.totalCapacity, false);
		}
	}

	private boolean allCapacityReserved() {
		return this.requestedCapacity <= 0;
	}

	private String description(final Reservation reservation) {
		return reservation2StringConverter.convertTo(reservation);
	}

	private String description(final License license) {
		return license2StringConverter.convertTo(license);
	}

	static final class CapacityException extends CLSException {
		private static final long serialVersionUID = 4365158616896337939L;

		private final long capacity;
		private final boolean release;

		CapacityException(final long capacity, final boolean release) {
			this.capacity = capacity;
			this.release = release;
		}

		long getCapacity() {
			return this.capacity;
		}

		boolean isRelease() {
			return this.release;
		}
	}
}
