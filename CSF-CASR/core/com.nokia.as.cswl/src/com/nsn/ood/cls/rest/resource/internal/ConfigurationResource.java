/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.internal.ActivityService;
import com.nsn.ood.cls.core.service.internal.ConfigurationService;
import com.nsn.ood.cls.model.CLSMediaType;
import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.model.internal.SettingKey;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.rest.BasicSecure;
import com.nsn.ood.cls.rest.resource.BaseResource;
import com.nsn.ood.cls.rest.resource.CLSApplication;
import com.nsn.ood.cls.util.CollectionUtils;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;

import io.swagger.v3.oas.annotations.Operation;


/**
 * @author wro50095
 *
 */
@Component(provides = ConfigurationResource.class)
@Path(CLSApplication.INTERNAL + "/configurations")
@Produces({
		CLSMediaType.APPLICATION_CLS_JSON, CLSMediaType.APPLICATION_ERROR_JSON })
@Loggable(duration = true)
public class ConfigurationResource {
	private static final Logger LOG = LoggerFactory.getLogger(ConfigurationResource.class);
	private static final String LOG_UPDATE_SETTINGS = "Update settings";

	@ServiceDependency
	private ConfigurationService configurationService;
	@ServiceDependency
	private ActivityService activityService;
	
	@ServiceDependency
	private BaseResource baseResource;
	
	@ServiceDependency(filter = "(&(from=uriInfo)(to=conditions))")
	private Converter<UriInfo, Conditions> uriInfo2ConditionsConverter;
	
	@ServiceDependency(filter = "(&(from=errorException)(to=error))")
	private Converter<ErrorException, Error> errorException2ErrorConverter;
	
	@ServiceDependency(filter = "(&(from=setting)(to=string))")
	private Converter<Setting, String> setting2StringConverter;

	@Start
	public void start() {
		baseResource.init(LOG, "configurations");
	}

	@GET
	@Operation(hidden = true)
	public Response getSettings(@Context final UriInfo uriInfo) {
		final Conditions conditions = uriInfo2ConditionsConverter.convertTo(uriInfo);

		try {
			final MetaDataList<Setting> settings = this.configurationService.getSettings(conditions.clone());

			return getSettingsResponse(baseResource.getResourceFactory()//
					.metaData(baseResource.links(conditions, settings.getMetaData().getFiltered()), settings.getMetaData())//
					.settings(settings.getList()).build());
		} catch (final ServiceException e) {
			return baseResource.exceptionResponse(e);
		}
	}

	@GET
	@Path("{settingKey}")
	@Operation(hidden = true)
	public Response getSetting(@PathParam("settingKey") final String settingKey) {
		final Conditions conditions = ConditionsBuilder.createAndSkipMetaData()
				.equalFilter("key", getSettingKey(settingKey).toString()).build();

		try {
			final MetaDataList<Setting> settings = this.configurationService.getSettings(conditions);

			return getSettingsResponse(baseResource.getResourceFactory()//
					.selfLink(baseResource.link(settingKey))//
					.settings(settings.getList()).build());
		} catch (final ServiceException e) {
			return baseResource.exceptionResponse(e);
		}
	}

	@PUT
	@Consumes(CLSMediaType.APPLICATION_CLS_JSON)
	@BasicSecure
	@Operation(hidden = true)
	public Response setSettings(final List<Setting> settings) {
		verifySettings(settings);
		return updateSettings(settings);
	}

	@PUT
	@Path("{settingKey}")
	@Consumes(CLSMediaType.APPLICATION_CLS_JSON)
	@BasicSecure
	@Operation(hidden = true)
	public Response setSetting(@PathParam("settingKey") final String settingKey, final Setting setting) {
		final Setting verifiedSetting = verifySetting(setting, getSettingKey(settingKey));
		return updateSettings(Arrays.asList(verifiedSetting));
	}

	private Response updateSettings(final List<Setting> settings) {
		final List<ErrorException> errorExceptions = new ArrayList<>();

		baseResource.logInit(LOG_UPDATE_SETTINGS, settings.stream().map(setting2StringConverter::convertTo).collect(Collectors.toList()).toString());
		try {
			this.configurationService.setSettings(settings);
			baseResource.logSuccess(LOG_UPDATE_SETTINGS);
		} catch (final ServiceException e) {
			baseResource.logFailure(LOG_UPDATE_SETTINGS, e);
			errorExceptions.addAll(e.getExceptions());
		}

		addActivity(settings, errorExceptions);
		return createResponse(errorExceptions);
	}

	private void addActivity(final List<Setting> settings, final List<ErrorException> errorExceptions) {
		baseResource.logInit(BaseResource.LOG_ADD_ACTIVITY, LOG_UPDATE_SETTINGS);
		try {
			this.activityService.addSettingUpdateActivity(settings, errorExceptions);
			baseResource.logSuccess(BaseResource.LOG_ADD_ACTIVITY);
		} catch (final ServiceException e) {
			baseResource.logFailure(BaseResource.LOG_ADD_ACTIVITY, e);
		}
	}

	private Response createResponse(final List<ErrorException> errorExceptions) {
		if (errorExceptions.isEmpty()) {
			return baseResource.getResponseFactory().noContent().build();
		} else {
			return baseResource.getRestUtil().errorResponse(Status.BAD_REQUEST,
				   errorExceptions.stream().map(errorException2ErrorConverter::convertTo).collect(Collectors.toList()));
		}
	}

	private void verifySettings(final List<Setting> settings) {
		if (CollectionUtils.isEmpty(settings)) {
			throw baseResource.getViolationFactory().exception("configurations.oneOrMoreSettings");
		}
		for (int i = 0; i < settings.size(); i++) {
			validateSetting(settings.get(i), i);
		}
	}

	private Setting verifySetting(final Setting setting, final SettingKey key) {
		if (setting.getKey() != null) {
			throw baseResource.getViolationFactory().settingKeyException("configurations.unexpectedKey", setting.getKey());
		}
		setting.setKey(key);
		validateSetting(setting, null);
		return setting;
	}

	private void validateSetting(final Setting setting, final Integer index) {
		if (setting.getValue() instanceof String) {
			final String value = (String) setting.getValue();
			if (value.length() > Setting.VALUE_MAX_LENGTH) {
				throw baseResource.getViolationFactory().settingValueException("configurations.valueTooLong", index, value);
			}
		}
		try {
			setting.validate();
		} catch (final IllegalArgumentException e) {
			logForDebug(e);
			throw baseResource.getViolationFactory().settingValueException("configurations.invalidValue", index, setting.getValue());
		}
	}

	private SettingKey getSettingKey(final String value) {
		try {
			return SettingKey.fromValue(value);
		} catch (final IllegalArgumentException e) {
			logForDebug(e);
			throw baseResource.getViolationFactory().valueException("configurations.unknownSetting", value);
		}
	}

	@GET
	@Path("targetId")
	@Operation(hidden = true)
	public Response getTargetId() {
		final String targetId = this.configurationService.getTargetId();

		final Resource resource = baseResource.getResourceFactory().selfLink(baseResource.link("targetId")).embedded("targetId", targetId)
				.build();
		return getSettingsResponse(resource);
	}

	private Response getSettingsResponse(final Resource resource) {
		return baseResource.getResponseFactory().ok(resource).build();
	}

	private void logForDebug(final IllegalArgumentException e) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Error occured during configuration processing", e);
		}
	}
}
