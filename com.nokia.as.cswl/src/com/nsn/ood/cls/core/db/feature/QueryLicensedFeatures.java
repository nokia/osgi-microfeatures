/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.feature;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.db.ListConditionsQuery;
import com.nsn.ood.cls.core.db.creator.LicensedFeatureCreator;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.model.internal.LicensedFeature;


/**
 * @author marynows
 * 
 */
public class QueryLicensedFeatures extends ListConditionsQuery<LicensedFeature> {
	private final LicensedFeatureCreator creator;

	public QueryLicensedFeatures(final Conditions conditions, final ConditionsMapper mapper,
			final LicensedFeatureCreator creator) throws ConditionProcessingException {
		super("select * from cls.features", conditions, mapper, null);
		this.creator = creator;
	}

	@Override
	protected LicensedFeature handleRow(final ResultSet resultSet) throws SQLException {
		return this.creator.createLicensedFeature(resultSet);
	}
}
