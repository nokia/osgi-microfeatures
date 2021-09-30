/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.util;

import java.util.MissingResourceException;
import java.util.ResourceBundle;


/**
 * @author marynows
 * 
 */
public final class Messages {
	private static final ResourceBundle SUBJECTS_BUNDLE = ResourceBundle.getBundle("strings.subjects"); //$NON-NLS-1$

	private Messages() {
	}

	public static String getSubject(final String key) {
		try {
			return SUBJECTS_BUNDLE.getString(key);
		} catch (final MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
