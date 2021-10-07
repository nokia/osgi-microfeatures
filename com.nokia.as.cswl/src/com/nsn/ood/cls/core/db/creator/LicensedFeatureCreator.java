/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.creator;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.felix.dm.annotation.api.Component;

import com.nsn.ood.cls.model.internal.LicensedFeature;


/**
 * @author marynows
 * 
 */
@Component(provides = LicensedFeatureCreator.class)
public class LicensedFeatureCreator {

	public LicensedFeature createLicensedFeature(final ResultSet resultSet) throws SQLException {
		return new LicensedFeature()//
				.withFeatureCode(resultSet.getLong("featurecode"))//
				.withFeatureName(resultSet.getString("featurename"))//
				.withCapacityUnit(resultSet.getString("capacityunit"))//
				.withTargetType(resultSet.getString("targettype"))//
				.withTotalCapacity(resultSet.getLong("total"))//
				.withUsedCapacity(resultSet.getLong("used"))//
				.withRemainingCapacity(resultSet.getLong("remaining"));
	}
}
