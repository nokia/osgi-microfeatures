/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;

import com.nokia.licensing.dtos.AddnColumns.LicenseMode;
import com.nokia.licensing.dtos.AddnColumns.LicenseType;
import com.nokia.licensing.dtos.FeatureInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.dtos.TargetSystem;
import com.nsn.ood.cls.model.gen.licenses.Feature;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.gen.licenses.Target;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * Stored license <-> License
 *
 * @author marynows
 *
 */
@Component
@Property(name = "from", value = "storedLicense")
@Property(name = "to", value = "license")
public class StoredLicense2LicenseConverter implements Converter<StoredLicense, License> {
	
	@ServiceDependency(filter = "(&(from=date)(to=dateTime))")
	private Converter<Date, DateTime> date2DateTimeConverter;
	
	@ServiceDependency(filter = "(&(from=storedLicenseMode)(to=licenseMode))")
	private Converter<LicenseMode, License.Mode> storedLicenseMode2LicenseModeConverter;
	
	@ServiceDependency(filter = "(&(from=storedLicenseType)(to=licenseType))")
	private Converter<LicenseType, License.Type> storedLicenseType2LicenseTypeConverter;
	
	@ServiceDependency(filter = "(&(from=featureInfo)(to=feature))")
	private Converter<FeatureInfo, Feature> featureInfo2Feature;
	
	@ServiceDependency(filter = "(&(from=targetSystem)(to=target))")
	private Converter<TargetSystem, Target> targetSystem2TargetConverter;	
	
	@Override
	public License convertTo(final StoredLicense storedLicense) {
		if (storedLicense == null) {
			throw new CLSIllegalArgumentException("Stored license must not be null");
		}
		
		return new License()//
				.withCapacityUnit(storedLicense.getCapacityUnit())//
				.withCode(storedLicense.getLicenseCode())//
				.withEndDate(date2DateTimeConverter.convertTo(storedLicense.getEndTime()))//
				.withFileName(storedLicense.getLicenseFileName())//
				.withMode(storedLicenseMode2LicenseModeConverter.convertTo(storedLicense.getLicenseMode()))//
				.withName(storedLicense.getLicenseName())//
				.withSerialNumber(storedLicense.getSerialNbr())//
				.withStartDate(date2DateTimeConverter.convertTo(storedLicense.getStartTime()))//
				.withTargetType(storedLicense.getTargetNEType())//
				.withTotalCapacity(calculateCapacity(storedLicense.getMaxValue()))//
				.withType(storedLicenseType2LicenseTypeConverter.convertTo(storedLicense.getLicenseType()))//
				.withTargets(convertTargets(storedLicense.getTargetIds()))//
				.withFeatures(convertFeatures(storedLicense.getFeatureInfoList()))//
				.withUsedCapacity(0L);
	}

	private long calculateCapacity(final long maxValue) {
		if (maxValue < 0L) {
			return Long.MAX_VALUE;
		}
		return maxValue;
	}

	private List<Feature> convertFeatures(final List<FeatureInfo> storedFeatures) {
		if (storedFeatures == null) {
			return null;
		}
		
		List<Feature> features = storedFeatures.stream()
									.map(featureInfo2Feature::convertTo)
									.collect(Collectors.toList());

		return features.isEmpty() ? null : features;
	}

	private List<Target> convertTargets(final List<TargetSystem> storedTargets) {
		if (storedTargets == null) {
			return null;
		}
		
		List<Target> targets = storedTargets.stream()
							      .map(targetSystem2TargetConverter::convertTo)
							      .collect(Collectors.toList());

		return targets.isEmpty() ? null : targets;
	}

	@Override
	public StoredLicense convertFrom(final License license) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
