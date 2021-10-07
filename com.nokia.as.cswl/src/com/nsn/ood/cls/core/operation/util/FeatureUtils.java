/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.dm.annotation.api.Component;

import com.nsn.ood.cls.model.gen.features.Allocation;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Loggable
@Component(provides = FeatureUtils.class)
public class FeatureUtils {

	public Feature createFeatureWithAllocations(final Feature feature, final List<Reservation> reservations) {
		final List<Allocation> allocations = new ArrayList<>();
		for (final Reservation reservation : reservations) {
			allocations.add(createAllocation(feature, reservation));
		}

		return feature.withAllocations(allocations);
	}

	private Allocation createAllocation(final Feature feature, final Reservation reservation) {
		final Allocation allocation = new Allocation().withPoolLicense(URI.create(reservation.getSerialNumber()));
		if (feature.getType() == Feature.Type.CAPACITY) {
			allocation.setCapacity(reservation.getCapacity());
		}
		allocation.setUsage(convertTypeToUsage(reservation.getType()));
		allocation.setEndDate(reservation.getEndDate());
		return allocation;
	}

	private Allocation.Usage convertTypeToUsage(final License.Type type) {
		if (type == License.Type.FLOATING_POOL) {
			return Allocation.Usage.FLOATING_POOL;
		} else if (type == License.Type.POOL) {
			return Allocation.Usage.POOL;
		}
		return null;
	}
}
