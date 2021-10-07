/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.util;

import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.operation.util.ReservationsCalculator.CapacityException;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.errors.FeatureError;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSException;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component(provides = ReservationsUtils.class)
@Loggable
public class ReservationsUtils {
	private static final Logger LOG = LoggerFactory.getLogger(ReservationsUtils.class);

	@ServiceDependency(filter = "(&(from=reservation)(to=string))")
	private Converter<Reservation, String> reservation2StringConverter;
	@ServiceDependency(filter = "(&(from=license)(to=string))")
	private Converter<License, String> license2StringConverter;

	public boolean isCalculationsNeeded(final Feature feature, final List<Reservation> reservations) {
		verifyParameters(feature, reservations);

		final long requestedCapacity = getRequestedCapacity(feature);
		LOG.debug("Requested capacity: {}", requestedCapacity);

		final long reservedCapacity = getReservedCapacity(reservations);
		LOG.debug("Reserved capacity: {}", reservedCapacity);

		return requestedCapacity != reservedCapacity;
	}

	private long getReservedCapacity(final List<Reservation> reservations) {
		long reservedCapacity = 0L;
		for (final Reservation reservation : reservations) {
			reservedCapacity += reservation.getCapacity();
		}
		return reservedCapacity;
	}

	public List<Reservation> calculate(final Client client, final Feature feature, final List<License> licenses,
			final List<Reservation> reservations) throws CalculationException {
		verifyParameters(client, feature, licenses, reservations);
		verifyLicenses(feature, licenses);

		try {
			final long requestedCapacity = getRequestedCapacity(feature);
			LOG.debug("Requested capacity: {}", requestedCapacity);
			final ReservationsCalculator calculator = createCalculator(licenses, reservations, requestedCapacity,
					DateTime.now());
			return supplementMissingData(calculator.calculate(), client, feature);
		} catch (final CapacityException e) {
			throw new CalculationException(feature, e);
		}
	}

	private long getRequestedCapacity(final Feature feature) {
		return (feature.getType() == Feature.Type.CAPACITY ? feature.getCapacity() : 1L);
	}

	protected ReservationsCalculator createCalculator(final List<License> licenses,
			final List<Reservation> reservations, final long requestedCapacity, final DateTime reservationTime) {
		return new ReservationsCalculator(licenses, reservations, requestedCapacity, reservationTime, reservation2StringConverter, license2StringConverter);
	}

	private List<Reservation> supplementMissingData(final List<Reservation> reservations, final Client client,
			final Feature feature) {
		for (final Reservation reservation : reservations) {
			reservation.setClientId(client.getClientId());
			reservation.setFeatureCode(feature.getFeatureCode());
		}
		return reservations;
	}

	private void verifyParameters(final Object... objects) {
		for (final Object object : objects) {
			if (object == null) {
				throw new CLSIllegalArgumentException("Input parameter cannot be null");
			}
		}
	}

	private void verifyLicenses(final Feature feature, final List<License> licenses) throws CalculationException {
		if (licenses.isEmpty() && ((feature.getType() == Feature.Type.ON_OFF) || (feature.getCapacity() > 0))) {
			LOG.debug("Aborting operation: No licenses found");
			throw new CalculationException(feature, new ReservationsCalculator.CapacityException(0L, false));
		}
	}

	public static final class CalculationException extends CLSException {
		private static final long serialVersionUID = 959503484400430490L;

		private final FeatureError error;
		private final ReservationErrorType type;

		private CalculationException(final Feature feature, final CapacityException e) {
			super(null, e);
			final long capacity = e.getCapacity();
			final boolean release = e.isRelease();

			this.error = new FeatureError().withFeatureCode(feature.getFeatureCode());
			if (release) {
				this.error.setCapacity(capacity);
				this.type = ReservationErrorType.RELEASE;
			} else if (feature.getType() == Feature.Type.CAPACITY) {
				this.error.setRequestedCapacity(feature.getCapacity());
				this.error.setRemainingCapacity(capacity);
				this.type = ReservationErrorType.CAPACITY;
			} else {
				this.type = ReservationErrorType.ON_OFF;
			}
		}

		public FeatureError getError() {
			return this.error;
		}

		public ReservationErrorType getErrorType() {
			return this.type;
		}
	}
}
