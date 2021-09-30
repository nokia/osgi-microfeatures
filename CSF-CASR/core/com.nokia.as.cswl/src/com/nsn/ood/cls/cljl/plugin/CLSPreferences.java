/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.cljl.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nokia.licensing.interfaces.CLJLPreferences;
import com.nokia.licensing.interfaces.LicenseException;
import com.nsn.ood.cls.util.DevMode;


/**
 * @author marynows
 *
 */
public class CLSPreferences implements CLJLPreferences {
	private static final Logger LOG = LoggerFactory.getLogger(CLSPreferences.class);
	private static final String ROOT_NODE = "cls";

	@Override
	public Preferences getPreferencesSystemRoot() throws LicenseException {
		return DevMode.isEnable() ? getPreferencesUserRoot() : Preferences.systemRoot().node(ROOT_NODE);
	}

	@Override
	public Preferences getPreferencesUserRoot() throws LicenseException {
		return Preferences.userRoot().node(ROOT_NODE);
	}

	public static void importPreferences() {
		final String resourceName = (DevMode.isEnable() ? "/Pref_system_LicenseInstall_user_CLS.xml"
				: "/Pref_system_LicenseInstall_CLS.xml");
		final InputStream prefStream = CLSPreferences.class.getResourceAsStream(resourceName);

		try {
			Preferences.importPreferences(prefStream);
		} catch (IOException | InvalidPreferencesFormatException e) {
			LOG.warn("Cannot import preferences.", e);
		}
	}
}
