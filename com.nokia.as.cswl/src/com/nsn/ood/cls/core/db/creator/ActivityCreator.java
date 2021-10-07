/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.creator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;

import com.nsn.ood.cls.model.internal.Activity;
import com.nsn.ood.cls.model.internal.Activity.OperationType;
import com.nsn.ood.cls.model.internal.Activity.Result;
import com.nsn.ood.cls.model.internal.ActivityDetail;
import com.nsn.ood.cls.model.internal.ActivityDetail.Status;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
@Component(provides = ActivityCreator.class)
public class ActivityCreator {
	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	
	@ServiceDependency(filter = "(&(from=activityOperation)(to=string))")
	private Converter<OperationType, String> activityOperationType2StringConverter;
	
	@ServiceDependency(filter = "(&(from=activityResult)(to=string))")
	private Converter<Result, String> activityResult2StringConverter;
	
	@ServiceDependency(filter = "(&(from=activityDetailStatus)(to=string))")
	private Converter<Status, String> activityDetailStatus2StringConverter;

	public Activity createActivity(final ResultSet resultSet) throws SQLException {
		return new Activity()//
				.withId(resultSet.getLong("id"))//
				.withClientId(resultSet.getString("clientid"))//
				.withActivityTime(timestamp2DateTimeConverter.convertTo(resultSet.getTimestamp("activitytime")))//
				.withOperationType(activityOperationType2StringConverter.convertFrom(resultSet.getString("operationtype")))//
				.withResult(activityResult2StringConverter.convertFrom(resultSet.getString("result")));
	}

	public ActivityDetail createActivityDetail(final ResultSet resultSet) throws SQLException {
		Long featureCode = resultSet.getLong("featurecode");
		if (resultSet.wasNull()) {
			featureCode = null;
		}
		return new ActivityDetail()//
				.withFeatureCode(featureCode)//
				.withFeatureName(resultSet.getString("featurename"))//
				.withFileName(resultSet.getString("filename"))//
				.withErrorCode(resultSet.getString("errorcode"))//
				.withStatus(activityDetailStatus2StringConverter.convertFrom(resultSet.getString("status")))//
				.withSettingKey(resultSet.getString("settingkey"))//
				.withSettingValue(resultSet.getString("settingvalue"));
	}
}
