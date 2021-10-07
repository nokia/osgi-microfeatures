/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.util.DescriptionBuilder;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * Reservation <-> String
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "reservation")
@Property(name = "to", value = "string")
public class Reservation2StringConverter implements Converter<Reservation, String> {

	@Override
	public String convertTo(final Reservation reservation) {
		if (reservation == null) {
			throw new CLSIllegalArgumentException("Reservation must not be null");
		}

		return new DescriptionBuilder()//
				.append("capacity", reservation.getCapacity())//
				.append("serialNumber", reservation.getSerialNumber())//
				.append("reservationTime", reservation.getReservationTime())//
				.append("fileName", reservation.getFileName())//
				.append("mode", reservation.getMode())//
				.append("type", reservation.getType())//
				.append("endDate", reservation.getEndDate()).build();
	}

	@Override
	public Reservation convertFrom(final String string) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
