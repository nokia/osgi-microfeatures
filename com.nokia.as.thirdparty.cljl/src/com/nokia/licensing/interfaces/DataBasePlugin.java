// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.interfaces;

import java.sql.Connection;


/**
 * Database plugin for storing the license details.
 */
public interface DataBasePlugin {

    Connection getConnection() throws LicenseException;
}
