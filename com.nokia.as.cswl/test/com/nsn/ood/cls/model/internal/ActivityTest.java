/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal;

import static com.nsn.ood.cls.model.internal.test.ActivityTestUtil.assertActivity;
import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonEnum;
import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonProperty;
import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonPropertyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.joda.time.DateTime;
import org.junit.Test;

import com.nsn.ood.cls.model.internal.Activity.OperationType;
import com.nsn.ood.cls.model.internal.Activity.Result;


/**
 * @author marynows
 * 
 */
public class ActivityTest {
	private static final DateTime TIME = new DateTime(2015, 6, 1, 11, 59);

	@Test
	public void testEmptyActivity() throws Exception {
		assertActivity(new Activity(), null, null, null, null, null);
	}

	@Test
	public void testActivity() throws Exception {
		assertActivity(new Activity().withId(12L), 12L, null, null, null, null);
		assertActivity(new Activity().withClientId("id"), null, "id", null, null, null);
		assertActivity(new Activity().withOperationType(OperationType.LICENSE_CANCEL), null, null,
				OperationType.LICENSE_CANCEL, null, null);
		assertActivity(new Activity().withResult(Result.SUCCESS), null, null, null, Result.SUCCESS, null);
		assertActivity(new Activity().withActivityTime(TIME), null, null, null, null, TIME);

		assertActivity(new Activity().withId(12L).withClientId("id").withOperationType(OperationType.LICENSE_INSTALL)
				.withResult(Result.FAILURE).withActivityTime(TIME), 12L, "id", OperationType.LICENSE_INSTALL,
				Result.FAILURE, TIME);
	}

	@Test
	public void testActivitySetters() throws Exception {
		final Activity activity = new Activity();
		activity.setId(12L);
		activity.setClientId("id");
		activity.setOperationType(OperationType.LICENSE_INSTALL);
		activity.setResult(Result.FAILURE);
		activity.setActivityTime(TIME);

		assertActivity(activity, 12L, "id", OperationType.LICENSE_INSTALL, Result.FAILURE, TIME);
	}

	@Test
	public void testAnnotations() throws Exception {
		assertJsonPropertyOrder(Activity.class, "id", "activityTime", "clientId", "operationType", "result");
		assertJsonProperty(Activity.class, "id", "id");
		assertJsonProperty(Activity.class, "activityTime", "activityTime");
		assertJsonProperty(Activity.class, "clientId", "clientId");
		assertJsonProperty(Activity.class, "operationType", "operationType");
		assertJsonProperty(Activity.class, "result", "result");
		assertJsonEnum(OperationType.class);
		assertJsonEnum(Result.class);
	}

	@Test
	public void testOperationType() throws Exception {
		assertEquals("license_install", OperationType.LICENSE_INSTALL.toString());
		assertEquals("license_cancel", OperationType.LICENSE_CANCEL.toString());
		assertEquals("setting_update", OperationType.SETTING_UPDATE.toString());

		assertEquals(OperationType.LICENSE_INSTALL, OperationType.fromValue("license_install"));
		assertEquals(OperationType.LICENSE_CANCEL, OperationType.fromValue("license_cancel"));
		assertEquals(OperationType.SETTING_UPDATE, OperationType.fromValue("setting_update"));
		try {
			OperationType.fromValue(null);
		} catch (final IllegalArgumentException e) {
		}
	}

	@Test
	public void testResult() throws Exception {
		assertEquals("failure", Result.FAILURE.toString());
		assertEquals("partial", Result.PARTIAL.toString());
		assertEquals("success", Result.SUCCESS.toString());

		assertEquals(Result.FAILURE, Result.fromValue("failure"));
		assertEquals(Result.PARTIAL, Result.fromValue("partial"));
		assertEquals(Result.SUCCESS, Result.fromValue("success"));
		try {
			Result.fromValue(null);
		} catch (final IllegalArgumentException e) {
		}
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(new Activity().toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		final Activity activity = new Activity().withResult(Result.PARTIAL);

		assertFalse(activity.equals(null));
		assertFalse(activity.equals("test"));
		assertEquals(activity, activity);

		assertFalse(activity.equals(new Activity()));
		assertNotEquals(activity.hashCode(), new Activity().hashCode());

		final Activity activity2 = new Activity().withResult(Result.PARTIAL);
		assertEquals(activity, activity2);
		assertEquals(activity.hashCode(), activity2.hashCode());
	}
}
