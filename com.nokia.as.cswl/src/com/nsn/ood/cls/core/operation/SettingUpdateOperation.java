/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.sql.SQLException;
import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.Update;
import com.nsn.ood.cls.core.db.setting.UpdateSettings;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = SettingUpdateOperation.class)
@Loggable
public class SettingUpdateOperation {
	private static final Logger LOG = LoggerFactory.getLogger(SettingUpdateOperation.class);

	@ServiceDependency(filter = "(name=update)")
	private StatementExecutor<Update> updateExecutor;

	public void updateSettings(final List<Setting> settings) throws UpdateException {
		final UpdateSettings update = createSettingsUpdate(settings);
		try {
			this.updateExecutor.execute(update);
		} catch (final SQLException e) {
			LOG.error(LogMessage.UPDATE_FAIL, e);
			throw new UpdateException(e, update.getIndex());
		}
	}

	protected UpdateSettings createSettingsUpdate(final List<Setting> settings) {
		return new UpdateSettings(settings);
	}
}
