/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.activity;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import com.nsn.ood.cls.core.db.IterableUpdate;
import com.nsn.ood.cls.model.internal.Activity;
import com.nsn.ood.cls.model.internal.ActivityDetail;
import com.nsn.ood.cls.model.internal.ActivityDetail.Status;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 *
 */
class InsertActivityDetails extends IterableUpdate<ActivityDetail> {
	private static final int FILENAME = 1;
	private static final int ERROR_CODE = 2;
	private static final int STATUS = 3;
	private static final int FEATURE_CODE = 4;
	private static final int FEATURE_NAME = 5;
	private static final int SETTING_KEY = 6;
	private static final int SETTING_VALUE = 7;

	private final Converter<Status, String> activityDetailStatus2StringConverter;

	public InsertActivityDetails(final Activity activity, final List<ActivityDetail> activityDetails, final Converter<Status, String> activityDetailStatus2StringConverter) {
		super("insert into cls.activitydetails"
				+ " (activityid, filename, errorcode, status, featurecode, featurename, settingkey, settingvalue)"
				+ " values (@actId, ?, ?, ?, ?, ?, ?, ?)", activityDetails);
		this.activityDetailStatus2StringConverter = activityDetailStatus2StringConverter;
	}

	@Override
	protected void prepareRow(final PreparedStatement statement, final ActivityDetail activityDetail) throws SQLException {
		// statement.setLong(ACTIVITY_ID, this.activity.getId());
		statement.setString(FILENAME, activityDetail.getFileName());
		statement.setString(ERROR_CODE, activityDetail.getErrorCode());
		statement.setString(STATUS, activityDetailStatus2StringConverter.convertTo(activityDetail.getStatus()));
		if (activityDetail.getFeatureCode() == null) {
			statement.setNull(FEATURE_CODE, Types.NUMERIC);
		} else {
			statement.setLong(FEATURE_CODE, activityDetail.getFeatureCode());
		}
		statement.setString(FEATURE_NAME, activityDetail.getFeatureName());
		statement.setString(SETTING_KEY, activityDetail.getSettingKey());
		statement.setString(SETTING_VALUE, activityDetail.getSettingValue());
	}
}
