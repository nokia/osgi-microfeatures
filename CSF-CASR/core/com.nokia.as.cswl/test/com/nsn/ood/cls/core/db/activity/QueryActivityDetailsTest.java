/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.activity;

import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.activityDetail;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.getInternalState;

import java.sql.ResultSet;

import org.junit.Test;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.db.ConditionsQuery;
import com.nsn.ood.cls.core.db.creator.ActivityCreator;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;


/**
 * @author marynows
 * 
 */
public class QueryActivityDetailsTest {
	private static final Conditions CONDITIONS = ConditionsBuilder.create().build();
	private static final ConditionsMapper MAPPER = new ConditionsMapper();

	@Test
	public void testInitialization() throws Exception {
		final QueryActivityDetails query = new QueryActivityDetails(CONDITIONS, MAPPER, null);
		assertEquals("select * from cls.activitydetails", getInternalState(query, String.class, ConditionsQuery.class));
	}

	@Test
	public void testHandleRow() throws Exception {
		final ActivityCreator activityCreatorMock = createMock(ActivityCreator.class);
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(activityCreatorMock.createActivityDetail(resultSetMock)).andReturn(activityDetail("eee"));

		replayAll();
		final QueryActivityDetails query = new QueryActivityDetails(CONDITIONS, MAPPER, activityCreatorMock);
		assertEquals(activityDetail("eee"), query.handleRow(resultSetMock));
		verifyAll();
	}
}
