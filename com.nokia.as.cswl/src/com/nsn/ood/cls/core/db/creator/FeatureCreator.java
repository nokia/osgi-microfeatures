/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.creator;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;

import com.nsn.ood.cls.model.gen.features.Allocation;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
@Component(provides = FeatureCreator.class)
public class FeatureCreator {
	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	
	@ServiceDependency(filter = "(&(from=featureType)(to=integer))")
	private Converter<Feature.Type, Integer> featureType2IntegerConverter;
	
	@ServiceDependency(filter = "(&(from=allocationUsage)(to=integer))")
	private Converter<Allocation.Usage, Integer> allocationUsage2IntegerConverter;

	public Feature createFeature(final ResultSet resultSet) throws SQLException {
		return new Feature()//
				.withFeatureCode(resultSet.getLong("featurecode"))//
				.withType(featureType2IntegerConverter.convertFrom(resultSet.getInt("mode")));
	}

	public void addAllocation(final Feature feature, final ResultSet resultSet) throws SQLException {
		final Allocation allocation = new Allocation()//
				.withPoolLicense(URI.create(resultSet.getString("serialnumber")))//
				.withUsage(allocationUsage2IntegerConverter.convertFrom(resultSet.getInt("type")))//
				.withEndDate(timestamp2DateTimeConverter.convertTo(resultSet.getTimestamp("enddate")));
		feature.getAllocations().add(allocation);

		if (feature.getType() == Feature.Type.CAPACITY) {
			final long capacity = resultSet.getLong("capacity");
			allocation.setCapacity(capacity);
			final long featureCapacity = (feature.getCapacity() == null ? 0L : feature.getCapacity());
			feature.setCapacity(featureCapacity + capacity);
		}
	}
}
