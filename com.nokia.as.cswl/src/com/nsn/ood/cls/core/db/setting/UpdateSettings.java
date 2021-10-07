/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.setting;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import com.nsn.ood.cls.core.db.IterableUpdate;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author marynows
 * 
 */
public class UpdateSettings extends IterableUpdate<Setting> {
	private static final Logger LOG = LoggerFactory.getLogger(UpdateSettings.class);
	private static final int VALUE = 1;
	private static final int KEY = 2;

	public UpdateSettings(final List<Setting> settings) {
		super("update cls.settings set value = ? where settingskey = ?", settings);
	}

	@Override
	protected void prepareRow(final PreparedStatement statement, final Setting setting) throws SQLException {
		statement.setString(VALUE, Strings.emptyToNull(Objects.toString(setting.getValue(), null)));
		statement.setString(KEY, setting.getKey().name());
		LOG.info("####################    QUERY SENT    #####################");
		LOG.info("#######  "+ sql() +"   #####");
		LOG.info("#########################################");

	}

	@Override
	public void handle(final int affectedRows) throws SQLException {
		if (affectedRows == 0) {
			throw new SQLException("Setting does not exist");
		}
	}
}
