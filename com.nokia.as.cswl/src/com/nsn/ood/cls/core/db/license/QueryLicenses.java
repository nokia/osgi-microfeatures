/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.license;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.db.ListConditionsQuery;
import com.nsn.ood.cls.core.db.creator.LicenseCreator;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.model.gen.licenses.License;


/**
 * @author marynows
 * 
 */
public class QueryLicenses extends ListConditionsQuery<License> {
	private final LicenseCreator creator;

	public QueryLicenses(final Conditions conditions, final ConditionsMapper mapper, final LicenseCreator creator)
			throws ConditionProcessingException {
		super("select * from cls.stored_licenses", conditions, mapper, null);
		this.creator = creator;
	}

	@Override
	protected License handleRow(final ResultSet resultSet) throws SQLException {
		return this.creator.createLicense(resultSet);
	}
}
