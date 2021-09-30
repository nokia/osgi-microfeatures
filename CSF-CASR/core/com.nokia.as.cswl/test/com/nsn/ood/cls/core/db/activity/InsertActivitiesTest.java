/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.activity;

import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.activityDetail;
import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.activityDetailsList;
import static com.nsn.ood.cls.model.internal.test.ActivityTestUtil.activity;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.Test;

import com.nsn.ood.cls.core.convert.ActivityDetailStatus2StringConverter;
import com.nsn.ood.cls.core.convert.ActivityOperationType2StringConverter;
import com.nsn.ood.cls.core.convert.ActivityResult2StringConverter;
import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.model.internal.Activity.OperationType;
import com.nsn.ood.cls.model.internal.Activity.Result;
import com.nsn.ood.cls.model.internal.ActivityDetail.Status;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class InsertActivitiesTest {
	private static final DateTime TIME = new DateTime(2015, 6, 1, 13, 47);

	@Test
	public void testSql() throws Exception {
		assertEquals(
				"insert into cls.activities (clientid, activitytime, operationtype, result) values (?, ?, ?, ?)",
				new InsertActivities(null, null, null, null, null, null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final Converter<Timestamp, DateTime> timestamp2DateTimeConverter = createMock(Timestamp2DateTimeConverter.class);
		final Converter<OperationType, String> activityOperationType2StringConverter = createMock(ActivityOperationType2StringConverter.class);
		final Converter<Result, String> activityResult2StringConverter = createMock(ActivityResult2StringConverter.class);
		final Converter<Status, String> activityDetailStatus2StringConverter = createMock(ActivityDetailStatus2StringConverter.class);
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		//statementMock.setLong(1, 13L);
		statementMock.setString(1, "id");
		expect(timestamp2DateTimeConverter.convertFrom(TIME)).andReturn(new Timestamp(2L));
		statementMock.setTimestamp(2, new Timestamp(2L));
		expect(activityOperationType2StringConverter.convertTo(OperationType.SETTING_UPDATE)).andReturn("type");
		statementMock.setString(3, "type");
		expect(activityResult2StringConverter.convertTo(Result.PARTIAL)).andReturn("result");
		statementMock.setString(4, "result");

		replayAll();
		new InsertActivities(activity("id", OperationType.SETTING_UPDATE, Result.PARTIAL, TIME), null, timestamp2DateTimeConverter, 
				activityOperationType2StringConverter, activityResult2StringConverter, activityDetailStatus2StringConverter).prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testNext() throws Exception {
		assertNull(new InsertActivities(null, null, null, null, null, null).next());
		assertNull(new InsertActivities(null, activityDetailsList(), null, null, null, null).next());
		assertTrue(new InsertActivities(null, activityDetailsList(activityDetail()), null, null, null, null).next() instanceof SetLastInsertIDVariable);
	}
}
