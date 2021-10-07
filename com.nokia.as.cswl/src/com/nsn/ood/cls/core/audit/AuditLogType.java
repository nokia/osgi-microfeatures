/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.audit;

/**
 * @author marynows
 * 
 */
public enum AuditLogType {
	UNDEFINED(""), //
	LICENSE_INSTALLATION("License installation"), //
	LICENSE_TERMINATION("License termination"), //
	CHANGE_SETTING("Change setting"), //
	;

	private final String name;

	private AuditLogType(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}
}