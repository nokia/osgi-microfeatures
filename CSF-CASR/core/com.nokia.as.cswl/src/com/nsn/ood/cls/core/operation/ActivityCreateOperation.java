/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.Query;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.Update;
import com.nsn.ood.cls.core.db.activity.InsertActivities;
import com.nsn.ood.cls.core.db.activity.QueryNextActivityId;
import com.nsn.ood.cls.core.operation.exception.CreateException;
import com.nsn.ood.cls.core.security.BasicPrincipal;
import com.nsn.ood.cls.model.internal.Activity;
import com.nsn.ood.cls.model.internal.ActivityDetail;
import com.nsn.ood.cls.model.internal.Activity.OperationType;
import com.nsn.ood.cls.model.internal.Activity.Result;
import com.nsn.ood.cls.model.internal.ActivityDetail.Status;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component(provides = ActivityCreateOperation.class)
@Loggable
public class ActivityCreateOperation {
	private static final Logger LOG = LoggerFactory.getLogger(ActivityCreateOperation.class);

	@ServiceDependency(filter = "(name=query)")
	private StatementExecutor<Query> queryExecutor;
	@ServiceDependency(filter = "(name=update)")
	private StatementExecutor<Update> updateExecutor;
	@ServiceDependency
	private BasicPrincipal basicPrincipal;

	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	@ServiceDependency(filter = "(&(from=activityOperation)(to=string))")
	private Converter<OperationType, String> activityOperationType2StringConverter;
	@ServiceDependency(filter = "(&(from=activityResult)(to=string))")
	private Converter<Result, String> activityResult2StringConverter;
	@ServiceDependency(filter = "(&(from=activityDetailStatus)(to=string))")
	private Converter<Status, String> activityDetailStatus2StringConverter;

	public void createActivity(final Activity activity, final List<ActivityDetail> activityDetails) throws CreateException {
		activity.setActivityTime(DateTime.now());
		activity.setClientId(this.basicPrincipal.getUser());
		// activity.setId(retrieveNextId());//still waiting for sequences in MariaDB

		try {
			this.updateExecutor.execute(createInsertActivities(activity, activityDetails));
		} catch (final SQLException e) {
			LOG.error(LogMessage.UPDATE_FAIL, e);
			throw new CreateException(e);
		}
	}

	private long retrieveNextId() throws CreateException {
		try {
			final QueryNextActivityId query = createQueryNextActivityId();
			this.queryExecutor.execute(query);
			return query.getValue();
		} catch (final SQLException e) {
			LOG.error(LogMessage.QUERY_FAIL, e);
			throw new CreateException(e);
		}
	}

	protected QueryNextActivityId createQueryNextActivityId() {
		return new QueryNextActivityId();
	}

	protected InsertActivities createInsertActivities(final Activity activity, final List<ActivityDetail> activityDetails) {
		return new InsertActivities(activity, activityDetails, timestamp2DateTimeConverter, activityOperationType2StringConverter, activityResult2StringConverter, activityDetailStatus2StringConverter);
	}
}
