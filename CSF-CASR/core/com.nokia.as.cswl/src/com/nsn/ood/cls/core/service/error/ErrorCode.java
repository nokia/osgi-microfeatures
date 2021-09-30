/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.error;

/**
 * @author marynows
 *
 */
public enum ErrorCode {
	/** 100 */
	NOT_ENOUGH_CAPACITY(100L),
	/** 101 */
	CANNOT_RELEASE_CAPACITY(101L),
	/** 102 */
	ON_OFF_LICENSE_MISSING(102L),
	/** 103 */
	DUPLICATED_CLIENT_ID(103L),
	/** 104 */
	CANNOT_RESERVE_CLIENT_ID(104L),
	/** 105 */
	CANNOT_UPDATE_KEEP_ALIVE(105L),

	/** 120 */
	CONDITIONS_FAIL(120L),
	/** 121 */
	CONCURRENT_ACTIONS_FAIL(121L),
	/** 122 */
	CONFIGURATION_UPDATE_FAIL(122L),
	/** 123 */
	ACTIVITY_CREATION_FAIL(123L),
	/** 124 */
	CAPACITY_UPDATE_FAIL(124L),

	/** 140 */
	VALIDATION_ERROR(140L),

	/** 150 */
	CLJL_LICENSE_INSTALL_FAIL(150L),
	/** 151 */
	LICENSE_VERIFICATION_FAIL(151L),
	/** 152 */
	CLJL_LICENSE_CANCEL_FAIL(152L),
	/** 153 */
	LICENSE_EXPORT_FAIL(153L),
	/** 154 */
	LICENSE_INSTALL_FAIL(154L),
	/** 155 */
	LICENSE_CANCEL_FAIL(155L),

	/** 4 */
	RESOURCE_NOT_FOUND(4L),

	/** 500 */
	INTERNAL_ERROR(500L);

	private final long code;

	private ErrorCode(final long code) {
		this.code = code;
	}

	public long getCode() {
		return this.code;
	}

	public static ErrorCode fromCode(final long code) {
		for (final ErrorCode clsErrorCode : values()) {
			if (clsErrorCode.getCode() == code) {
				return clsErrorCode;
			}
		}
		return null;
	}
}
