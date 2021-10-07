/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.activity;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.db.ListConditionsQuery;
import com.nsn.ood.cls.core.db.creator.ActivityCreator;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.model.internal.ActivityDetail;


/**
 * @author marynows
 * 
 */
public class QueryActivityDetails extends ListConditionsQuery<ActivityDetail> {
	private final ActivityCreator creator;

	public QueryActivityDetails(final Conditions conditions, final ConditionsMapper mapper,
			final ActivityCreator creator) throws ConditionProcessingException {
		super("select * from cls.activitydetails", conditions, mapper, "activityId");
		this.creator = creator;
	}

	@Override
	protected ActivityDetail handleRow(final ResultSet resultSet) throws SQLException {
		return this.creator.createActivityDetail(resultSet);
	}
}
