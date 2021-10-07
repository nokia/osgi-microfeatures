/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.activity;

import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.activityDetail;
import static com.nsn.ood.cls.model.internal.test.ActivityTestUtil.activity;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.Types;

import org.junit.Test;

import com.nsn.ood.cls.core.convert.ActivityDetailStatus2StringConverter;
import com.nsn.ood.cls.model.internal.ActivityDetail.Status;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class InsertActivityDetailsTest {

	@Test
	public void testSql() throws Exception {
		assertEquals("insert into cls.activitydetails"
				+ " (activityid, filename, errorcode, status, featurecode, featurename, settingkey, settingvalue)"
				+ " values (@actId, ?, ?, ?, ?, ?, ?, ?)", new InsertActivityDetails(null, null, null).sql());
	}

	@Test
	public void testPrepareRowForLicense() throws Exception {
		final Converter<Status, String> converterMock = createMock(ActivityDetailStatus2StringConverter.class);
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		//statementMock.setLong(1, 33L);
		statementMock.setString(1, "file");
		statementMock.setString(2, "code");
		expect(converterMock.convertTo(Status.FAILURE)).andReturn("status");
		statementMock.setString(3, "status");
		statementMock.setLong(4, 12L);
		statementMock.setString(5, "name");
		statementMock.setString(6, null);
		statementMock.setString(7, null);

		replayAll();
		new InsertActivityDetails(activity(), null, converterMock)//
				.prepareRow(statementMock, activityDetail("code", Status.FAILURE, "file", "name", 12L));
		verifyAll();
	}

	@Test
	public void testPrepareRowWithNullFeatureCode() throws Exception {
		final Converter<Status, String> converterMock = createMock(ActivityDetailStatus2StringConverter.class);
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		//statementMock.setLong(1, 44L);
		statementMock.setString(1, null);
		statementMock.setString(2, "code");
		expect(converterMock.convertTo(Status.SUCCESS)).andReturn("status");
		statementMock.setString(3, "status");
		statementMock.setNull(4, Types.NUMERIC);
		statementMock.setString(5, null);
		statementMock.setString(6, "key");
		statementMock.setString(7, "value");

		replayAll();
		new InsertActivityDetails(activity(), null, converterMock)//
				.prepareRow(statementMock, activityDetail("code", Status.SUCCESS, "key", "value"));
		verifyAll();
	}
}
