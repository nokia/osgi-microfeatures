/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal;

import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.assertActivityDetail;
import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonEnum;
import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonProperty;
import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonPropertyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import com.nsn.ood.cls.model.internal.ActivityDetail.Status;


/**
 * @author marynows
 * 
 */
public class ActivityDetailTest {

	@Test
	public void testEmptyDetail() throws Exception {
		assertActivityDetail(new ActivityDetail(), null, null, null, null, null, null, null);
	}

	@Test
	public void testDetail() throws Exception {
		assertActivityDetail(new ActivityDetail().withErrorCode("ec"), "ec", null, null, null, null, null, null);
		assertActivityDetail(new ActivityDetail().withStatus(Status.SUCCESS), null, Status.SUCCESS, null, null, null,
				null, null);
		assertActivityDetail(new ActivityDetail().withFileName("file"), null, null, "file", null, null, null, null);
		assertActivityDetail(new ActivityDetail().withFeatureName("name"), null, null, null, "name", null, null, null);
		assertActivityDetail(new ActivityDetail().withFeatureCode(12L), null, null, null, null, 12L, null, null);
		assertActivityDetail(new ActivityDetail().withSettingKey("key"), null, null, null, null, null, "key", null);
		assertActivityDetail(new ActivityDetail().withSettingValue("value"), null, null, null, null, null, null,
				"value");

		assertActivityDetail(new ActivityDetail().withErrorCode("ec").withStatus(Status.FAILURE).withFileName("file")
				.withFeatureName("name").withFeatureCode(12L).withSettingKey("key").withSettingValue("value"), "ec",
				Status.FAILURE, "file", "name", 12L, "key", "value");
	}

	@Test
	public void testDetailSetters() throws Exception {
		final ActivityDetail activityDetail = new ActivityDetail();
		activityDetail.setErrorCode("ec");
		activityDetail.setStatus(Status.FAILURE);
		activityDetail.setFileName("file");
		activityDetail.setFeatureName("name");
		activityDetail.setFeatureCode(12L);
		activityDetail.setSettingKey("key");
		activityDetail.setSettingValue("value");

		assertActivityDetail(activityDetail, "ec", Status.FAILURE, "file", "name", 12L, "key", "value");
	}

	@Test
	public void testAnnotations() throws Exception {
		assertJsonPropertyOrder(ActivityDetail.class, "errorCode", "status", "fileName", "featureCode", "featureName",
				"settingKey", "settingValue");
		assertJsonProperty(ActivityDetail.class, "errorCode", "errorCode");
		assertJsonProperty(ActivityDetail.class, "status", "status");
		assertJsonProperty(ActivityDetail.class, "fileName", "fileName");
		assertJsonProperty(ActivityDetail.class, "featureCode", "featureCode");
		assertJsonProperty(ActivityDetail.class, "featureName", "featureName");
		assertJsonProperty(ActivityDetail.class, "settingKey", "settingKey");
		assertJsonProperty(ActivityDetail.class, "settingValue", "settingValue");
		assertJsonEnum(Status.class);
	}

	@Test
	public void testStatus() throws Exception {
		assertEquals("failure", Status.FAILURE.toString());
		assertEquals("success", Status.SUCCESS.toString());

		assertEquals(Status.FAILURE, Status.fromValue("failure"));
		assertEquals(Status.SUCCESS, Status.fromValue("success"));
		try {
			Status.fromValue(null);
		} catch (final IllegalArgumentException e) {
		}
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(new ActivityDetail().toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		final ActivityDetail activityDetail = new ActivityDetail().withStatus(Status.FAILURE);

		assertFalse(activityDetail.equals(null));
		assertFalse(activityDetail.equals("test"));
		assertEquals(activityDetail, activityDetail);

		assertFalse(activityDetail.equals(new ActivityDetail()));
		assertNotEquals(activityDetail.hashCode(), new ActivityDetail().hashCode());

		final ActivityDetail activityDetail2 = new ActivityDetail().withStatus(Status.FAILURE);
		assertEquals(activityDetail, activityDetail2);
		assertEquals(activityDetail.hashCode(), activityDetail2.hashCode());
	}
}
