// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.agnosticImpl.cljlPrefs;

import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;


/**
 * PreferencesFactory implementation.
 */
public class PreferencesFactoryImpl implements PreferencesFactory {
    /**
     * Synchronization lock, must be same for all PreferencesImpl instances. Visibility should be package private.
     */
    static final Object PREF_USE_LOCK = new Object();

    /** Handle to system root */
    private PreferencesImpl systemRootImpl = null;

    /** Handle to user root */
    private PreferencesImpl userRootImpl = null;

    /**
     * Returns the system root preference node. (Multiple calls on this method will return the same object reference.)
     *
     * @see java.util.prefs.PreferencesFactory#systemRoot()
     * @return systemroot
     */
    @Override
    public Preferences systemRoot() {
        if (this.systemRootImpl != null) {
            return this.systemRootImpl;
        }
        return getImplementation(true);
    }

    /**
     * Returns the system user preference node. (Multiple calls on this method will return the same object reference.)
     * client-context.
     *
     * @see java.util.prefs.PreferencesFactory#userRoot()
     * @return userroot
     */
    @Override
    public Preferences userRoot() {
        if (this.userRootImpl != null) {
            return this.userRootImpl;
        }
        return getImplementation(false);
    }

    /**
     * Gets implementation for system or user root.
     *
     * @param isSystemRoot
     *            True if system root is returned
     * @return The implementation
     */
    private Preferences getImplementation(final boolean isSystemRoot) {

        synchronized (PREF_USE_LOCK) {
            if (this.systemRootImpl == null) {
                // Should be done only once per thread
                this.systemRootImpl = new PreferencesImpl();
            }
            if (isSystemRoot) {
                return this.systemRootImpl;
            }
            if (this.userRootImpl == null) {
                this.userRootImpl = this.systemRootImpl.createUserPreferencesImpl();
            }
            return this.userRootImpl;
        }
    }
}
