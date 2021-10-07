package com.nokia.licensing.interfaces;

import java.util.prefs.Preferences;


public interface CLJLPreferences {

    // Get the systemRoot of preferences.
    public Preferences getPreferencesSystemRoot() throws LicenseException;

    // Get the userRoot of preferences.
    public Preferences getPreferencesUserRoot() throws LicenseException;
}
