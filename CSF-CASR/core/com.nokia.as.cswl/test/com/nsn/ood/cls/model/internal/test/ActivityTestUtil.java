/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;

import com.nsn.ood.cls.model.internal.Activity;
import com.nsn.ood.cls.model.internal.Activity.OperationType;
import com.nsn.ood.cls.model.internal.Activity.Result;


/**
 * @author marynows
 * 
 */
public class ActivityTestUtil {

	public static List<Activity> activitiesList(final Activity... activities) {
		return Arrays.asList(activities);
	}

	public static Activity activity() {
		return new Activity();
	}

	public static Activity activity(final Long id) {
		return activity().withId(id);
	}

	public static Activity activity(final Long id, final String clientId, final OperationType operationType,
			final Result result, final DateTime time) {
		return activity(id).withClientId(clientId).withOperationType(operationType).withResult(result)
				.withActivityTime(time);
	}
	
	public static Activity activity(final String clientId, final OperationType operationType,
			final Result result, final DateTime time) {
		return activity().withClientId(clientId).withOperationType(operationType).withResult(result)
				.withActivityTime(time);
	}

	public static void assertActivity(final Activity activity, final Long expectedID, final String expectedClientId,
			final OperationType expectedOperationType, final Result expectedResult, final DateTime expectedActivityTime) {
		assertEquals(expectedID, activity.getId());
		assertEquals(expectedClientId, activity.getClientId());
		assertEquals(expectedOperationType, activity.getOperationType());
		assertEquals(expectedResult, activity.getResult());
		assertEquals(expectedActivityTime, activity.getActivityTime());
	}
}
