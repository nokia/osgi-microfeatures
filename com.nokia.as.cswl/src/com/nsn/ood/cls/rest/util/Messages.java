/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.model.internal.SettingKey;
import com.nsn.ood.cls.rest.exception.CLSCertificateException;


/**
 * @author marynows
 *
 */
public final class Messages {
	private static final Logger LOG = LoggerFactory.getLogger(Messages.class);

	private static final ResourceBundle ERRORS_BUNDLE = ResourceBundle.getBundle("strings.errors", Locale.US, CLSCertificateException.class.getClassLoader()); //$NON-NLS-1$
	private static final ResourceBundle SETTINGS_BUNDLE = ResourceBundle.getBundle("strings.settings", Locale.US, CLSCertificateException.class.getClassLoader()); //$NON-NLS-1$
	private static final ResourceBundle VALIDATIONS_BUNDLE = ResourceBundle.getBundle("strings.validations", Locale.US, CLSCertificateException.class.getClassLoader()); //$NON-NLS-1$

	private Messages() {
	}

	public static String getErrorDevMessage(final long code) {
		return getErrorString(code + ".dev"); //$NON-NLS-1$
	}

	public static String getErrorUserMessage(final long code) {
		return getErrorString(code + ".user"); //$NON-NLS-1$
	}

	private static String getErrorString(final String key) {
		try {
			return ERRORS_BUNDLE.getString(key);
		} catch (final MissingResourceException e) {
			LOG.warn("Missing key " + key, e);
			return '!' + key + '!';
		}
	}

	public static String getSetting(final SettingKey key) {
		try {
			return SETTINGS_BUNDLE.getString(key.toString());
		} catch (final MissingResourceException e) {
			LOG.warn("Missing key " + key, e);
			return '!' + key.toString() + '!';
		}
	}

	public static String getValidationError(final String key) {
		try {
			return VALIDATIONS_BUNDLE.getString(key);
		} catch (final MissingResourceException e) {
			LOG.warn("Missing key " + key, e);
			return '!' + key + '!';
		}
	}
}
