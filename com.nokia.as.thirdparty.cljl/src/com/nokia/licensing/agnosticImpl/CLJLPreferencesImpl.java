// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.agnosticImpl;

import java.util.prefs.Preferences;

import com.nokia.licensing.interfaces.CLJLPreferences;


public class CLJLPreferencesImpl implements CLJLPreferences {

    /**
     * Get the systemRoot of preferences from default implementation.
     *
     */
    @Override
    public Preferences getPreferencesSystemRoot() {
        Preferences prefs = null;
        prefs = Preferences.systemRoot();
        return prefs;
    }

    /**
     * Get the userRoot of preferences from default implementation.
     *
     */
    @Override
    public Preferences getPreferencesUserRoot() {
        Preferences prefs = null;
        prefs = Preferences.userRoot();
        return prefs;
    }
}
