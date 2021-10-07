/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.creator;

import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.assertActivityDetail;
import static com.nsn.ood.cls.model.internal.test.ActivityTestUtil.assertActivity;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createStrictMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.ResultSet;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.convert.ActivityDetailStatus2StringConverter;
import com.nsn.ood.cls.core.convert.ActivityOperationType2StringConverter;
import com.nsn.ood.cls.core.convert.ActivityResult2StringConverter;
import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
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
public class ActivityCreatorTest {
	private static final DateTime TIME = new DateTime(2015, 6, 1, 13, 28);

	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	private Converter<OperationType, String> activityOperationType2StringConverter;
	private Converter<Result, String> activityResult2StringConverter;
	private Converter<Status, String> activityDetailStatus2StringConverter;
	private ActivityCreator creator;

	@Before
	public void setUp() throws Exception {
		this.timestamp2DateTimeConverter = createMock(Timestamp2DateTimeConverter.class);
		this.activityOperationType2StringConverter = createMock(ActivityOperationType2StringConverter.class);
		this.activityResult2StringConverter = createMock(ActivityResult2StringConverter.class);
		this.activityDetailStatus2StringConverter = createMock(ActivityDetailStatus2StringConverter.class);
		this.creator = new ActivityCreator();
		setInternalState(creator, "timestamp2DateTimeConverter", timestamp2DateTimeConverter);
		setInternalState(creator, "activityOperationType2StringConverter", activityOperationType2StringConverter);
		setInternalState(creator, "activityResult2StringConverter", activityResult2StringConverter);
		setInternalState(creator, "activityDetailStatus2StringConverter", activityDetailStatus2StringConverter);
	}

	@Test
	public void testCreateActivity() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.getLong("id")).andReturn(34L);
		expect(resultSetMock.getString("clientid")).andReturn("id");
		expect(resultSetMock.getTimestamp("activitytime")).andReturn(new Timestamp(1L));
		expect(this.timestamp2DateTimeConverter.convertTo(new Timestamp(1L))).andReturn(TIME);
		expect(resultSetMock.getString("operationtype")).andReturn("type");
		expect(this.activityOperationType2StringConverter.convertFrom("type")).andReturn(OperationType.LICENSE_INSTALL);
		expect(resultSetMock.getString("result")).andReturn("result");
		expect(this.activityResult2StringConverter.convertFrom("result")).andReturn(Result.SUCCESS);

		replayAll();
		final Activity activity = this.creator.createActivity(resultSetMock);
		verifyAll();

		assertNotNull(activity);
		assertActivity(activity, 34L, "id", OperationType.LICENSE_INSTALL, Result.SUCCESS, TIME);
	}

	@Test
	public void testCreateActivityDetail() throws Exception {
		final ResultSet resultSetMock = createStrictMock(ResultSet.class);

		expect(resultSetMock.getLong("featurecode")).andReturn(4321L);
		expect(resultSetMock.wasNull()).andReturn(false);
		expect(resultSetMock.getString("featurename")).andReturn("name");
		expect(resultSetMock.getString("filename")).andReturn("file");
		expect(resultSetMock.getString("errorcode")).andReturn("code");
		expect(resultSetMock.getString("status")).andReturn("status");
		expect(this.activityDetailStatus2StringConverter.convertFrom("status")).andReturn(Status.FAILURE);
		expect(resultSetMock.getString("settingkey")).andReturn("key");
		expect(resultSetMock.getString("settingvalue")).andReturn("value");

		replayAll();
		final ActivityDetail activityDetail = this.creator.createActivityDetail(resultSetMock);
		verifyAll();

		assertNotNull(activityDetail);
		assertActivityDetail(activityDetail, "code", Status.FAILURE, "file", "name", 4321L, "key", "value");
	}

	@Test
	public void testCreateErrorActivityDetail() throws Exception {
		final ResultSet resultSetMock = createStrictMock(ResultSet.class);

		expect(resultSetMock.getLong("featurecode")).andReturn(0L);
		expect(resultSetMock.wasNull()).andReturn(true);
		expect(resultSetMock.getString("featurename")).andReturn("name");
		expect(resultSetMock.getString("filename")).andReturn("file");
		expect(resultSetMock.getString("errorcode")).andReturn("code");
		expect(resultSetMock.getString("status")).andReturn("status");
		expect(this.activityDetailStatus2StringConverter.convertFrom("status")).andReturn(Status.FAILURE);
		expect(resultSetMock.getString("settingkey")).andReturn("key");
		expect(resultSetMock.getString("settingvalue")).andReturn("value");

		replayAll();
		final ActivityDetail activityDetail = this.creator.createActivityDetail(resultSetMock);
		verifyAll();

		assertNotNull(activityDetail);
		assertActivityDetail(activityDetail, "code", Status.FAILURE, "file", "name", null, "key", "value");
	}
}
