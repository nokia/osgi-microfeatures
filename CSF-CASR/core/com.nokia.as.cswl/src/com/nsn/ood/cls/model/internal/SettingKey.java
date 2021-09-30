/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * @author marynows
 *
 */
public enum SettingKey {
	/**
	 * Name: <code>floatingReleaseTime</code><br/>
	 * Default: <code>1</code> day in seconds (<code>86400</code>s)<br/>
	 * Value: <code>>0</code> and <code><=359999999999</code> (100000000*3600-1)
	 */
	FLOATING_RELEASE_TIME("floatingReleaseTime", 86400L) {
		@Override
		public void validate(final Object value) {
			expectPositiveNumberLessOrEqual(value, FLOATING_RELEASE_TIME_MAX);
		}
	},

	/**
	 * Name: <code>expiringLicensesThreshold</code><br/>
	 * Default: <code>100</code> days<br/>
	 * Value: <code>>0</code> and <code><=99999999</code>
	 */
	EXPIRING_LICENSES_THRESHOLD("expiringLicensesThreshold", 100L) {
		@Override
		public void validate(final Object value) {
			expectPositiveNumberLessOrEqual(value, EXPIRING_LICENSES_THRESHOLD_MAX);
		}
	},

	/**
	 * Name: <code>emailNotifications</code><br/>
	 * Default: <code>false</code><br/>
	 * Value: <code>true</code>, <code>false</code>
	 */
	EMAIL_NOTIFICATIONS("emailNotifications", false) {
		@Override
		public void validate(final Object value) {
			expectBoolean(value);
		}
	},

	/**
	 * Name: <code>emailServer</code><br/>
	 * Default: empty string<br/>
	 * Value: valid URI
	 */
	EMAIL_SERVER("emailServer", "") {
		@Override
		public void validate(final Object value) {
			URI.create(expectString(value));
		}
	},

	/**
	 * Name: <code>emailSender</code><br/>
	 * Default: empty string<br/>
	 * Value: valid e-mail address (RFC822 format)
	 */
	EMAIL_SENDER("emailSender", "") {
		@Override
		public void validate(final Object value) {
			try {
				new InternetAddress(expectString(value), true);
			} catch (final AddressException e) {
				throw illegalValueException(value, e);
			}
		}
	},

	/**
	 * Name: <code>emailRecipients</code><br/>
	 * Default: empty string<br/>
	 * Value: valid e-mail addresses (RFC822 format)
	 */
	EMAIL_RECIPIENTS("emailRecipients", "") {
		@Override
		public void validate(final Object value) {
			try {
				for (final InternetAddress address : InternetAddress.parse(expectString(value), true)) {
					address.validate();
				}
			} catch (final AddressException e) {
				throw illegalValueException(value, e);
			}
		}
	},

	/**
	 * Name: <code>emailSubject</code><br/>
	 * Default: [CLS]<br/>
	 * Value: any string
	 */
	EMAIL_SUBJECT("emailSubject", "[CLS]") {
		@Override
		public void validate(final Object value) {
			expectString(value);
		}
	},

	/**
	 * Name: <code>capacityThreshold</code><br/>
	 * Default: <code>80</code> percent<br/>
	 * Value: <code>>0</code> and <code><=100</code>
	 */
	CAPACITY_THRESHOLD("capacityThreshold", 80L) {
		@Override
		public void validate(final Object value) {
			expectPositiveNumberLessOrEqual(value, CAPACITY_THRESHOLD_MAX);
		}
	},;

	private static final long FLOATING_RELEASE_TIME_MAX = 359999999999L;
	private static final long EXPIRING_LICENSES_THRESHOLD_MAX = 99999999L;
	private static final long CAPACITY_THRESHOLD_MAX = 100L;

	private final String name;
	private final Object defaultValue;
	private static Map<String, SettingKey> constants = new HashMap<>();

	static {
		for (final SettingKey c : values()) {
			constants.put(c.name, c);
		}
	}

	private SettingKey(final String name, final Object defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
	}

	public abstract void validate(Object value);

	public Object defaultValue() {
		return this.defaultValue;
	}

	@JsonValue
	@Override
	public String toString() {
		return this.name;
	}

	@JsonCreator
	public static SettingKey fromValue(final String value) {
		final SettingKey constant = constants.get(value);
		if (constant == null) {
			throw new IllegalArgumentException(value);
		} else {
			return constant;
		}
	}

	private static Long expectPositiveNumber(final Object value) {
		if (((value instanceof Integer) || (value instanceof Long)) && (((Number) value).longValue() > 0)) {
			return ((Number) value).longValue();
		}
		throw illegalValueException(value, null);
	}

	private static Boolean expectBoolean(final Object value) {
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		throw illegalValueException(value, null);
	}

	private static String expectString(final Object value) {
		if (value instanceof String) {
			return (String) value;
		}
		throw illegalValueException(value, null);
	}

	private static void expectPositiveNumberLessOrEqual(final Object value, final Long limit) {
		final Long number = expectPositiveNumber(value);
		if (number <= limit) {
			return;
		}
		throw illegalValueException(value, null);
	}

	private static IllegalArgumentException illegalValueException(final Object value, final Throwable e) {
		String message = "Invalid value: " + Objects.toString(value, "<null>");
		if (e != null) {
			return new IllegalArgumentException(message, e);
		} else {
			return new IllegalArgumentException(message);
		}
	}
}