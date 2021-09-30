/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.setting;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.db.ListConditionsQuery;
import com.nsn.ood.cls.core.db.creator.SettingCreator;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.model.internal.Setting;


/**
 * @author marynows
 * 
 */
public class QuerySettings extends ListConditionsQuery<Setting> {
	private final SettingCreator creator;

	public QuerySettings(final Conditions conditions, final ConditionsMapper mapper, final SettingCreator creator)
			throws ConditionProcessingException {
		super("select * from cls.settings", conditions, mapper, null);
		this.creator = creator;
	}

	@Override
	protected Setting handleRow(final ResultSet resultSet) throws SQLException {
		return this.creator.createSetting(resultSet);
	}
}
