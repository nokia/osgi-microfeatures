// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.utils;

import java.io.File;
import java.io.InputStream;

import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.LicenseException;


/**
 *
 * @author bogacz
 *
 */
public interface LicensesFilesRepository {
    /**
     * Gets repository dir
     * 
     * @return the location of repository
     */
    public String getRepositoryDir();

    /**
     * Delete license file from license-repository.
     * 
     * @param licenseFilePath
     */
    public void deleteLicenseFromFileRepo(String licenseFileName) throws LicenseException;

    /**
     * Creating a file from InputStream
     * 
     * @param licenseFileStream
     * @param destPath
     * @param licenseFilePath
     * @return
     * @throws LicenseException
     */
    public File copyLicenseIntoFileRepo(InputStream licenseFileStream, String licenseFileName) throws LicenseException;

    /**
     * Reads license details from file
     * 
     * @param licenseFileName
     * @return StoredLicense
     * @throws LicenseException
     */
    public StoredLicense readStoredLicense(String licenseFileName) throws LicenseException;

    /**
     * 
     * @param storedLicense
     * @param deleteInvalid
     *            try to delete license from repository if not valid
     * @param checkCertificateExpiration
     *            if true verify certificate expiration date (PKI C+/C++)
     * @return is license file valid
     * @throws LicenseException
     */
    public boolean isValid(StoredLicense storedLicense, boolean deleteInvalid, boolean checkCertificateExpiration)
            throws LicenseException;
}
