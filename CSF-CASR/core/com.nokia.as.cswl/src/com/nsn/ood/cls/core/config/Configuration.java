/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.config;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.operation.SettingRetrieveOperation;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.model.internal.SettingKey;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.util.CollectionUtils;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = Configuration.class)
@Loggable
public class Configuration {
	private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

	@ServiceDependency
	private SettingRetrieveOperation settingRetrieveOperation;

	public Long getDefaultFloatingReleaseTime() {
		return getSpecificSetting(SettingKey.FLOATING_RELEASE_TIME);
	}

	public Long getExpiringLicensesThreshold() {
		return getSpecificSetting(SettingKey.EXPIRING_LICENSES_THRESHOLD);
	}

	public boolean isEmailNotificationsEnabled() {
		return getSpecificSetting(SettingKey.EMAIL_NOTIFICATIONS);
	}

	public String getEmailServer() {
		return getSpecificSetting(SettingKey.EMAIL_SERVER);
	}

	public String getEmailSubjectPrefix() {
		return getSpecificSetting(SettingKey.EMAIL_SUBJECT);
	}

	public String getEmailSender() {
		return getSpecificSetting(SettingKey.EMAIL_SENDER);
	}

	public String getEmailRecipients() {
		return getSpecificSetting(SettingKey.EMAIL_RECIPIENTS);
	}

	public Long getCapacityThreshold() {
		return getSpecificSetting(SettingKey.CAPACITY_THRESHOLD);
	}

	@SuppressWarnings("unchecked")
	private <T> T getSpecificSetting(final SettingKey key) {
		try {
			final MetaDataList<Setting> setting = this.settingRetrieveOperation.getList(ConditionsBuilder
					.createAndSkipMetaData().equalFilter("key", key.toString()).build());
			if (CollectionUtils.isNotEmpty(setting.getList())) {
				return (T) setting.getList().get(0).getValue();
			}
		} catch (final RetrieveException e) {
			LOG.warn("Cannot retrieve setting", e);
		}
		return (T) key.defaultValue();
	}
}
