/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.activityDetail;
import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.activityDetailsList;
import static com.nsn.ood.cls.model.test.JodaTestUtil.assertNow;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.SQLException;
import java.util.List;

import org.easymock.Capture;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.Ignore;

import com.nsn.ood.cls.core.db.QueryExecutor;
import com.nsn.ood.cls.core.db.UpdateExecutor;
import com.nsn.ood.cls.core.db.activity.InsertActivities;
import com.nsn.ood.cls.core.db.activity.QueryNextActivityId;
import com.nsn.ood.cls.core.operation.exception.CreateException;
import com.nsn.ood.cls.core.security.BasicPrincipal;
import com.nsn.ood.cls.model.internal.Activity;
import com.nsn.ood.cls.model.internal.ActivityDetail;


/**
 * @author marynows
 * 
 */
public class ActivityCreateOperationTest extends ActivityCreateOperation {
	private static final long ID = 77L;
	private static final QueryNextActivityId QUERY_NEXT_ACTIVITY_ID_STUB = new QueryNextActivityId() {
		@Override
		public Long getValue() {
			return ID;
		}
	};

	private Activity capturedActivity;
	private List<ActivityDetail> capturedActivityDetails;

	@Override
	protected QueryNextActivityId createQueryNextActivityId() {
		super.createQueryNextActivityId();
		return QUERY_NEXT_ACTIVITY_ID_STUB;
	}

	@Override
	protected InsertActivities createInsertActivities(final Activity activity,
			final List<ActivityDetail> activityDetails) {
		this.capturedActivity = activity;
		this.capturedActivityDetails = activityDetails;
		return super.createInsertActivities(null, null);
	}

	@Test
	public void testCreateActivity() throws Exception {
		final BasicPrincipal basicPrincipalMock = createMock(BasicPrincipal.class);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);
		final Activity activityMock = createMock(Activity.class);
		final Capture<DateTime> capturedTime = new Capture<>();

		activityMock.setActivityTime(capture(capturedTime));
		expect(basicPrincipalMock.getUser()).andReturn("user");
		activityMock.setClientId("user");
		//queryExecutorMock.execute(same(QUERY_NEXT_ACTIVITY_ID_STUB));
		//activityMock.setId(ID);
		updateExecutorMock.execute(isA(InsertActivities.class));

		replayAll();
		setInternalState(this, basicPrincipalMock);
		setInternalState(this, "updateExecutor", updateExecutorMock);
		setInternalState(this, "queryExecutor", queryExecutorMock);
		createActivity(activityMock, activityDetailsList(activityDetail("code")));
		verifyAll();

		assertNow(capturedTime.getValue());
		assertEquals(activityMock, this.capturedActivity);
		assertEquals(activityDetailsList(activityDetail("code")), this.capturedActivityDetails);
	}

	@Test
	public void testCreateActivityWithSQLExceptionDuringUpdate() throws Exception {
		final BasicPrincipal basicPrincipalMock = createMock(BasicPrincipal.class);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);
		final Activity activityMock = createMock(Activity.class);
		final Capture<DateTime> capturedTime = new Capture<>();

		activityMock.setActivityTime(capture(capturedTime));
		expect(basicPrincipalMock.getUser()).andReturn("user");
		activityMock.setClientId("user");
		//queryExecutorMock.execute(same(QUERY_NEXT_ACTIVITY_ID_STUB));
		//activityMock.setId(ID);
		updateExecutorMock.execute(isA(InsertActivities.class));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this, basicPrincipalMock);
		setInternalState(this, "updateExecutor", updateExecutorMock);
		setInternalState(this, "queryExecutor", queryExecutorMock);
		try {
			createActivity(activityMock, activityDetailsList(activityDetail("code")));
			fail();
		} catch (final CreateException e) {
			assertEquals("message", e.getMessage());
		}
		verifyAll();

		assertNow(capturedTime.getValue());
		assertEquals(activityMock, this.capturedActivity);
		assertEquals(activityDetailsList(activityDetail("code")), this.capturedActivityDetails);
	}

	@Test
	@Ignore
	public void testCreateActivityWithSQLExceptionDuringQuery() throws Exception {
		final BasicPrincipal basicPrincipalMock = createMock(BasicPrincipal.class);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);
		final Activity activityMock = createMock(Activity.class);
		final Capture<DateTime> capturedTime = new Capture<>();

		activityMock.setActivityTime(capture(capturedTime));
		expect(basicPrincipalMock.getUser()).andReturn("user");
		activityMock.setClientId("user");
		queryExecutorMock.execute(same(QUERY_NEXT_ACTIVITY_ID_STUB));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this, basicPrincipalMock, queryExecutorMock, updateExecutorMock);
		try {
			createActivity(activityMock, activityDetailsList(activityDetail("code")));
			fail();
		} catch (final CreateException e) {
			assertEquals("message", e.getMessage());
		}
		verifyAll();

		assertNow(capturedTime.getValue());
		assertNull(this.capturedActivity);
		assertNull(this.capturedActivityDetails);
	}
}
