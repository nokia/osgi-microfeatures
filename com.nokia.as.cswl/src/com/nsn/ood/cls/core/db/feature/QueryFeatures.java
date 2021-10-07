/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.feature;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.db.MapConditionsQuery;
import com.nsn.ood.cls.core.db.creator.FeatureCreator;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.model.gen.features.Feature;


/**
 * @author marynows
 * 
 */
public class QueryFeatures extends MapConditionsQuery<Long, Feature> {
	private final FeatureCreator creator;

	public QueryFeatures(final Conditions conditions, final ConditionsMapper mapper, final FeatureCreator creator)
			throws ConditionProcessingException {
		super("select * from cls.reservations", conditions, mapper, "clientId");
		this.creator = creator;
	}

	@Override
	protected void handleRow(final ResultSet resultSet) throws SQLException {
		Feature feature = this.creator.createFeature(resultSet);
		if (contains(feature.getFeatureCode())) {
			feature = get(feature.getFeatureCode());
		} else {
			put(feature.getFeatureCode(), feature);
		}
		this.creator.addAllocation(feature, resultSet);
	}
}
