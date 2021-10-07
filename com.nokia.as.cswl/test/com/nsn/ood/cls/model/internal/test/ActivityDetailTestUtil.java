/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import com.nsn.ood.cls.model.internal.ActivityDetail;
import com.nsn.ood.cls.model.internal.ActivityDetail.Status;


/**
 * @author marynows
 * 
 */
public class ActivityDetailTestUtil {

	public static List<ActivityDetail> activityDetailsList(final ActivityDetail... activityDetails) {
		return Arrays.asList(activityDetails);
	}

	public static ActivityDetail activityDetail() {
		return new ActivityDetail();
	}

	public static ActivityDetail activityDetail(final String errorCode) {
		return activityDetail().withErrorCode(errorCode);
	}

	public static ActivityDetail activityDetail(final Status status) {
		return activityDetail().withStatus(status);
	}

	public static ActivityDetail activityDetail(final String errorCode, final Status status, final String fileName,
			final String featureName, final Long featureCode) {
		return activityDetail(errorCode).withStatus(status).withFileName(fileName).withFeatureName(featureName)
				.withFeatureCode(featureCode);
	}

	public static ActivityDetail activityDetail(final String errorCode, final Status status, final String settingKey,
			final String settingValue) {
		return activityDetail(errorCode).withStatus(status).withSettingKey(settingKey).withSettingValue(settingValue);
	}

	public static void assertActivityDetail(final ActivityDetail activityDetail, final String expectedErrorCode,
			final Status expectedStatus, final String expectedFileName, final String expectedFeatureName,
			final Long expectedFeatureCode, final String expectedSettingKey, final String expectedSettingValue) {
		assertEquals(expectedErrorCode, activityDetail.getErrorCode());
		assertEquals(expectedStatus, activityDetail.getStatus());
		assertEquals(expectedFileName, activityDetail.getFileName());
		assertEquals(expectedFeatureName, activityDetail.getFeatureName());
		assertEquals(expectedFeatureCode, activityDetail.getFeatureCode());
		assertEquals(expectedSettingKey, activityDetail.getSettingKey());
		assertEquals(expectedSettingValue, activityDetail.getSettingValue());
	}
}
