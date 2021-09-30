/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.model;

import java.util.HashMap;
import java.util.Map;


/**
 * @author marynows
 * 
 */
public enum LicenseState {
	INACTIVE("inactive"), //
	ACTIVE("active"), //
	EXPIRED("expired"), //
	;

	private final String value;
	private static Map<String, LicenseState> constants = new HashMap<>();

	static {
		for (final LicenseState c : values()) {
			constants.put(c.value, c);
		}
	}

	private LicenseState(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return this.value;
	}

	public static LicenseState fromValue(final String value) {
		final LicenseState constant = constants.get(value);
		if (constant == null) {
			throw new IllegalArgumentException(value);
		} else {
			return constant;
		}
	}
}
