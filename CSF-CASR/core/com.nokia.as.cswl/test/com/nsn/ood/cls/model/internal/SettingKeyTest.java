/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal;

import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonEnum;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class SettingKeyTest {

	@Test
	public void testToString() throws Exception {
		assertEquals("floatingReleaseTime", SettingKey.FLOATING_RELEASE_TIME.toString());
		assertEquals("expiringLicensesThreshold", SettingKey.EXPIRING_LICENSES_THRESHOLD.toString());
		assertEquals("emailNotifications", SettingKey.EMAIL_NOTIFICATIONS.toString());
		assertEquals("emailRecipients", SettingKey.EMAIL_RECIPIENTS.toString());
		assertEquals("emailSender", SettingKey.EMAIL_SENDER.toString());
		assertEquals("emailServer", SettingKey.EMAIL_SERVER.toString());
		assertEquals("emailSubject", SettingKey.EMAIL_SUBJECT.toString());
		assertEquals("capacityThreshold", SettingKey.CAPACITY_THRESHOLD.toString());
	}

	@Test
	public void testFromValue() throws Exception {
		assertEquals(SettingKey.FLOATING_RELEASE_TIME, SettingKey.fromValue("floatingReleaseTime"));
		assertEquals(SettingKey.EXPIRING_LICENSES_THRESHOLD, SettingKey.fromValue("expiringLicensesThreshold"));
		assertEquals(SettingKey.EMAIL_NOTIFICATIONS, SettingKey.fromValue("emailNotifications"));
		assertEquals(SettingKey.EMAIL_RECIPIENTS, SettingKey.fromValue("emailRecipients"));
		assertEquals(SettingKey.EMAIL_SENDER, SettingKey.fromValue("emailSender"));
		assertEquals(SettingKey.EMAIL_SERVER, SettingKey.fromValue("emailServer"));
		assertEquals(SettingKey.EMAIL_SUBJECT, SettingKey.fromValue("emailSubject"));
		assertEquals(SettingKey.CAPACITY_THRESHOLD, SettingKey.fromValue("capacityThreshold"));
		try {
			SettingKey.fromValue("test");
			fail();
		} catch (final IllegalArgumentException e) {
		}
		try {
			SettingKey.fromValue(null);
			fail();
		} catch (final IllegalArgumentException e) {
		}
	}

	@Test
	public void testDefaultValue() throws Exception {
		assertEquals(86400L, SettingKey.FLOATING_RELEASE_TIME.defaultValue());
		assertTrue(SettingKey.FLOATING_RELEASE_TIME.defaultValue() instanceof Long);

		assertEquals(100L, SettingKey.EXPIRING_LICENSES_THRESHOLD.defaultValue());
		assertTrue(SettingKey.EXPIRING_LICENSES_THRESHOLD.defaultValue() instanceof Long);

		assertEquals(false, SettingKey.EMAIL_NOTIFICATIONS.defaultValue());
		assertTrue(SettingKey.EMAIL_NOTIFICATIONS.defaultValue() instanceof Boolean);

		assertEquals("", SettingKey.EMAIL_RECIPIENTS.defaultValue());
		assertTrue(SettingKey.EMAIL_RECIPIENTS.defaultValue() instanceof String);

		assertEquals("", SettingKey.EMAIL_SENDER.defaultValue());
		assertTrue(SettingKey.EMAIL_SENDER.defaultValue() instanceof String);

		assertEquals("", SettingKey.EMAIL_SERVER.defaultValue());
		assertTrue(SettingKey.EMAIL_SERVER.defaultValue() instanceof String);

		assertEquals("[CLS]", SettingKey.EMAIL_SUBJECT.defaultValue());
		assertTrue(SettingKey.EMAIL_SUBJECT.defaultValue() instanceof String);

		assertEquals(80L, SettingKey.CAPACITY_THRESHOLD.defaultValue());
		assertTrue(SettingKey.CAPACITY_THRESHOLD.defaultValue() instanceof Long);

	}

	@Test
	public void testValidate() throws Exception {
		expectValidationFail(SettingKey.FLOATING_RELEASE_TIME, null);
		expectValidationFail(SettingKey.FLOATING_RELEASE_TIME, "");
		expectValidationFail(SettingKey.FLOATING_RELEASE_TIME, "1");
		expectValidationOK(SettingKey.FLOATING_RELEASE_TIME, 1);
		expectValidationFail(SettingKey.FLOATING_RELEASE_TIME, 0);
		expectValidationFail(SettingKey.FLOATING_RELEASE_TIME, -1);
		expectValidationOK(SettingKey.FLOATING_RELEASE_TIME, 1L);
		expectValidationFail(SettingKey.FLOATING_RELEASE_TIME, 0L);
		expectValidationFail(SettingKey.FLOATING_RELEASE_TIME, -1L);
		expectValidationFail(SettingKey.FLOATING_RELEASE_TIME, 1.1);
		expectValidationOK(SettingKey.FLOATING_RELEASE_TIME, 359999999999L); // max value: 100000000*3600-1
		expectValidationFail(SettingKey.FLOATING_RELEASE_TIME, 360000000000L);

		expectValidationFail(SettingKey.EXPIRING_LICENSES_THRESHOLD, null);
		expectValidationFail(SettingKey.EXPIRING_LICENSES_THRESHOLD, "");
		expectValidationFail(SettingKey.EXPIRING_LICENSES_THRESHOLD, "1");
		expectValidationOK(SettingKey.EXPIRING_LICENSES_THRESHOLD, 1);
		expectValidationFail(SettingKey.EXPIRING_LICENSES_THRESHOLD, 0);
		expectValidationFail(SettingKey.EXPIRING_LICENSES_THRESHOLD, -1);
		expectValidationOK(SettingKey.EXPIRING_LICENSES_THRESHOLD, 1L);
		expectValidationFail(SettingKey.EXPIRING_LICENSES_THRESHOLD, 0L);
		expectValidationFail(SettingKey.EXPIRING_LICENSES_THRESHOLD, -1L);
		expectValidationFail(SettingKey.EXPIRING_LICENSES_THRESHOLD, 1.1);
		expectValidationOK(SettingKey.EXPIRING_LICENSES_THRESHOLD, 99999999L);
		expectValidationFail(SettingKey.EXPIRING_LICENSES_THRESHOLD, 100000000L);

		expectValidationOK(SettingKey.EMAIL_NOTIFICATIONS, true);
		expectValidationOK(SettingKey.EMAIL_NOTIFICATIONS, false);
		expectValidationFail(SettingKey.EMAIL_NOTIFICATIONS, null);
		expectValidationFail(SettingKey.EMAIL_NOTIFICATIONS, "");
		expectValidationFail(SettingKey.EMAIL_NOTIFICATIONS, "true");

		expectValidationFail(SettingKey.EMAIL_RECIPIENTS, null);
		expectValidationFail(SettingKey.EMAIL_RECIPIENTS, 33);
		expectValidationOK(SettingKey.EMAIL_RECIPIENTS, "");
		expectValidationFail(SettingKey.EMAIL_RECIPIENTS, "test");
		expectValidationOK(SettingKey.EMAIL_RECIPIENTS, "test@test.com");
		expectValidationOK(SettingKey.EMAIL_RECIPIENTS, "Name <test@test.com>");
		expectValidationFail(SettingKey.EMAIL_RECIPIENTS, "Name <test@test.com>, aaa");
		expectValidationOK(SettingKey.EMAIL_RECIPIENTS, "Name <test@test.com>, aaa@bb.cc");

		expectValidationFail(SettingKey.EMAIL_SENDER, null);
		expectValidationFail(SettingKey.EMAIL_SENDER, 33);
		expectValidationFail(SettingKey.EMAIL_SENDER, "");
		expectValidationFail(SettingKey.EMAIL_SENDER, "test");
		expectValidationOK(SettingKey.EMAIL_SENDER, "test@domain.com");

		expectValidationFail(SettingKey.EMAIL_SERVER, null);
		expectValidationFail(SettingKey.EMAIL_SERVER, 33);
		expectValidationOK(SettingKey.EMAIL_SERVER, "");
		expectValidationOK(SettingKey.EMAIL_SERVER, "host");
		expectValidationFail(SettingKey.EMAIL_SERVER, ":host");
		expectValidationOK(SettingKey.EMAIL_SERVER, "1.2.3.4");

		expectValidationFail(SettingKey.EMAIL_SUBJECT, null);
		expectValidationFail(SettingKey.EMAIL_SUBJECT, 33);
		expectValidationOK(SettingKey.EMAIL_SUBJECT, "");
		expectValidationOK(SettingKey.EMAIL_SUBJECT, "test");

		expectValidationFail(SettingKey.CAPACITY_THRESHOLD, null);
		expectValidationFail(SettingKey.CAPACITY_THRESHOLD, "");
		expectValidationFail(SettingKey.CAPACITY_THRESHOLD, "1");
		expectValidationFail(SettingKey.CAPACITY_THRESHOLD, 1.000001);
		expectValidationFail(SettingKey.CAPACITY_THRESHOLD, 0);
		expectValidationOK(SettingKey.CAPACITY_THRESHOLD, 1);
		expectValidationOK(SettingKey.CAPACITY_THRESHOLD, 10L);
		expectValidationOK(SettingKey.CAPACITY_THRESHOLD, 90);
		expectValidationOK(SettingKey.CAPACITY_THRESHOLD, 100L);
		expectValidationFail(SettingKey.CAPACITY_THRESHOLD, 101);
	}

	private void expectValidationOK(final SettingKey key, final Object value) {
		key.validate(value);
	}

	private void expectValidationFail(final SettingKey key, final Object value) {
		try {
			key.validate(value);
			fail();
		} catch (final IllegalArgumentException e) {
		}
	}

	@Test
	public void testAnnotations() throws Exception {
		assertJsonEnum(SettingKey.class);
	}
}
