// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.interfaces;

import java.util.prefs.Preferences;


public interface CLJLPreferences {

    // Get the systemRoot of preferences.
    public Preferences getPreferencesSystemRoot() throws LicenseException;

    // Get the userRoot of preferences.
    public Preferences getPreferencesUserRoot() throws LicenseException;
}
