/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.activity;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.joda.time.DateTime;

import com.nsn.ood.cls.core.db.SimpleUpdate;
import com.nsn.ood.cls.core.db.Update;
import com.nsn.ood.cls.model.internal.Activity;
import com.nsn.ood.cls.model.internal.ActivityDetail;
import com.nsn.ood.cls.model.internal.Activity.OperationType;
import com.nsn.ood.cls.model.internal.Activity.Result;
import com.nsn.ood.cls.model.internal.ActivityDetail.Status;
import com.nsn.ood.cls.util.CollectionUtils;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 *
 */
public class InsertActivities extends SimpleUpdate {
	private static final int CLIENT_ID = 1;
	private static final int ACTIVITY_TIME = 2;
	private static final int OPERATION_TYPE = 3;
	private static final int RESULT = 4;

	private final Activity activity;
	private final List<ActivityDetail> activityDetails;
	private final Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	private final Converter<OperationType, String> activityOperationType2StringConverter;
	private final Converter<Result, String> activityResult2StringConverter;
	private final Converter<Status, String> activityDetailStatus2StringConverter;

	public InsertActivities(final Activity activity, final List<ActivityDetail> activityDetails, 
							final Converter<Timestamp, DateTime> timestamp2DateTimeConverter,
							final Converter<OperationType, String> activityOperationType2StringConverter,
							final Converter<Result, String> activityResult2StringConverter,
							final Converter<Status, String> activityDetailStatus2StringConverter) {
		super("insert into cls.activities (clientid, activitytime, operationtype, result) values (?, ?, ?, ?)");
		this.activity = activity;
		this.activityDetails = activityDetails;
		this.timestamp2DateTimeConverter = timestamp2DateTimeConverter;
		this.activityOperationType2StringConverter = activityOperationType2StringConverter;
		this.activityResult2StringConverter = activityResult2StringConverter;
		this.activityDetailStatus2StringConverter = activityDetailStatus2StringConverter;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		// statement.setLong(ID, this.activity.getId());
		statement.setString(CLIENT_ID, this.activity.getClientId());
		statement.setTimestamp(ACTIVITY_TIME, timestamp2DateTimeConverter.convertFrom(this.activity.getActivityTime()));
		statement.setString(OPERATION_TYPE, activityOperationType2StringConverter.convertTo(this.activity.getOperationType()));
		statement.setString(RESULT, activityResult2StringConverter.convertTo(this.activity.getResult()));
	}

	@Override
	public Update next() {
		if (CollectionUtils.isEmpty(this.activityDetails)) {
			return null;
		} else {
			final InsertActivityDetails actDetailsQuery = new InsertActivityDetails(this.activity, this.activityDetails, this.activityDetailStatus2StringConverter);
			return new SetLastInsertIDVariable(actDetailsQuery);
		}
	}
}
