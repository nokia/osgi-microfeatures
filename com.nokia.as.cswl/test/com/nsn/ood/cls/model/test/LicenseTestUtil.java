/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.test;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;

import com.nsn.ood.cls.model.gen.licenses.Feature;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.gen.licenses.License.Mode;
import com.nsn.ood.cls.model.gen.licenses.License.Type;
import com.nsn.ood.cls.model.gen.licenses.Target;


/**
 * @author marynows
 * 
 */
public class LicenseTestUtil {

	public static List<License> licensesList(final License... licenses) {
		return Arrays.asList(licenses);
	}

	public static License license() {
		return new License();
	}

	public static License license(final String serialNumber) {
		return license().withSerialNumber(serialNumber);
	}

	public static License license(final Long totalCapacity, final Long usedCapacity) {
		return license().withTotalCapacity(totalCapacity).withUsedCapacity(usedCapacity);
	}

	public static License license(final Long totalCapacity, final Long usedCapacity, final List<Feature> features,
			final List<Target> targets) {
		return license().withTotalCapacity(totalCapacity).withUsedCapacity(usedCapacity).withFeatures(features)
				.withTargets(targets);
	}

	public static License license(final String serialNumber, final Long totalCapacity, final Long usedCapacity) {
		return license(serialNumber).withTotalCapacity(totalCapacity).withUsedCapacity(usedCapacity);
	}

	public static License license(final String serialNumber, final Long totalCapacity, final Long usedCapacity,
			final Type type, final DateTime endDate) {
		return license(serialNumber, totalCapacity, usedCapacity).withType(type).withEndDate(endDate);
	}

	public static License license(final String serialNumber, final Type type, final DateTime endDate) {
		return license(serialNumber).withType(type).withEndDate(endDate);
	}

	public static License license(final String serialNumber, final Type type, final Mode mode) {
		return license(serialNumber).withType(type).withMode(mode);
	}

	public static License license(final String serialNumber, final String fileName, final Type type) {
		return license(serialNumber).withFileName(fileName).withType(type);
	}

	public static License license(final String serialNumber, final Type type, final Mode mode, final DateTime endDate,
			final String fileName) {
		return license(serialNumber, type, mode).withEndDate(endDate).withFileName(fileName);
	}

	public static License license(final String serialNumber, final Type type, final Mode mode, final DateTime endDate,
			final Long totalCapacity, final Long usedCapacity, final String fileName, final String targetType) {
		return license(serialNumber, type, mode, endDate, fileName).withTotalCapacity(totalCapacity)
				.withUsedCapacity(usedCapacity).withTargetType(targetType);
	}

	public static License license(final String serialNumber, final Type type, final Mode mode,
			final DateTime startDate, final DateTime endDate, final Long totalCapacity, final Long usedCapacity,
			final String capacityUnit, final String code, final String name, final String fileName,
			final String targetType) {
		return license(serialNumber, type, mode, endDate, totalCapacity, usedCapacity, fileName, targetType)
				.withStartDate(startDate).withCapacityUnit(capacityUnit).withCode(code).withName(name);
	}

	public static License license(final List<Target> targets, final List<Feature> features) {
		return license().withTargets(targets).withFeatures(features);
	}

	public static License license(final String fileName, final List<Feature> features) {
		return license().withFileName(fileName).withFeatures(features);
	}

	public static List<Target> targetsList(final Target... targets) {
		return Arrays.asList(targets);
	}

	public static Target target() {
		return new Target();
	}

	public static Target target(final String targetId) {
		return target().withTargetId(targetId);
	}

	public static List<Feature> featuresList(final Feature... features) {
		return Arrays.asList(features);
	}

	public static Feature feature() {
		return new Feature();
	}

	public static Feature feature(final Long featureCode, final String featureName) {
		return new Feature().withFeatureCode(featureCode).withFeatureName(featureName);
	}
}
