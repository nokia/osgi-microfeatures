/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util;

/**
 * @author marynows
 * 
 */
public final class Strings {

	private Strings() {
	}

	public static boolean isNullOrEmpty(final String string) {
		return (string == null) || string.isEmpty();
	}

	public static String nullToEmpty(final String string) {
		return (string == null) ? "" : string;
	}

	public static String emptyToNull(final String string) {
		return isNullOrEmpty(string) ? null : string;
	}
}
