/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.internal;

import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.core.audit.AuditLog;
import com.nsn.ood.cls.core.audit.AuditLogType;
import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.operation.SettingRetrieveOperation;
import com.nsn.ood.cls.core.operation.SettingUpdateOperation;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.core.platform.PlatformPreferences;
import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = ConfigurationService.class)
@Loggable
public class ConfigurationService {
	@ServiceDependency
	private SettingRetrieveOperation settingRetrieveOperation;
	@ServiceDependency
	private SettingUpdateOperation settingUpdateOperation;
	@ServiceDependency
	private PlatformPreferences platformPreferences;
	@ServiceDependency
	private ServiceExceptionFactory serviceExceptionFactory;

	public String getTargetId() {
		return this.platformPreferences.getTargetId();
	}

	public MetaDataList<Setting> getSettings(final Conditions conditions) throws ServiceException {
		try {
			return this.settingRetrieveOperation.getList(conditions);
		} catch (final RetrieveException e) {
			throw this.serviceExceptionFactory.violation(e, e.getError());
		}
	}

	@AuditLog(AuditLogType.CHANGE_SETTING)
	public void setSettings(final List<Setting> settings) throws ServiceException {
		try {
			this.settingUpdateOperation.updateSettings(settings);
		} catch (final UpdateException e) {
			final Setting setting = settings.get(e.getIndex());
			throw this.serviceExceptionFactory.setting(ErrorCode.CONFIGURATION_UPDATE_FAIL, e, setting);
		}
	}
}
