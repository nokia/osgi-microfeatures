/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.error;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class ErrorCodeTest {

	@Test
	public void testGetCode() throws Exception {
		assertEquals(100L, ErrorCode.NOT_ENOUGH_CAPACITY.getCode());
		assertEquals(101L, ErrorCode.CANNOT_RELEASE_CAPACITY.getCode());
		assertEquals(102L, ErrorCode.ON_OFF_LICENSE_MISSING.getCode());
		assertEquals(103L, ErrorCode.DUPLICATED_CLIENT_ID.getCode());
		assertEquals(104L, ErrorCode.CANNOT_RESERVE_CLIENT_ID.getCode());
		assertEquals(105L, ErrorCode.CANNOT_UPDATE_KEEP_ALIVE.getCode());
		assertEquals(120L, ErrorCode.CONDITIONS_FAIL.getCode());
		assertEquals(121L, ErrorCode.CONCURRENT_ACTIONS_FAIL.getCode());
		assertEquals(122L, ErrorCode.CONFIGURATION_UPDATE_FAIL.getCode());
		assertEquals(123L, ErrorCode.ACTIVITY_CREATION_FAIL.getCode());
		assertEquals(124L, ErrorCode.CAPACITY_UPDATE_FAIL.getCode());
		assertEquals(140L, ErrorCode.VALIDATION_ERROR.getCode());
		assertEquals(150L, ErrorCode.CLJL_LICENSE_INSTALL_FAIL.getCode());
		assertEquals(151L, ErrorCode.LICENSE_VERIFICATION_FAIL.getCode());
		assertEquals(152L, ErrorCode.CLJL_LICENSE_CANCEL_FAIL.getCode());
		assertEquals(153L, ErrorCode.LICENSE_EXPORT_FAIL.getCode());
		assertEquals(154L, ErrorCode.LICENSE_INSTALL_FAIL.getCode());
		assertEquals(155L, ErrorCode.LICENSE_CANCEL_FAIL.getCode());
		assertEquals(4L, ErrorCode.RESOURCE_NOT_FOUND.getCode());
	}

	@Test
	public void testFromCode() throws Exception {
		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.fromCode(4));
		assertEquals(ErrorCode.NOT_ENOUGH_CAPACITY, ErrorCode.fromCode(100));
		assertEquals(ErrorCode.CANNOT_RELEASE_CAPACITY, ErrorCode.fromCode(101));
		assertEquals(ErrorCode.ON_OFF_LICENSE_MISSING, ErrorCode.fromCode(102));
		assertEquals(ErrorCode.DUPLICATED_CLIENT_ID, ErrorCode.fromCode(103));
		assertEquals(ErrorCode.CANNOT_RESERVE_CLIENT_ID, ErrorCode.fromCode(104));
		assertEquals(ErrorCode.CANNOT_UPDATE_KEEP_ALIVE, ErrorCode.fromCode(105));
		assertEquals(ErrorCode.CONDITIONS_FAIL, ErrorCode.fromCode(120));
		assertEquals(ErrorCode.CONCURRENT_ACTIONS_FAIL, ErrorCode.fromCode(121));
		assertEquals(ErrorCode.CONFIGURATION_UPDATE_FAIL, ErrorCode.fromCode(122));
		assertEquals(ErrorCode.ACTIVITY_CREATION_FAIL, ErrorCode.fromCode(123));
		assertEquals(ErrorCode.CAPACITY_UPDATE_FAIL, ErrorCode.fromCode(124));
		assertEquals(ErrorCode.VALIDATION_ERROR, ErrorCode.fromCode(140));
		assertEquals(ErrorCode.CLJL_LICENSE_INSTALL_FAIL, ErrorCode.fromCode(150));
		assertEquals(ErrorCode.LICENSE_VERIFICATION_FAIL, ErrorCode.fromCode(151));
		assertEquals(ErrorCode.CLJL_LICENSE_CANCEL_FAIL, ErrorCode.fromCode(152));
		assertEquals(ErrorCode.LICENSE_EXPORT_FAIL, ErrorCode.fromCode(153));
		assertEquals(ErrorCode.LICENSE_INSTALL_FAIL, ErrorCode.fromCode(154));
		assertEquals(ErrorCode.LICENSE_CANCEL_FAIL, ErrorCode.fromCode(155));
		assertNull(ErrorCode.fromCode(999));
	}
}
