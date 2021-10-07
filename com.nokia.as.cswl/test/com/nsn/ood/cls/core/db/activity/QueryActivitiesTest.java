/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.activity;

import static com.nsn.ood.cls.model.internal.test.ActivityTestUtil.activity;
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
public class QueryActivitiesTest {
	private static final Conditions CONDITIONS = ConditionsBuilder.create().build();
	private static final ConditionsMapper MAPPER = new ConditionsMapper();

	@Test
	public void testInitialization() throws Exception {
		final QueryActivities query = new QueryActivities(CONDITIONS, MAPPER, null);
		assertEquals("select * from cls.activities", getInternalState(query, String.class, ConditionsQuery.class));
	}

	@Test
	public void testHandleRow() throws Exception {
		final ActivityCreator activityCreatorMock = createMock(ActivityCreator.class);
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(activityCreatorMock.createActivity(resultSetMock)).andReturn(activity(7L));

		replayAll();
		final QueryActivities query = new QueryActivities(CONDITIONS, MAPPER, activityCreatorMock);
		assertEquals(activity(7L), query.handleRow(resultSetMock));
		verifyAll();
	}
}
