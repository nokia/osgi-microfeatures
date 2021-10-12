// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.LicenseException;


interface LicenseCheckValidator {

    /**
     * 
     * @param storedLicense
     * @param licenseInstall
     * @return
     * @throws LicenseException
     */
    public boolean checkIntegrity(StoredLicense storedLicense, LicenseIntegrityValidator licenseIntegrityValidator)
            throws LicenseException;

    /**
     * 
     * @param storedLicense
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws LicenseException
     * @throws ParseException
     */
    public boolean checkValidity(StoredLicense storedLicense)
            throws SQLException, ClassNotFoundException, IOException, LicenseException, ParseException;
}
