/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.operation.ActivityCreateOperation;
import com.nsn.ood.cls.core.operation.ActivityDetailRetrieveOperation;
import com.nsn.ood.cls.core.operation.ActivityRetrieveOperation;
import com.nsn.ood.cls.core.operation.exception.CreateException;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Activity;
import com.nsn.ood.cls.model.internal.Activity.OperationType;
import com.nsn.ood.cls.model.internal.Activity.Result;
import com.nsn.ood.cls.model.internal.ActivityDetail;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = ActivityService.class)
@Loggable
public class ActivityService {
	@ServiceDependency
	private ActivityRetrieveOperation activityRetrieveOperation;
	@ServiceDependency
	private ActivityDetailRetrieveOperation activityDetailRetrieveOperation;
	@ServiceDependency
	private ActivityCreateOperation activityCreateOperation;
	@ServiceDependency
	private ServiceExceptionFactory serviceExceptionFactory;
	@ServiceDependency(filter = "(&(from=errorException)(to=activityDetail))")
	private Converter<ErrorException, ActivityDetail> errorException2ActivityDetailConverter;
	@ServiceDependency(filter = "(&(from=license)(to=activityDetail))")
	private Converter<License, ActivityDetail> license2ActivityDetailConverter;
	@ServiceDependency(filter = "(&(from=setting)(to=activityDetail))")
	private Converter<Setting, ActivityDetail> setting2ActivityDetailConverter;

	public MetaDataList<Activity> getActivities(final Conditions conditions) throws ServiceException {
		try {
			return this.activityRetrieveOperation.getList(conditions);
		} catch (final RetrieveException e) {
			throw this.serviceExceptionFactory.violation(e, e.getError());
		}
	}

	public List<String> getActivityFilterValues(final String filterName) throws ServiceException {
		try {
			return this.activityRetrieveOperation.getFilterValues(filterName,//
					ConditionsBuilder.createAndSkipMetaData().build());
		} catch (final RetrieveException e) {
			throw this.serviceExceptionFactory.violation(e, e.getError());
		}
	}

	public MetaDataList<ActivityDetail> getActivityDetails(final Long activityId, final Conditions conditions)
			throws ServiceException {
		final Conditions conditionsWithActivityId = ConditionsBuilder.use(conditions)
				.equalFilter("activityId", Objects.toString(activityId, "")).build();

		try {
			return this.activityDetailRetrieveOperation.getList(conditionsWithActivityId);
		} catch (final RetrieveException e) {
			throw this.serviceExceptionFactory.violation(e, e.getError());
		}
	}

	public List<String> getActivityDetailFilterValues(final Long activityId, final String filterName)
			throws ServiceException {
		try {
			return this.activityDetailRetrieveOperation.getFilterValues(filterName, ConditionsBuilder
					.createAndSkipMetaData().equalFilter("activityId", Objects.toString(activityId, "")).build());
		} catch (final RetrieveException e) {
			throw this.serviceExceptionFactory.violation(e, e.getError());
		}
	}

	public void addLicenseInstallActivity(final List<License> licenses, final List<ErrorException> errors)
			throws ServiceException {
		final Activity activity = createActivity(OperationType.LICENSE_INSTALL, licenses, errors);
		final List<ActivityDetail> activityDetails = createLicensesActivityDetails(licenses, errors);
		addActivity(activity, activityDetails);
	}

	public void addLicenseCancelActivity(final List<License> licenses, final List<ErrorException> errors)
			throws ServiceException {
		final Activity activity = createActivity(OperationType.LICENSE_CANCEL, licenses, errors);
		final List<ActivityDetail> activityDetails = createLicensesActivityDetails(licenses, errors);
		addActivity(activity, activityDetails);
	}

	private Activity createActivity(final OperationType operationType, final List<License> licenses,
			final List<ErrorException> errors) {
		return createActivity(operationType,//
				errors.isEmpty() ? Result.SUCCESS : (licenses.isEmpty() ? Result.FAILURE : Result.PARTIAL));
	}

	private List<ActivityDetail> createLicensesActivityDetails(final List<License> licenses,
			final List<ErrorException> errors) {
		final List<ActivityDetail> activityDetails = new ArrayList<>();
		List<ActivityDetail> convertedLicenses = licenses.stream().map(license2ActivityDetailConverter::convertTo).collect(Collectors.toList());
		List<ActivityDetail> convertedErrors = errors.stream().map(errorException2ActivityDetailConverter::convertTo).collect(Collectors.toList());
		activityDetails.addAll(convertedLicenses);
		activityDetails.addAll(convertedErrors);
		return activityDetails;
	}

	public void addSettingUpdateActivity(final List<Setting> settings, final List<ErrorException> errors)
			throws ServiceException {
		final Activity activity = createActivity(OperationType.SETTING_UPDATE,//
				errors.isEmpty() ? Result.SUCCESS : Result.FAILURE);
		final List<ActivityDetail> activityDetails = createSettingsActivityDetails(settings, errors);
		addActivity(activity, activityDetails);
	}

	private List<ActivityDetail> createSettingsActivityDetails(final List<Setting> settings,
			final List<ErrorException> errors) {
		final List<ActivityDetail> activityDetails = new ArrayList<>();
		if (errors.isEmpty()) {
			List<ActivityDetail> convertedSettings = settings.stream().map(setting2ActivityDetailConverter::convertTo).collect(Collectors.toList());
			activityDetails.addAll(convertedSettings);
		} else {
			List<ActivityDetail> convertedErrors = errors.stream().map(errorException2ActivityDetailConverter::convertTo).collect(Collectors.toList());
			activityDetails.addAll(convertedErrors);
		}
		return activityDetails;
	}

	private Activity createActivity(final OperationType operationType, final Result result) {
		return new Activity().withOperationType(operationType).withResult(result);
	}

	private void addActivity(final Activity activity, final List<ActivityDetail> activityDetails)
			throws ServiceException {
		try {
			this.activityCreateOperation.createActivity(activity, activityDetails);
		} catch (final CreateException e) {
			throw this.serviceExceptionFactory.error(ErrorCode.ACTIVITY_CREATION_FAIL, e);
		}
	}
}
