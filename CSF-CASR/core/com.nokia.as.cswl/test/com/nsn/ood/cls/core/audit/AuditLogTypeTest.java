/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.audit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class AuditLogTypeTest {

	@Test
	public void testToString() throws Exception {
		assertEquals("Change setting", AuditLogType.CHANGE_SETTING.toString());
		assertEquals("License installation", AuditLogType.LICENSE_INSTALLATION.toString());
		assertEquals("License termination", AuditLogType.LICENSE_TERMINATION.toString());
		assertEquals("", AuditLogType.UNDEFINED.toString());
	}
}
