// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.apptracker;

import org.osgi.framework.Bundle;

/**
 * Service used by applications with an activator to register to the platform
 */
public interface AppService
{
    /**
     * Register an application bundle on demand
     * @param b the application bundle
     */
    public void registerApplication(Bundle b);

    /**
     * Unregister an application bundle on demand
     * @param b the application bundle
     */
    public void unregisterApplication(Bundle b);
}
