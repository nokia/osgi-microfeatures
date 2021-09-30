/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;

import com.nsn.ood.cls.model.gen.features.Allocation;
import com.nsn.ood.cls.model.gen.features.Allocation.Usage;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.features.Feature.Type;
import com.nsn.ood.cls.model.gen.features.Features;


/**
 * @author marynows
 * 
 */
public class FeatureTestUtil {

	public static Features features(final List<Feature> features) {
		return new Features().withFeatures(features);
	}

	public static Features features(final Feature... features) {
		return new Features().withFeatures(Arrays.asList(features));
	}

	public static List<Feature> featuresList(final Feature... features) {
		return Arrays.asList(features);
	}

	public static Feature feature() {
		return new Feature();
	}

	public static Feature feature(final Long featureCode) {
		return feature().withFeatureCode(featureCode);
	}

	public static Feature featureCapacity(final Long featureCode, final Long capacity) {
		return feature(featureCode).withType(Type.CAPACITY).withCapacity(capacity);
	}

	public static Feature featureCapacity(final Long featureCode, final Long capacity, final Allocation... allocations) {
		return featureCapacity(featureCode, capacity).withAllocations(Arrays.asList(allocations));
	}

	public static Feature featureOnOff(final Long featureCode) {
		return feature(featureCode).withType(Type.ON_OFF);
	}

	public static Feature featureOnOff(final Long featureCode, final Allocation... allocations) {
		return featureOnOff(featureCode).withAllocations(Arrays.asList(allocations));
	}

	public static void assertFeature(final Feature feature, final Long expectedFeatureCode, final Type expectedType,
			final Long expectedCapacity) {
		assertEquals(expectedFeatureCode, feature.getFeatureCode());
		assertEquals(expectedType, feature.getType());
		assertEquals(expectedCapacity, feature.getCapacity());
	}

	public static void assertCapacityFeature(final Feature feature, final Long expectedFeatureCode,
			final Long expectedCapacity) {
		assertFeature(feature, expectedFeatureCode, Type.CAPACITY, expectedCapacity);
	}

	public static void assertOnOffFeature(final Feature feature, final Long expectedFeatureCode) {
		assertFeature(feature, expectedFeatureCode, Type.ON_OFF, null);
	}

	public static void assertFeatureAllocations(final Feature feature, final Allocation... expectedAllocations) {
		final List<Allocation> allocations = feature.getAllocations();

		assertEquals(expectedAllocations.length, allocations.size());
		for (int i = 0; i < expectedAllocations.length; i++) {
			assertEquals(expectedAllocations[i], allocations.get(i));
		}
	}

	public static List<Allocation> allocationsList(final Allocation... allocations) {
		return Arrays.asList(allocations);
	}

	public static Allocation allocation() {
		return new Allocation();
	}

	public static Allocation allocation(final Long capacity) {
		return new Allocation().withCapacity(capacity);
	}

	public static Allocation allocation(final Long capacity, final String licenseURI) {
		return allocation(capacity).withPoolLicense(licenseURI == null ? null : URI.create(licenseURI));
	}

	public static Allocation allocation(final Long capacity, final String licenseURI, final Usage usage,
			final DateTime endDate) {
		return allocation(capacity, licenseURI).withUsage(usage).withEndDate(endDate);
	}
}
